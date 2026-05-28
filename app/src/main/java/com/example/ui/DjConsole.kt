package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.SpotifyTrack
import com.example.data.TrackEntity
import kotlinx.coroutines.delay

val CyberCyan = Color(0xFF00E5FF)
val TechnoOrange = Color(0xFFFF5722)
val DJCabinBg = Color(0xFF09090B)           // Luxury pure dark depth
val SurfaceDark = Color(0xFF18181B)         // Elegant Dark panel backdrop
val ControlGray = Color(0xFF27272A)         // Elegant Dark control surfaces
val MixerBg = Color(0xFF121214)             // Elegant Dark mixer backdrop
val SpotifyGreen = Color(0xFF1DB954)        // Spotify Brand Accent
val BorderWhiteTranslucent = Color(0x0DFFFFFF) // border-white/5 translucent bevel outline
val TextSlate200 = Color(0xFFE2E8F0)         // Slate 200 text color
val TextSlate400 = Color(0xFF94A3B8)         // Slate 400 info color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DjConsole(
    viewModel: DjViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val deckA by viewModel.deckAState.collectAsStateWithLifecycle()
    val deckB by viewModel.deckBState.collectAsStateWithLifecycle()
    val xFadeVal by viewModel.xFade.collectAsStateWithLifecycle()
    
    val localSongs by viewModel.localLibrary.collectAsStateWithLifecycle()
    val spotifyResults by viewModel.spotifySearchResults.collectAsStateWithLifecycle()
    val isSearchLoading by viewModel.isSearchLoading.collectAsStateWithLifecycle()
    
    val spotifyId by viewModel.spotifyClientId.collectAsStateWithLifecycle()
    val spotifySec by viewModel.spotifyClientSecret.collectAsStateWithLifecycle()
    val searchQuery by viewModel.spotifySearchQuery.collectAsStateWithLifecycle()
    
    val showSettings by viewModel.showSettings.collectAsStateWithLifecycle()
    val showAdvanced by viewModel.showAdvancedControls.collectAsStateWithLifecycle()

    // Sound FX state collections
    val fx1ActiveA by viewModel.fx1ActiveA.collectAsStateWithLifecycle()
    val fx1AmountA by viewModel.fx1AmountA.collectAsStateWithLifecycle()
    val fx2ActiveA by viewModel.fx2ActiveA.collectAsStateWithLifecycle()
    val fx2AmountA by viewModel.fx2AmountA.collectAsStateWithLifecycle()
    val fx3ActiveA by viewModel.fx3ActiveA.collectAsStateWithLifecycle()
    val fx3AmountA by viewModel.fx3AmountA.collectAsStateWithLifecycle()

    val fx1ActiveB by viewModel.fx1ActiveB.collectAsStateWithLifecycle()
    val fx1AmountB by viewModel.fx1AmountB.collectAsStateWithLifecycle()
    val fx2ActiveB by viewModel.fx2ActiveB.collectAsStateWithLifecycle()
    val fx2AmountB by viewModel.fx2AmountB.collectAsStateWithLifecycle()
    val fx3ActiveB by viewModel.fx3ActiveB.collectAsStateWithLifecycle()
    val fx3AmountB by viewModel.fx3AmountB.collectAsStateWithLifecycle()
    
    // Song load dialog/sheet
    var selectedTrackToLoad by remember { mutableStateOf<TrackEntity?>(null) }
    var currentLibTab by remember { mutableStateOf(0) } // 0 = Biblioteca Local, 1 = Spotify Web

    // File picker launcher for local MP3 importing
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Read display name
            var displayName = "imported_track.mp3"
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIdx != -1 && cursor.moveToFirst()) {
                    displayName = cursor.getString(nameIdx)
                }
            }
            viewModel.importLocalFile(it, displayName, context)
            Toast.makeText(context, "Importando $displayName...", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DJCabinBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                ConsoleHeader(
                    onSettingsClick = { viewModel.toggleSettings(true) },
                    onAdvancedToggle = { viewModel.toggleAdvancedControls(!showAdvanced) },
                    advancedActive = showAdvanced
                )

                Spacer(modifier = Modifier.height(16.dp))

                // DJ DECKS AND MAIN MIXER ROW
                // On narrow portrait screens, we wrap or stack them elegantly. But to look like a console,
                // we stack Deck A, Central Mixer, and Deck B sequentially. That mirrors professional hardware!
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, BorderWhiteTranslucent),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Decks Grid (Symmetric layout)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Deck A (Cyan)
                            Box(modifier = Modifier.weight(1.3f)) {
                                DjDeck(
                                    deckState = deckA,
                                    deckColor = CyberCyan,
                                    deckLabel = "DECK A",
                                    onPlayPause = { viewModel.togglePlay(deckA = true) },
                                    onSeek = { pos -> viewModel.seekTrack(pos, deckA = true) },
                                    onSync = {
                                        val targetBpm = deckB.bpm * deckB.pitch
                                        val rawBpmA = deckA.bpm
                                        if (rawBpmA > 0) {
                                            val matchFactor = targetBpm / rawBpmA
                                            viewModel.setPitch(matchFactor.coerceIn(0.5f, 2.0f), deckA = true)
                                            Toast.makeText(context, "Sincronizado: ${String.format("%.1f", targetBpm)} BPM", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "No hay canción en Deck A", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    isLandscape = isLandscape
                                )
                            }

                            // DJ MIXER STRIP (Central Channel Vol Faders + EQ knobs)
                            Box(
                                modifier = Modifier
                                    .weight(1.5f) // slightly wider on dynamic grid for readability
                                    .fillMaxHeight()
                                    .background(MixerBg, shape = RoundedCornerShape(16.dp))
                                    .border(1.dp, BorderWhiteTranslucent, shape = RoundedCornerShape(16.dp))
                                    .padding(8.dp)
                            ) {
                                CentralMixer(
                                    deckA = deckA,
                                    deckB = deckB,
                                    onEqChangeA = { b, m, t -> viewModel.setEq(b, m, t, deckA = true) },
                                    onEqChangeB = { b, m, t -> viewModel.setEq(b, m, t, deckA = false) },
                                    onVolChangeA = { vol -> viewModel.setVolume(vol, deckA = true) },
                                    onVolChangeB = { vol -> viewModel.setVolume(vol, deckA = false) }
                                )
                            }

                            // Deck B (Orange)
                            Box(modifier = Modifier.weight(1.3f)) {
                                DjDeck(
                                    deckState = deckB,
                                    deckColor = TechnoOrange,
                                    deckLabel = "DECK B",
                                    onPlayPause = { viewModel.togglePlay(deckA = false) },
                                    onSeek = { pos -> viewModel.seekTrack(pos, deckA = false) },
                                    onSync = {
                                        val targetBpm = deckA.bpm * deckA.pitch
                                        val rawBpmB = deckB.bpm
                                        if (rawBpmB > 0) {
                                            val matchFactor = targetBpm / rawBpmB
                                            viewModel.setPitch(matchFactor.coerceIn(0.5f, 2.0f), deckA = false)
                                            Toast.makeText(context, "Sincronizado: ${String.format("%.1f", targetBpm)} BPM", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "No hay canción en Deck B", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    isLandscape = isLandscape
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Crossfader Strip (Full width blending)
                        CrossfaderSection(
                            value = xFadeVal,
                            onValueChange = { viewModel.setCrossfader(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CONDITIONAL POPUP: ADVANCED CONTROLS (Pitch, BPM matches, Hot cues, Loops)
                AnimatedVisibility(
                    visible = showAdvanced,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    AdvancedControlsModule(
                        deckA = deckA,
                        deckB = deckB,
                        onPitchChangeA = { target -> viewModel.setPitch(target, deckA = true) },
                        onPitchChangeB = { target -> viewModel.setPitch(target, deckA = false) },
                        onPitchDeltaA = { delta -> viewModel.modifyPitch(delta, deckA = true) },
                        onPitchDeltaB = { delta -> viewModel.modifyPitch(delta, deckA = false) },
                        onCueSetA = { viewModel.setCue(deckA = true) },
                        onCueSetB = { viewModel.setCue(deckA = false) },
                        onCueJumpA = { viewModel.jumpToCue(deckA = true) },
                        onCueJumpB = { viewModel.jumpToCue(deckA = false) },
                        onLoopToggleA = { viewModel.toggleLoop(deckA = true) },
                        onLoopToggleB = { viewModel.toggleLoop(deckA = false) },
                        isLandscape = isLandscape,
                        
                        fx1ActiveA = fx1ActiveA,
                        fx1AmountA = fx1AmountA,
                        onFx1ChangeA = { active, amt ->
                            viewModel.setFx1ActiveA(active)
                            viewModel.setFx1AmountA(amt)
                        },
                        fx2ActiveA = fx2ActiveA,
                        fx2AmountA = fx2AmountA,
                        onFx2ChangeA = { active, amt ->
                            viewModel.setFx2ActiveA(active)
                            viewModel.setFx2AmountA(amt)
                        },
                        fx3ActiveA = fx3ActiveA,
                        fx3AmountA = fx3AmountA,
                        onFx3ChangeA = { active, amt ->
                            viewModel.setFx3ActiveA(active)
                            viewModel.setFx3AmountA(amt)
                        },
                        
                        fx1ActiveB = fx1ActiveB,
                        fx1AmountB = fx1AmountB,
                        onFx1ChangeB = { active, amt ->
                            viewModel.setFx1ActiveB(active)
                            viewModel.setFx1AmountB(amt)
                        },
                        fx2ActiveB = fx2ActiveB,
                        fx2AmountB = fx2AmountB,
                        onFx2ChangeB = { active, amt ->
                            viewModel.setFx2ActiveB(active)
                            viewModel.setFx2AmountB(amt)
                        },
                        fx3ActiveB = fx3ActiveB,
                        fx3AmountB = fx3AmountB,
                        onFx3ChangeB = { active, amt ->
                            viewModel.setFx3ActiveB(active)
                            viewModel.setFx3AmountB(amt)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // MUSIC LIBRARY AND SPOTIFY ENGINE CONTROL
                LibraryPanel(
                    currentTab = currentLibTab,
                    onTabToggle = { currentLibTab = it },
                    localTracks = localSongs,
                    spotifyTracks = spotifyResults,
                    searchQuery = searchQuery,
                    isSearchLoading = isSearchLoading,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onTrackSelected = { selectedTrackToLoad = it },
                    onImportClick = { filePickerLauncher.launch("audio/mpeg") },
                    onDeleteClick = { id -> viewModel.deleteTrack(id) },
                    hasSpotifyCredentials = spotifyId.isNotBlank() && spotifySec.isNotBlank(),
                    onPromptSetup = { viewModel.toggleSettings(true) }
                )
            }

            // POPUP MODAL: Load Track options
            selectedTrackToLoad?.let { track ->
                AlertDialog(
                    onDismissRequest = { selectedTrackToLoad = null },
                    containerColor = SurfaceDark,
                    titleContentColor = Color.White,
                    textContentColor = Color.LightGray,
                    title = { Text("Cargar Canción", fontWeight = FontWeight.SemiBold) },
                    text = {
                        Column {
                            Text(text = "Título: ${track.title}", fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "Artista: ${track.artist}", fontSize = 13.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Selecciona en qué Deck quieres reproducir el tema:")
                        }
                    },
                    confirmButton = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    viewModel.loadTrack(track, toDeckA = true)
                                    selectedTrackToLoad = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.2f), contentColor = CyberCyan),
                                border = BorderStroke(1.dp, CyberCyan)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyberCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DECK A")
                            }

                            Button(
                                onClick = {
                                    viewModel.loadTrack(track, toDeckA = false)
                                    selectedTrackToLoad = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TechnoOrange.copy(alpha = 0.2f), contentColor = TechnoOrange),
                                border = BorderStroke(1.dp, TechnoOrange)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TechnoOrange)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DECK B")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedTrackToLoad = null }) {
                            Text("Cancelar", color = Color.Gray)
                        }
                    }
                )
            }

            // POPUP MODAL: Settings overlay for Spotify Credentials
            if (showSettings) {
                SpotifySettingsDialog(
                    initialClientId = spotifyId,
                    initialClientSecret = spotifySec,
                    onDismiss = { viewModel.toggleSettings(false) },
                    onSave = { cid, sec ->
                        viewModel.updateSpotifyCredentials(cid, sec)
                        viewModel.toggleSettings(false)
                        Toast.makeText(context, "Credenciales de Spotify Actualizadas", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// ------------------- UI SUBCOMPONENTS -------------------

@Composable
fun ConsoleHeader(
    onSettingsClick: () -> Unit,
    onAdvancedToggle: () -> Unit,
    advancedActive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MixerBg, shape = RoundedCornerShape(12.dp))
            .border(1.dp, BorderWhiteTranslucent, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Brand turntable icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(SpotifyGreen, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .border(2.dp, Color.Black, shape = CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "PRO-LINK SYNC",
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 12.sp
                )
                Text(
                    text = "ESTADO: EN LÍNEA",
                    color = SpotifyGreen,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Configuración",
                    tint = TextSlate400,
                    modifier = Modifier.size(20.dp)
                )
            }

            Button(
                onClick = onAdvancedToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (advancedActive) TechnoOrange else Color(0xFF1F1F24),
                    contentColor = if (advancedActive) Color.Black else TextSlate200
                ),
                shape = RoundedCornerShape(20.dp), // Fully rounded pill shape
                border = BorderStroke(1.dp, if (advancedActive) TechnoOrange else BorderWhiteTranslucent),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("cue_pitch_loop_toggle")
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("FX / TOOLS", fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun DjDeck(
    deckState: DeckState,
    deckColor: Color,
    deckLabel: String,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSync: () -> Unit = {},
    isLandscape: Boolean = false
) {
    val vinylSize = if (isLandscape) 75.dp else 100.dp
    val spacing = if (isLandscape) 4.dp else 8.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, shape = RoundedCornerShape(16.dp))
            .border(1.dp, BorderWhiteTranslucent, shape = RoundedCornerShape(16.dp))
            .border(1.dp, deckColor.copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp))
            .padding(if (isLandscape) 6.dp else 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Deck Indicator Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = deckLabel,
                color = deckColor,
                fontWeight = FontWeight.Black,
                fontSize = if (isLandscape) 10.sp else 12.sp,
                fontFamily = FontFamily.Monospace
            )
            
            // Spinning Disk indicator lights or Spotify Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (deckState.isSpotify) {
                    Icon(
                        imageVector = Icons.Default.Stream,
                        contentDescription = "Spotify Streaming",
                        tint = Color(0xFF1DB954),
                        modifier = Modifier
                            .size(12.dp)
                            .padding(end = 2.dp)
                    )
                    Text("SPOTIFY", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1DB954))
                } else {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (deckState.isPlaying) Color.Green else Color.Red, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (deckState.isPlaying) "OK" else "READY",
                        fontSize = 7.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.LightGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing))

        // Vinyl Platter
        VinylPlatter(
            isPlaying = deckState.isPlaying,
            progress = if (deckState.durationMs > 0) deckState.currentPositionMs.toFloat() / deckState.durationMs else 0f,
            albumArtUrl = deckState.albumArtUrl,
            platterColor = deckColor,
            modifier = Modifier
                .size(vinylSize)
                .testTag("${deckLabel.lowercase().replace(" ", "_")}_platter")
        )

        Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 10.dp))

        // Track Text Metadata Scrollable
        Text(
            text = deckState.title,
            style = if (isLandscape) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = deckState.artist.uppercase(),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp,
            fontSize = if (isLandscape) 8.sp else 9.sp
        )

        Spacer(modifier = Modifier.height(spacing))

        // Technical Specs overlay: BPM, Pitch and Progress time
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0C0C10), shape = RoundedCornerShape(4.dp))
                .padding(if (isLandscape) 4.dp else 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text("BPM", fontSize = 6.sp, color = Color.Gray)
                Text(
                    text = String.format("%.1f", deckState.bpm * deckState.pitch),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isLandscape) 9.sp else 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SPEED", fontSize = 6.sp, color = Color.Gray)
                Text(
                    text = "${if (deckState.pitch >= 1f) "+" else ""}${String.format("%.1f", (deckState.pitch - 1f) * 100)}%",
                    color = if (deckState.pitch == 1.0f) Color.LightGray else deckColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isLandscape) 9.sp else 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("TIME", fontSize = 6.sp, color = Color.Gray)
                Text(
                    text = formatProgress(deckState.currentPositionMs, deckState.durationMs),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isLandscape) 9.sp else 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 10.dp))

        // Waveform Progress Scrub
        AudioProgressWaveform(
            currentPositionMs = deckState.currentPositionMs,
            durationMs = deckState.durationMs,
            isPlaying = deckState.isPlaying,
            onSeek = onSeek,
            activeColor = deckColor
        )

        Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 10.dp))

        // Direct Transport Controls: CUE and PLAY/PAUSE
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = if (isLandscape) 2.dp else 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SYNC outline button
            OutlinedButton(
                onClick = onSync,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, deckColor.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = deckColor
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier
                    .height(if (isLandscape) 28.dp else 34.dp)
                    .testTag("${deckLabel.lowercase().replace(" ", "_")}_sync_btn")
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("SYNC", fontWeight = FontWeight.Black, fontSize = 9.sp)
            }

            Spacer(modifier = Modifier.width(if (isLandscape) 8.dp else 16.dp))

            // Main play button
            Button(
                onClick = onPlayPause,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (deckState.isPlaying) Color(0xFFEF5350) else deckColor,
                    contentColor = if (deckState.isPlaying) Color.White else Color.Black
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(if (isLandscape) 36.dp else 46.dp)
                    .testTag("${deckLabel.lowercase().replace(" ", "_")}_play_btn")
            ) {
                Icon(
                    imageVector = if (deckState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Reproducir / Pausar",
                    modifier = Modifier.size(if (isLandscape) 18.dp else 26.dp)
                )
            }
        }
    }
}

@Composable
fun CentralMixer(
    deckA: DeckState,
    deckB: DeckState,
    onEqChangeA: (Float, Float, Float) -> Unit,
    onEqChangeB: (Float, Float, Float) -> Unit,
    onVolChangeA: (Float) -> Unit,
    onVolChangeB: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "MIX CENTRAL",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        // EQ Rotaries Dual Grid (Deck A on Left, Deck B on Right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Channel A EQ (Cyan indicators)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CH A", fontSize = 8.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                EqKnob(
                    label = "BASS",
                    value = deckA.bass,
                    onValueChange = { onEqChangeA(it, deckA.mid, deckA.treble) },
                    color = CyberCyan
                )
                EqKnob(
                    label = "MID",
                    value = deckA.mid,
                    onValueChange = { onEqChangeA(deckA.bass, it, deckA.treble) },
                    color = CyberCyan
                )
                EqKnob(
                    label = "HI",
                    value = deckA.treble,
                    onValueChange = { onEqChangeA(deckA.bass, deckA.mid, it) },
                    color = CyberCyan
                )
            }

            // Channel B EQ (Orange indicators)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CH B", fontSize = 8.sp, color = TechnoOrange, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                EqKnob(
                    label = "BASS",
                    value = deckB.bass,
                    onValueChange = { onEqChangeB(it, deckB.mid, deckB.treble) },
                    color = TechnoOrange
                )
                EqKnob(
                    label = "MID",
                    value = deckB.mid,
                    onValueChange = { onEqChangeB(deckB.bass, it, deckB.treble) },
                    color = TechnoOrange
                )
                EqKnob(
                    label = "HI",
                    value = deckB.treble,
                    onValueChange = { onEqChangeB(deckB.bass, deckB.mid, it) },
                    color = TechnoOrange
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Linear Master Volume Faders with vertical track and glowing dB LEDs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color(0xFF0B0B0E), shape = RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF1E1E26), shape = RoundedCornerShape(6.dp))
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Fader A
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxHeight()
            ) {
                Text("A VOL", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small virtual dB level meter
                    VerticalLedMeter(progress = deckA.volume, isPlaying = deckA.isPlaying, activeColor = CyberCyan)
                    
                    Slider(
                        value = deckA.volume,
                        onValueChange = onVolChangeA,
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .width(100.dp)
                            .rotate(-90f)
                            .testTag("vol_fader_a")
                    )
                }
            }

            // Fader B
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxHeight()
            ) {
                Text("B VOL", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VerticalLedMeter(progress = deckB.volume, isPlaying = deckB.isPlaying, activeColor = TechnoOrange)

                    Slider(
                        value = deckB.volume,
                        onValueChange = onVolChangeB,
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .width(100.dp)
                            .rotate(-90f)
                            .testTag("vol_fader_b")
                    )
                }
            }
        }
    }
}

@Composable
fun VerticalLedMeter(
    progress: Float,
    isPlaying: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val totalLeds = 8
    
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(6.dp)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom)
    ) {
        repeat(totalLeds) { index ->
            // Led ranking from bottom-to-top (0 = bottom, 7 = top)
            val ledIdx = totalLeds - 1 - index
            val relativeLevel = (ledIdx.toFloat() / totalLeds)
            
            val isGlowing = isPlaying && (progress >= relativeLevel)
            val ledColor = when {
                !isGlowing -> Color(0xFF1E1E24)
                ledIdx > 6 -> Color(0xFFFF1744) // Overload Clip (Red)
                ledIdx > 4 -> Color(0xFFFFEA00) // Warning yellow
                else -> activeColor // Warm color
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(ledColor, shape = RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
fun CrossfaderSection(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark, shape = RoundedCornerShape(16.dp))
            .border(1.dp, BorderWhiteTranslucent, shape = RoundedCornerShape(16.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DECK A (CYAN)", fontSize = 9.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
            Text("CROSSFADER CENTRAL", fontSize = 9.sp, color = Color.LightGray, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("DECK B (ORANGE)", fontSize = 9.sp, color = TechnoOrange, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Dual Track Metallic Slider
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = CyberCyan,
                inactiveTrackColor = TechnoOrange
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("crossfader")
        )
    }
}

@Composable
fun LibraryPanel(
    currentTab: Int,
    onTabToggle: (Int) -> Unit,
    localTracks: List<TrackEntity>,
    spotifyTracks: List<SpotifyTrack>,
    searchQuery: String,
    isSearchLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onTrackSelected: (TrackEntity) -> Unit,
    onImportClick: () -> Unit,
    onDeleteClick: (Int) -> Unit,
    hasSpotifyCredentials: Boolean,
    onPromptSetup: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderWhiteTranslucent),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tab Selector Switches
            TabRow(
                selectedTabIndex = currentTab,
                containerColor = MixerBg,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                        color = if (currentTab == 0) CyberCyan else Color(0xFF1DB954)
                    )
                }
            ) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { onTabToggle(0) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("BIBLIOTECA LOCAL", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { onTabToggle(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Podcasts, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("STREAM SPOTIFY", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                )
            }

            // Tab Content Window
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(12.dp)
            ) {
                if (currentTab == 0) {
                    // LOCAL LIBRARY AND IMPORTS
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Canciones Disponibles (${localTracks.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )

                            Button(
                                onClick = onImportClick,
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("IMPORTAR MP3", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (localTracks.isEmpty()) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No hay canciones disponibles. ¡Oprime importar!", color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(localTracks) { item ->
                                    LocalTrackRow(
                                        track = item,
                                        onSelect = { onTrackSelected(item) },
                                        onDelete = { onDeleteClick(item.id) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // SPOTIFY STREAMING SECTION
                    if (!hasSpotifyCredentials) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Spotify Web API se encuentra cerrado",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Necesitas configurar tu Client ID & Client Secret.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onPromptSetup,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954), contentColor = Color.White)
                            ) {
                                Text("Ingresar credenciales", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    } else {
                        // CLIENT CONNECTED: Standard Search View
                        Column(modifier = Modifier.fillMaxSize()) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onQueryChange,
                                placeholder = { Text("Buscar tracks en Spotify...", color = Color.Gray) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("spotify_search_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1DB954),
                                    unfocusedBorderColor = Color.Gray,
                                    focusedContainerColor = Color(0xFF0F0F14),
                                    unfocusedContainerColor = Color(0xFF0F0F14),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isSearchLoading) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF1DB954))
                                }
                            } else if (spotifyTracks.isEmpty()) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (searchQuery.isBlank()) "Digita el nombre de una canción o artista..." else "No se encontraron resultados en Spotify Web.",
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(spotifyTracks) { sTrack ->
                                        // Map SpotifyTrack to TrackEntity on demand
                                        val mapped = TrackEntity(
                                            title = sTrack.title,
                                            artist = sTrack.artist,
                                            durationMs = sTrack.durationMs,
                                            uriString = sTrack.previewUrl ?: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", // Use robust fallback if Spotify preview url is null
                                            isSpotify = true,
                                            albumArtUrl = sTrack.albumArtUrl
                                        )
                                        SpotifyTrackRow(
                                            spotifyTrack = sTrack,
                                            onSelect = { onTrackSelected(mapped) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocalTrackRow(
    track: TrackEntity,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF121214), shape = RoundedCornerShape(8.dp))
            .border(1.dp, BorderWhiteTranslucent, shape = RoundedCornerShape(8.dp))
            .clickable { onSelect() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = if (track.isDefault) Color.Gray else CyberCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist.uppercase(),
                    color = Color.Gray,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${track.durationMs / 60000}:${String.format("%02d", (track.durationMs % 60000) / 1000)}",
                color = Color.DarkGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 8.dp)
            )

            if (!track.isDefault) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Borrar",
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Text(
                    text = "DEMO",
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .background(Color(0xFF1E1E24), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SpotifyTrackRow(
    spotifyTrack: SpotifyTrack,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF121214), shape = RoundedCornerShape(8.dp))
            .border(1.dp, BorderWhiteTranslucent, shape = RoundedCornerShape(8.dp))
            .clickable { onSelect() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art loading
            if (spotifyTrack.albumArtUrl != null) {
                AsyncImage(
                    model = spotifyTrack.albumArtUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFF22222E), shape = RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Podcasts, contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = spotifyTrack.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = spotifyTrack.artist.uppercase(),
                    color = Color(0xFF1DB954),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Stream availability badge
            if (spotifyTrack.previewUrl != null) {
                Text(
                    text = "PLAYABLE",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .background(Color(0xFF1DB954), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            } else {
                Text(
                    text = "W/O PREV",
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .background(Color(0xFF1F1F2A), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${spotifyTrack.durationMs / 60000}:${String.format("%02d", (spotifyTrack.durationMs % 60000) / 1000)}",
                color = Color.DarkGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}

// Custom compact design potentiometer dial knob for sound effects
@Composable
fun FxKnob(
    label: String,
    value: Float, // 0f to 1f
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = CyberCyan
) {
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    var dragAccumulator by remember { mutableStateOf(0f) }
    val angle = 135f + value * 270f // Radial sweep from 0% to 100%
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val sensitivity = 120f // pixels for full range sweep
                        val delta = -dragAmount.y / sensitivity
                        val newValue = (currentValue + delta).coerceIn(0f, 1f)
                        currentOnValueChange(newValue)
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .size(32.dp) // Perfect compact hardware rack size
                .background(Color(0xFF141416), shape = CircleShape)
                .border(1.dp, Color(0xFF2D2D30), shape = CircleShape)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2
                
                // Track arc
                drawArc(
                    color = Color(0xFF07070A),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Value arc
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = value * 270f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Level pointer needle
                val angleRad = Math.toRadians(angle.toDouble())
                val endX = center.x + Math.cos(angleRad).toFloat() * (radius - 2.dp.toPx())
                val endY = center.y + Math.sin(angleRad).toFloat() * (radius - 2.dp.toPx())
                
                drawLine(
                    color = Color.White,
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 7.sp, color = Color.Gray),
            modifier = Modifier.padding(top = 1.dp)
        )
        Text(
            text = "${(value * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 7.sp, color = color, fontFamily = FontFamily.Monospace),
            modifier = Modifier.padding(top = 0.5.dp)
        )
    }
}

@Composable
fun AdvancedControlsModule(
    deckA: DeckState,
    deckB: DeckState,
    onPitchChangeA: (Float) -> Unit,
    onPitchChangeB: (Float) -> Unit,
    onPitchDeltaA: (Float) -> Unit,
    onPitchDeltaB: (Float) -> Unit,
    onCueSetA: () -> Unit,
    onCueSetB: () -> Unit,
    onCueJumpA: () -> Unit,
    onCueJumpB: () -> Unit,
    onLoopToggleA: () -> Unit,
    onLoopToggleB: () -> Unit,
    isLandscape: Boolean = false,
    
    // FX states and setters
    fx1ActiveA: Boolean, fx1AmountA: Float, onFx1ChangeA: (Boolean, Float) -> Unit,
    fx2ActiveA: Boolean, fx2AmountA: Float, onFx2ChangeA: (Boolean, Float) -> Unit,
    fx3ActiveA: Boolean, fx3AmountA: Float, onFx3ChangeA: (Boolean, Float) -> Unit,
    
    fx1ActiveB: Boolean, fx1AmountB: Float, onFx1ChangeB: (Boolean, Float) -> Unit,
    fx2ActiveB: Boolean, fx2AmountB: Float, onFx2ChangeB: (Boolean, Float) -> Unit,
    fx3ActiveB: Boolean, fx3AmountB: Float, onFx3ChangeB: (Boolean, Float) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderWhiteTranslucent),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = TechnoOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PERFORMANCE SUITE (PRO-FX / PITCH / CUE / LOOPS)",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TechnoOrange,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Deck A panel (Cyan border)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(MixerBg, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, BorderWhiteTranslucent, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, CyberCyan.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(" deck A ".uppercase(), color = CyberCyan, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))

                    // PITCH SPEED SLIDER
                    Text("PITCH SLIDER (SPEED)", fontSize = 8.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onPitchDeltaA(-0.02f) }, modifier = Modifier.size(28.dp)) {
                            Text("-", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Slider(
                            value = deckA.pitch,
                            onValueChange = onPitchChangeA,
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan),
                            modifier = Modifier.weight(1f).testTag("pitch_slider_a")
                        )
                        IconButton(onClick = { onPitchDeltaA(0.02f) }, modifier = Modifier.size(28.dp)) {
                            Text("+", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    Text(
                        text = "Fila: ${String.format("%.2f", deckA.pitch)}x (Valor normal: 1.00x)",
                        fontSize = 9.sp,
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // CUE NODES
                    Text("HOT CUE SYSTEM", fontSize = 8.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onCueSetA,
                            colors = ButtonDefaults.buttonColors(containerColor = ControlGray),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f).height(30.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("SET CUE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onCueJumpA,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f).height(30.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("JUMP CUE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // TRANS LOOP MODE
                    Text("AUTOMATIC TRACK LOOP", fontSize = 8.sp, color = Color.Gray)
                    Button(
                        onClick = onLoopToggleA,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (deckA.isLooping) Color(0xFFEF5350) else ControlGray
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(30.dp).padding(top = 2.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (deckA.isLooping) "LOOP ACTIVO" else "LOOP COMPLETAMENTE OFF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3 DECK FX
                    Text("DECK A - FX PROCESSOR", fontSize = 8.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Effect 1: ECHO
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Button(
                                onClick = { onFx1ChangeA(!fx1ActiveA, fx1AmountA) },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (fx1ActiveA) CyberCyan else ControlGray,
                                    contentColor = if (fx1ActiveA) Color.Black else TextSlate200
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp).fillMaxWidth().padding(horizontal = 2.dp)
                            ) {
                                Text("ECHO", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            FxKnob(
                                label = "AMOUNT",
                                value = fx1AmountA,
                                onValueChange = { onFx1ChangeA(fx1ActiveA, it) },
                                color = CyberCyan
                            )
                        }

                        // Effect 2: FILTER
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Button(
                                onClick = { onFx2ChangeA(!fx2ActiveA, fx2AmountA) },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (fx2ActiveA) CyberCyan else ControlGray,
                                    contentColor = if (fx2ActiveA) Color.Black else TextSlate200
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp).fillMaxWidth().padding(horizontal = 2.dp)
                            ) {
                                Text("FILTER", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            FxKnob(
                                label = "DEPTH",
                                value = fx2AmountA,
                                onValueChange = { onFx2ChangeA(fx2ActiveA, it) },
                                color = CyberCyan
                            )
                        }

                        // Effect 3: FLANGER
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Button(
                                onClick = { onFx3ChangeA(!fx3ActiveA, fx3AmountA) },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (fx3ActiveA) CyberCyan else ControlGray,
                                    contentColor = if (fx3ActiveA) Color.Black else TextSlate200
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp).fillMaxWidth().padding(horizontal = 2.dp)
                            ) {
                                Text("FLANGE", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            FxKnob(
                                label = "RATE",
                                value = fx3AmountA,
                                onValueChange = { onFx3ChangeA(fx3ActiveA, it) },
                                color = CyberCyan
                            )
                        }
                    }
                }

                // Deck B panel (Orange border)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(MixerBg, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, BorderWhiteTranslucent, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, TechnoOrange.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(" deck B ".uppercase(), color = TechnoOrange, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))

                    // PITCH SPEED SLIDER
                    Text("PITCH SLIDER (SPEED)", fontSize = 8.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onPitchDeltaB(-0.02f) }, modifier = Modifier.size(28.dp)) {
                            Text("-", color = TechnoOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Slider(
                            value = deckB.pitch,
                            onValueChange = onPitchChangeB,
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(activeTrackColor = TechnoOrange, thumbColor = TechnoOrange),
                            modifier = Modifier.weight(1f).testTag("pitch_slider_b")
                        )
                        IconButton(onClick = { onPitchDeltaB(0.02f) }, modifier = Modifier.size(28.dp)) {
                            Text("+", color = TechnoOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    Text(
                        text = "Fila: ${String.format("%.2f", deckB.pitch)}x (Valor normal: 1.00x)",
                        fontSize = 9.sp,
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // CUE NODES
                    Text("HOT CUE SYSTEM", fontSize = 8.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onCueSetB,
                            colors = ButtonDefaults.buttonColors(containerColor = ControlGray),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f).height(30.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("SET CUE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onCueJumpB,
                            colors = ButtonDefaults.buttonColors(containerColor = TechnoOrange, contentColor = Color.Black),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f).height(30.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("JUMP CUE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // TRANS LOOP MODE
                    Text("AUTOMATIC TRACK LOOP", fontSize = 8.sp, color = Color.Gray)
                    Button(
                        onClick = onLoopToggleB,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (deckB.isLooping) Color(0xFFEF5350) else ControlGray
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(30.dp).padding(top = 2.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (deckB.isLooping) "LOOP ACTIVO" else "LOOP COMPLETAMENTE OFF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3 DECK FX
                    Text("DECK B - FX PROCESSOR", fontSize = 8.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Effect 1: REVERB
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Button(
                                onClick = { onFx1ChangeB(!fx1ActiveB, fx1AmountB) },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (fx1ActiveB) TechnoOrange else ControlGray,
                                    contentColor = if (fx1ActiveB) Color.Black else TextSlate200
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp).fillMaxWidth().padding(horizontal = 2.dp)
                            ) {
                                Text("REVERB", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            FxKnob(
                                label = "SPACE",
                                value = fx1AmountB,
                                onValueChange = { onFx1ChangeB(fx1ActiveB, it) },
                                color = TechnoOrange
                            )
                        }

                        // Effect 2: BITCRUSH
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Button(
                                onClick = { onFx2ChangeB(!fx2ActiveB, fx2AmountB) },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (fx2ActiveB) TechnoOrange else ControlGray,
                                    contentColor = if (fx2ActiveB) Color.Black else TextSlate200
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp).fillMaxWidth().padding(horizontal = 2.dp)
                            ) {
                                Text("CRUSH", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            FxKnob(
                                label = "LO-FI",
                                value = fx2AmountB,
                                onValueChange = { onFx2ChangeB(fx2ActiveB, it) },
                                color = TechnoOrange
                            )
                        }

                        // Effect 3: PHASER
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Button(
                                onClick = { onFx3ChangeB(!fx3ActiveB, fx3AmountB) },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (fx3ActiveB) TechnoOrange else ControlGray,
                                    contentColor = if (fx3ActiveB) Color.Black else TextSlate200
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp).fillMaxWidth().padding(horizontal = 2.dp)
                            ) {
                                Text("PHASE", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            FxKnob(
                                label = "SPEED",
                                value = fx3AmountB,
                                onValueChange = { onFx3ChangeB(fx3ActiveB, it) },
                                color = TechnoOrange
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpotifySettingsDialog(
    initialClientId: String,
    initialClientSecret: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var clientId by remember { mutableStateOf(initialClientId) }
    var clientSecret by remember { mutableStateOf(initialClientSecret) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        titleContentColor = Color.White,
        textContentColor = Color.LightGray,
        title = { Text("Configuración Spotify Developer App", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(modifier = Modifier.width(340.dp)) {
                Text(
                    text = "Para buscar y hacer streaming de Spotify, necesitas ingresar tus credenciales de Spotify Developer dashboard (developer.spotify.com).",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = it },
                    label = { Text("Client ID") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1DB954),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = clientSecret,
                    onValueChange = { clientSecret = it },
                    label = { Text("Client Secret") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1DB954),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // Technical Guide details
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F0F14), shape = RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "PASOS:\n1. Ve a developer.spotify.com.\n2. Inicia sesión y crea una App.\n3. Copia las claves y pégalas aquí.",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(clientId, clientSecret) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

// Custom EQ rotary dial knob component
@Composable
fun EqKnob(
    label: String,
    value: Float, // -12f to 12f
    range: ClosedRange<Float> = -12f..12f,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    var dragAccumulator by remember { mutableStateOf(0f) }
    
    val percentage = (value - range.start) / (range.endInclusive - range.start)
    val angle = 135f + percentage * 270f // Radial angle sweep
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val sensitivity = 150f // pixels for full range
                        val delta = -dragAmount.y / sensitivity * (range.endInclusive - range.start)
                        val newValue = (currentValue + delta).coerceIn(range.start, range.endInclusive)
                        currentOnValueChange(newValue)
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF16161D), shape = CircleShape)
                .border(1.5.dp, Color(0xFF282834), shape = CircleShape)
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2
                
                // Draw inactive radial track
                drawArc(
                    color = Color(0xFF0B0B0E),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Draw active color radial sweep
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = percentage * 270f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Draw rotating marker level indicator
                val angleRad = Math.toRadians(angle.toDouble())
                val endX = center.x + Math.cos(angleRad).toFloat() * (radius - 3.dp.toPx())
                val endY = center.y + Math.sin(angleRad).toFloat() * (radius - 3.dp.toPx())
                
                drawLine(
                    color = Color.White,
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp, color = Color.Gray),
            modifier = Modifier.padding(top = 1.dp)
        )
        Text(
            text = "${if (value >= 0) "+" else ""}${String.format("%.1f", value)}dB",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 7.sp, color = color, fontFamily = FontFamily.Monospace),
            modifier = Modifier.padding(top = 0.5.dp)
        )
    }
}

// Waveform visual progress indicator
@Composable
fun AudioProgressWaveform(
    currentPositionMs: Int,
    durationMs: Int,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFFFF5722)
) {
    val progress = if (durationMs > 0) currentPositionMs.toFloat() / durationMs else 0f
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(Color(0xFF08080C), shape = RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF1B1B22), shape = RoundedCornerShape(4.dp))
            .pointerInput(durationMs) {
                if (durationMs > 0) {
                    detectTapGestures { offset ->
                        val relativeX = offset.x / size.width
                        onSeek(relativeX * durationMs)
                    }
                }
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        Canvas(modifier = Modifier.fillMaxSize()) {
            val numBars = 36
            val barSpacing = 3.dp.toPx()
            val availableWidth = size.width
            val barWidth = (availableWidth - (numBars - 1) * barSpacing) / numBars
            
            for (i in 0 until numBars) {
                // Generate simulated electronic beat peaks
                val heightScale = (Math.sin(i * 0.3) * 0.4 + Math.cos(i * 0.15) * 0.3 + 0.3).toFloat().coerceIn(0.15f, 1.0f)
                val barHeight = size.height * 0.75f * heightScale
                val x = i * (barWidth + barSpacing)
                
                val isActive = (i.toFloat() / numBars) <= progress
                val color = if (isActive) activeColor else Color(0xFF24242F)
                
                drawLine(
                    color = color,
                    start = Offset(x + barWidth / 2, (size.height - barHeight) / 2),
                    end = Offset(x + barWidth / 2, (size.height + barHeight) / 2),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

// 60FPS Rotation Mechanical Platter
@Composable
fun VinylPlatter(
    isPlaying: Boolean,
    progress: Float,
    albumArtUrl: String?,
    modifier: Modifier = Modifier,
    platterColor: Color = Color(0xFFFF5722)
) {
    var rotationAngle by remember { mutableStateOf(0f) }
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                rotationAngle = (rotationAngle + 2.4f) % 360f
                delay(16) // ~60fps rotation
            }
        }
    }
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color(0xFF07070A), shape = CircleShape)
            .border(2.5.dp, Color(0xFF1E1E26), shape = CircleShape)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotationAngle)
                .background(Color(0xFF0F0F12), shape = CircleShape)
                .border(0.5.dp, platterColor.copy(alpha = 0.3f), shape = CircleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(color = Color.Black, radius = size.width / 2)
                drawCircle(color = Color(0xFF17171E), radius = size.width / 3, style = Stroke(width = 0.5.dp.toPx()))
                drawCircle(color = Color(0xFF17171E), radius = size.width / 4, style = Stroke(width = 0.5.dp.toPx()))
                
                // Outer tracking marker stripe
                drawLine(
                    color = platterColor.copy(alpha = 0.6f),
                    start = center,
                    end = Offset(center.x, 3.dp.toPx()),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.Center)
                    .background(Color(0xFF1F1F27), shape = CircleShape)
                    .border(1.dp, Color.Black, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (albumArtUrl != null) {
                    AsyncImage(
                        model = albumArtUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = platterColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(Color.White, shape = CircleShape)
                        .border(0.5.dp, Color.Black, shape = CircleShape)
                )
            }
        }
    }
}

// Audio progression formatter
fun formatProgress(currentMs: Int, durationMs: Int): String {
    val totalSecs = currentMs / 1000
    val min = totalSecs / 60
    val sec = totalSecs % 60
    
    val dSecs = durationMs / 1000
    val dMin = dSecs / 60
    val dSec = dSecs % 60
    
    return "${min}:${String.format("%02d", sec)} / ${dMin}:${String.format("%02d", dSec)}"
}
