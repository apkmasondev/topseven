package com.topseven.fakty.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topseven.fakty.R
import androidx.compose.ui.draw.blur
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.topseven.fakty.data.models.Category
import com.topseven.fakty.data.models.Fact
import com.topseven.fakty.ui.theme.PrimaryAccent
import com.topseven.fakty.ui.components.GlossaryText
import com.topseven.fakty.ui.components.GlossaryBottomSheet

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import com.topseven.fakty.utils.StopTtsWhenScreenLeaves

private val WHITESPACE_REGEX = "\\s+".toRegex()

/** Czas pełnego obrotu fiszki 3D. */
private const val FLIP_DURATION_MS = 600

/** Połowa obrotu - moment, w którym karta stoi krawędzią do ekranu i można podmienić treść. */
private const val FLIP_HALF_DURATION_MS = 300L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is UiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is UiState.Error -> {
                Text(
                    text = stringResource(id = state.messageResId),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is UiState.Success -> {
                FlashcardsContent(categories = state.categories, viewModel = viewModel, onBackClick = onBackClick)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsContent(
    categories: List<Category>,
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    // Shuffled facts pool
    val allFacts = remember {
        categories.flatMap { cat -> cat.facts.map { cat to it } }.shuffled()
    }
    
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        viewModel.initTts(context)
    }
    val ttsManager by viewModel.ttsManager.collectAsState()
    val fallbackFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(false) }
    val isTtsReady by (ttsManager?.isReady ?: fallbackFlow).collectAsState()
    val isTtsPlaying by (ttsManager?.isPlaying ?: fallbackFlow).collectAsState()

    StopTtsWhenScreenLeaves(ttsManager)

    var currentIndex by remember { mutableIntStateOf(0) }

    // Zatrzymujemy TTS podczas przesuwania na nową fiszkę
    LaunchedEffect(currentIndex) {
        ttsManager?.stop()
    }

    var isFlipped by remember { mutableStateOf(false) }
    var selectedGlossaryTerm by remember { mutableStateOf<Pair<String, String>?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Przejście do następnej fiszki po odwróceniu karty jest opóźnione o pół obrotu.
    // Bez trzymania referencji do zadania każde kolejne kliknięcie startowało nową
    // korutynę i licznik przeskakiwał o kilka kart naraz.
    val advanceJob = remember { mutableStateOf<Job?>(null) }
    DisposableEffect(Unit) {
        onDispose { advanceJob.value?.cancel() }
    }

    val currentPair = if (allFacts.isNotEmpty()) allFacts[currentIndex % allFacts.size] else null

    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic Blurred Background
        if (currentPair != null) {
            val (category, fact) = currentPair
            val imagePath = if (isFlipped) {
                fact.imageUrl?.let { "file:///android_asset/images/facts/$it" }
                    ?: "file:///android_asset/images/${category.icon}"
            } else {
                "file:///android_asset/images/${category.icon}"
            }
            val backgroundRequest = remember(imagePath) {
                ImageRequest.Builder(context)
                    .data(imagePath)
                    .size(200) // Wymuszenie wielokrotnie mniejszego obrazka dla GPU
                    .build()
            }

            AsyncImage(
                model = backgroundRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 120.dp, edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
            )
            // Add a semi-transparent dark overlay to ensure text is readable
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            TopAppBar(
                title = { Text(stringResource(id = R.string.mode_flashcards), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.content_desc_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )

            if (currentPair != null) {
                val (category, fact) = currentPair
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Flashcard(
                        fact = fact,
                        category = category,
                        isFlipped = isFlipped,
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isFlipped = !isFlipped 
                        },
                        onGlossaryTermClick = { term, definition ->
                            selectedGlossaryTerm = term to definition
                        },
                        isTtsReady = isTtsReady,
                        isTtsPlaying = isTtsPlaying,
                        onPlayClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (isTtsPlaying) {
                                ttsManager?.stop()
                            } else {
                                ttsManager?.speak(fact.details)
                            }
                        }
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (isTtsPlaying) ttsManager?.stop()
                            if (isFlipped) {
                                isFlipped = false
                                // Podmieniamy dane dokładnie w połowie obrotu (90 stopni),
                                // kiedy karta jest niewidoczna (krawędzią do ekranu).
                                advanceJob.value?.cancel()
                                advanceJob.value = coroutineScope.launch {
                                    delay(FLIP_HALF_DURATION_MS)
                                    currentIndex++
                                }
                            } else {
                                advanceJob.value?.cancel()
                                currentIndex++
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryAccent
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.next_flashcard),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
        
        selectedGlossaryTerm?.let { (term, definition) ->
            GlossaryBottomSheet(
                term = term,
                definition = definition,
                onDismissRequest = { selectedGlossaryTerm = null }
            )
        }
    }
}

@Composable
fun Flashcard(
    fact: Fact,
    category: Category,
    isFlipped: Boolean,
    onClick: () -> Unit,
    onGlossaryTermClick: (String, String) -> Unit,
    isTtsReady: Boolean = false,
    isTtsPlaying: Boolean = false,
    onPlayClick: () -> Unit = {}
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = FLIP_DURATION_MS),
        label = "flipAnimation"
    )

    // Odczyt `rotation` wprost w ciele kompozycji rekomponował całą kartę (razem
    // z parsowaniem słowniczka) w każdej z ~36 klatek obrotu. derivedStateOf sprawia,
    // że rekompozycja następuje tylko w momencie faktycznej zamiany stron karty,
    // a sama animacja zostaje w fazie rysowania (graphicsLayer).
    val showFront by remember { derivedStateOf { rotation <= 90f } }

    // Kamery (Paralax) do lepszego efektu głębi 3D
    val cameraDistance = 12f * LocalContext.current.resources.displayMetrics.density

    val glassGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    )

    val descFront = stringResource(id = R.string.content_desc_flashcard_front)
    val descBack = stringResource(id = R.string.content_desc_flashcard_back)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.cameraDistance = cameraDistance
                rotationY = rotation
            }
            .semantics(mergeDescendants = true) {
                contentDescription = if (isFlipped) descBack else descFront
            }
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
    ) {
        if (showFront) {
            // PRZÓD KARTY
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(32.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp)),
                color = Color.Transparent
            ) {
                val context = LocalContext.current
                val imagePath = "file:///android_asset/images/${category.icon}"
                
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imagePath)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Dark/Glass overlay for text readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .background(glassGradient)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                        Text(
                            text = category.title.uppercase(),
                            color = PrimaryAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(id = R.string.did_you_know),
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryAccent
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        val longestWordLength = fact.shortDescription.split(WHITESPACE_REGEX).maxOfOrNull { it.length } ?: 0
                        val scaleFactor = if (longestWordLength > 14) 0.75f else if (longestWordLength > 10) 0.85f else 1f
                        
                        GlossaryText(
                            text = fact.shortDescription,
                            glossary = fact.glossary,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp * scaleFactor,
                                hyphens = Hyphens.None,
                                lineBreak = LineBreak.Heading
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 42.sp * scaleFactor,
                            onTermClick = onGlossaryTermClick
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                        Text(
                            text = stringResource(id = R.string.tap_to_flip),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        }
                    }
                }
            }
        } else {
            // TYŁ KARTY (Obrócony o 180 stopni w kodzie układu)
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f } // Odwracamy tekst, żeby nie był odbiciem lustrzanym
                    .clip(RoundedCornerShape(32.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp)),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val context = LocalContext.current
                    val imagePath = fact.imageUrl?.let { "file:///android_asset/images/facts/$it" }
                        ?: "file:///android_asset/images/${category.icon}"

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imagePath)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surfaceVariant)
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = fact.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = PrimaryAccent,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GlossaryText(
                            text = fact.details,
                            glossary = fact.glossary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            onTermClick = onGlossaryTermClick,
                            // `fill = false` zachowuje przyklejenie treści do dołu karty dla
                            // krótkich faktów, a przy długich ogranicza wysokość do dostępnego
                            // miejsca i włącza przewijanie zamiast ucinać tekst na małych ekranach.
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    if (isTtsReady) {
                        IconButton(
                            onClick = onPlayClick,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(50))
                        ) {
                            Icon(
                                imageVector = if (isTtsPlaying) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = stringResource(
                                    id = if (isTtsPlaying) R.string.content_desc_stop_reading
                                    else R.string.content_desc_read_fact
                                ),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
