package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val uriString: String,
    val isSpotify: Boolean = false,
    val albumArtUrl: String? = null,
    val isDefault: Boolean = false
)
