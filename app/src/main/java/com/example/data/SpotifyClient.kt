package com.example.data

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

data class SpotifyTrack(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val previewUrl: String?,
    val albumArtUrl: String?,
    val spotifyUri: String
)

class SpotifyClient {
    private val client = OkHttpClient()
    private var cachedToken: String? = null
    private var tokenExpiryTimeMs: Long = 0

    suspend fun getAccessToken(clientId: String, clientSecret: String): String? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedToken != null && now < tokenExpiryTimeMs) {
            return@withContext cachedToken
        }

        try {
            val authString = "$clientId:$clientSecret"
            val authBase64 = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)

            val requestBody = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .build()

            val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .post(requestBody)
                .addHeader("Authorization", "Basic $authBase64")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("SpotifyClient", "Token request failed: ${response.code} - ${response.message}")
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val token = json.getString("access_token")
                val expiresInSeconds = json.optLong("expires_in", 3600)
                
                cachedToken = token
                tokenExpiryTimeMs = now + (expiresInSeconds * 1000) - 60000 // Buffer of 1 minute
                return@withContext token
            }
        } catch (e: Exception) {
            Log.e("SpotifyClient", "Error fetching access token: ${e.message}", e)
            return@withContext null
        }
    }

    suspend fun searchTracks(
        query: String,
        clientId: String,
        clientSecret: String
    ): List<SpotifyTrack> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        
        val token = getAccessToken(clientId, clientSecret) ?: return@withContext emptyList()
        
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.spotify.com/v1/search?q=$encodedQuery&type=track&limit=20"
            
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer $token")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("SpotifyClient", "Search request failed: ${response.code} - ${response.message}")
                    return@withContext emptyList()
                }
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                val tracksJson = json.getJSONObject("tracks")
                val itemsJson = tracksJson.getJSONArray("items")
                
                val tracksList = mutableListOf<SpotifyTrack>()
                for (i in 0 until itemsJson.length()) {
                    val item = itemsJson.getJSONObject(i)
                    val title = item.getString("name")
                    
                    // Artists
                    val artistsArray = item.getJSONArray("artists")
                    val artistsList = mutableListOf<String>()
                    for (j in 0 until artistsArray.length()) {
                        artistsList.add(artistsArray.getJSONObject(j).getString("name"))
                    }
                    val artist = artistsList.joinToString(", ")
                    
                    val durationMs = item.getLong("duration_ms")
                    val previewUrl = if (item.isNull("preview_url")) null else item.optString("preview_url")
                    
                    // Album Art
                    val album = item.getJSONObject("album")
                    val images = album.getJSONArray("images")
                    val albumArtUrl = if (images.length() > 0) {
                        images.getJSONObject(0).getString("url")
                    } else null
                    
                    val spotifyUri = item.getString("uri")
                    
                    tracksList.add(SpotifyTrack(
                        title = title,
                        artist = artist,
                        durationMs = durationMs,
                        previewUrl = previewUrl,
                        albumArtUrl = albumArtUrl,
                        spotifyUri = spotifyUri
                    ))
                }
                return@withContext tracksList
            }
        } catch (e: Exception) {
            Log.e("SpotifyClient", "Error searching tracks: ${e.message}", e)
            return@withContext emptyList()
        }
    }
}
