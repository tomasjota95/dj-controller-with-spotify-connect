package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

class TrackRepository(private val trackDao: TrackDao) {

    val allTracks: Flow<List<TrackEntity>> = trackDao.getAllTracks()
        .onStart {
            seedDefaultTracksIfNeeded()
        }

    suspend fun insert(track: TrackEntity) {
        trackDao.insertTrack(track)
    }

    suspend fun deleteById(id: Int) {
        trackDao.deleteTrackById(id)
    }

    private suspend fun seedDefaultTracksIfNeeded() {
        try {
            if (trackDao.getCount() == 0) {
                val defaults = listOf(
                    TrackEntity(
                        title = "Night Ride (Synthwave)",
                        artist = "Retro Beats",
                        durationMs = 302000,
                        uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                        isDefault = true
                    ),
                    TrackEntity(
                        title = "Summer Sunset (Deep House)",
                        artist = "DJ Horizon",
                        durationMs = 425000,
                        uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                        isDefault = true
                    ),
                    TrackEntity(
                        title = "Street Vibe (Hip Hop)",
                        artist = "Lofi Beats",
                        durationMs = 302000,
                        uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                        isDefault = true
                    ),
                    TrackEntity(
                        title = "Cyberpunk Loop (Industrial)",
                        artist = "Neon Grid",
                        durationMs = 310000,
                        uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                        isDefault = true
                    )
                )
                trackDao.insertAll(defaults)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
