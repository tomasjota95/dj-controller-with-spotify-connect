package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.net.Uri
import android.util.Log

class DjChannel(private val context: Context, private val channelName: String) {
    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    
    var currentUri: String? = null
    var isPlaying = false
    var volume = 1.0f // Individual vertical fader volume
    var crossfadeFactor = 1.0f // Derived from crossfader position
    
    // EQ Gains in dB (customizable from -12dB to +12dB)
    var bassLevel = 0f // in dB
    var midLevel = 0f // in dB
    var trebleLevel = 0f // in dB
    
    // Cached Equalizer ranges to avoid expensive native Binder query overhead during continuous dragging
    private var numBands: Short = 0
    private var minRange: Int = -1500
    private var maxRange: Int = 1500
    
    // Pitch/playback speed (0.5x to 2.0x, normal is 1.0f)
    var pitch = 1.0f
    
    // Loop enable
    var isLooping = false
    
    // Cue point in milliseconds
    var cuePosition: Int? = null

    // Track state callbacks
    var onTrackCompleted: (() -> Unit)? = null
    var onTrackDurationReady: ((Int) -> Unit)? = null

    fun loadTrack(uriString: String) {
        synchronized(this) {
            try {
                stop()
                mediaPlayer?.release()
                equalizer?.release()
                mediaPlayer = null
                equalizer = null
                
                val mp = MediaPlayer().apply {
                    if (uriString.startsWith("http") || uriString.startsWith("https")) {
                        setDataSource(uriString)
                    } else {
                        setDataSource(context, Uri.parse(uriString))
                    }
                    prepareAsync()
                }
                mediaPlayer = mp
                currentUri = uriString
                isPlaying = false
                
                mp.setOnPreparedListener { preparedMp ->
                    synchronized(this@DjChannel) {
                        if (mediaPlayer == preparedMp) {
                            onTrackDurationReady?.invoke(preparedMp.duration)
                            updateAudioParams()
                            initEqualizer(preparedMp.audioSessionId)
                        }
                    }
                }
                
                mp.setOnCompletionListener { preparedMp ->
                    synchronized(this@DjChannel) {
                        if (mediaPlayer == preparedMp) {
                            isPlaying = false
                            onTrackCompleted?.invoke()
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e("DjChannel", "Error loading track for $channelName: ${e.message}", e)
            }
        }
    }
    
    private fun initEqualizer(audioSessionId: Int) {
        synchronized(this) {
            try {
                val eq = Equalizer(0, audioSessionId)
                eq.enabled = true
                
                // Query and cache these fixed properties once to avoid native Binder overhead later
                numBands = eq.numberOfBands
                if (numBands > 0) {
                    val range = eq.bandLevelRange
                    minRange = range[0].toInt()
                    maxRange = range[1].toInt()
                }
                
                equalizer = eq
                applyEqLevels()
            } catch (e: Exception) {
                Log.e("DjChannel", "Failed to init hardware Equalizer: ${e.message}")
            }
        }
    }
    
    fun play() {
        synchronized(this) {
            mediaPlayer?.let {
                try {
                    it.start()
                    isPlaying = true
                    // Re-apply pitch on play because sometimes playbackParams gets reset on pause/play transitions
                    setPitchSpeed(pitch)
                } catch (e: Exception) {
                    Log.e("DjChannel", "Error starting playback: ${e.message}")
                }
            }
        }
    }
    
    fun pause() {
        synchronized(this) {
            mediaPlayer?.let {
                try {
                    if (it.isPlaying) {
                        it.pause()
                    }
                    isPlaying = false
                } catch (e: Exception) {
                    Log.e("DjChannel", "Error pausing playback: ${e.message}")
                }
            }
        }
    }
    
    fun stop() {
        synchronized(this) {
            mediaPlayer?.let {
                try {
                    it.stop()
                    isPlaying = false
                } catch (e: Exception) {
                    Log.e("DjChannel", "Error stopping playback: ${e.message}")
                }
            }
        }
    }
    
    fun seekTo(positionMs: Int) {
        synchronized(this) {
            try {
                mediaPlayer?.seekTo(positionMs)
            } catch (e: Exception) {
                Log.e("DjChannel", "Error seekTo: ${e.message}")
            }
        }
    }
    
    val currentPosition: Int
        get() = synchronized(this) {
            try { mediaPlayer?.currentPosition ?: 0 } catch(e: Exception) { 0 }
        }
        
    val duration: Int
        get() = synchronized(this) {
            try { mediaPlayer?.duration ?: 0 } catch(e: Exception) { 0 }
        }
 
    fun release() {
        synchronized(this) {
            try {
                mediaPlayer?.release()
                equalizer?.release()
            } catch (e: Exception) {
                Log.e("DjChannel", "Error releasing players: ${e.message}")
            } finally {
                mediaPlayer = null
                equalizer = null
            }
        }
    }
    
    private fun updateAudioParams() {
        synchronized(this) {
            setVolumeAndFade(volume, crossfadeFactor)
            setPitchSpeed(pitch)
            setLoop(isLooping)
        }
    }
 
    fun setVolumeAndFade(individualVol: Float, fadeVol: Float) {
        synchronized(this) {
            volume = individualVol
            crossfadeFactor = fadeVol
            val effectiveVol = volume * crossfadeFactor
            try {
                mediaPlayer?.setVolume(effectiveVol, effectiveVol)
            } catch (e: Exception) {
                Log.e("DjChannel", "Error setVolume: ${e.message}")
            }
        }
    }
    
    fun setPitchSpeed(speed: Float) {
        synchronized(this) {
            pitch = speed
            val mp = mediaPlayer ?: return
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                try {
                    val params = mp.playbackParams
                    params.speed = pitch
                    mp.playbackParams = params
                    
                    // If it wasn't playing, playbackParams speed can auto-start it, so force pause if isPlaying state is false
                    if (!isPlaying && mp.isPlaying) {
                        mp.pause()
                    }
                } catch (e: Exception) {
                    Log.e("DjChannel", "Error setting pitch/playback parameters: ${e.message}")
                }
            }
        }
    }
    
    fun setLoop(loop: Boolean) {
        synchronized(this) {
            isLooping = loop
            try {
                mediaPlayer?.isLooping = loop
            } catch (e: Exception) {
                Log.e("DjChannel", "Error setting loop: ${e.message}")
            }
        }
    }
    
    fun setCue() {
        synchronized(this) {
            try {
                mediaPlayer?.let {
                    cuePosition = it.currentPosition
                }
            } catch (e: Exception) {
                Log.e("DjChannel", "Error setting cue: ${e.message}")
            }
        }
    }
    
    fun jumpToCue() {
        synchronized(this) {
            try {
                cuePosition?.let { pos ->
                    mediaPlayer?.seekTo(pos)
                } ?: run {
                    mediaPlayer?.seekTo(0)
                }
            } catch (e: Exception) {
                Log.e("DjChannel", "Error jumping to cue: ${e.message}")
            }
        }
    }
 
    fun setEqGains(bassdB: Float, middB: Float, trebledB: Float) {
        synchronized(this) {
            bassLevel = bassdB
            midLevel = middB
            trebleLevel = trebledB
            applyEqLevels()
        }
    }
    
    private fun applyEqLevels() {
        synchronized(this) {
            val eq = equalizer ?: return
            try {
                if (numBands == 0.toShort()) return
                
                fun dBToBandLevel(dB: Float): Short {
                    val millibels = (dB * 100).toInt()
                    return millibels.coerceIn(minRange, maxRange).toShort()
                }
                
                val bassVal = dBToBandLevel(bassLevel)
                val midVal = dBToBandLevel(midLevel)
                val trebleVal = dBToBandLevel(trebleLevel)
                
                if (numBands >= 5) {
                    eq.setBandLevel(0, bassVal)
                    eq.setBandLevel(1, bassVal)
                    eq.setBandLevel(2, midVal)
                    eq.setBandLevel(3, trebleVal)
                    eq.setBandLevel(4, trebleVal)
                } else if (numBands > 0) {
                    val avgVal = ((bassVal + midVal + trebleVal) / 3).toShort()
                    for (i in 0 until numBands) {
                        eq.setBandLevel(i.toShort(), avgVal)
                    }
                }
            } catch (e: Exception) {
                Log.e("DjChannel", "Error applying EQ band levels: ${e.message}")
            }
        }
    }
}
