package com.topseven.fakty.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.topseven.fakty.R
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.topseven.fakty.data.models.Category
import com.topseven.fakty.data.models.FavoriteItem
import com.topseven.fakty.ui.theme.PrimaryAccent
import com.topseven.fakty.ui.screens.MainViewModel
import com.topseven.fakty.ui.components.GlossaryText
import com.topseven.fakty.ui.components.GlossaryBottomSheet

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import com.topseven.fakty.utils.StopTtsWhenScreenLeaves

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FactDetailScreen(
    viewModel: MainViewModel,
    items: List<FavoriteItem>,
    initialFactId: Int,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBackClick: () -> Unit
) {
    val factIndex = items.indexOfFirst { it.fact.id == initialFactId }.takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(
        initialPage = factIndex,
        pageCount = { items.size }
    )

    val favorites by viewModel.favorites.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.initTts(context)
    }
    val ttsManager by viewModel.ttsManager.collectAsState()

    val fallbackFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(false) }
    val isTtsReady by (ttsManager?.isReady ?: fallbackFlow).collectAsState()
    val isTtsPlaying by (ttsManager?.isPlaying ?: fallbackFlow).collectAsState()
    val haptic = LocalHapticFeedback.current

    StopTtsWhenScreenLeaves(ttsManager)

    // Zatrzymujemy czytanie, jeśli użytkownik zmieni stronę (fakt)
    LaunchedEffect(pagerState.currentPage) {
        ttsManager?.stop()
    }

    val currentItem = items[pagerState.currentPage]
    val currentFact = currentItem.fact
    val category = currentItem.category
    val isFavorite = favorites.any { it.categoryId == category.id && it.factId == currentFact.id }
    
    var selectedGlossaryTerm by remember { mutableStateOf<Pair<String, String>?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageItem = items[page]
            val fact = pageItem.fact
            val pageCategory = pageItem.category
            val imagePath = fact.imageUrl?.let { "file:///android_asset/images/facts/$it" }
                ?: "file:///android_asset/images/${pageCategory.icon}"

            val glassGradient = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )

            Box(modifier = Modifier.fillMaxSize()) {
                // Tło ze zdjęciem (pełna szerokość, 60% wysokości)
                with(sharedTransitionScope) {
                    AsyncImage(
                        model = imagePath,
                        contentDescription = fact.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.6f)
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = "fact-image-${fact.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                    )
                }

                // Pionowy gradient płynnie wtapiający zdjęcie w tło
                with(sharedTransitionScope) {
                    with(animatedVisibilityScope) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.6f)
                                .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 0.5f)
                                .animateEnterExit(
                                    enter = fadeIn(tween(400)),
                                    exit = fadeOut(tween(400))
                                )
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                                    )
                                )
                        )
                    }
                }
                
                // Treść faktu (przewijana, nakładająca się na zdjęcie)
                with(sharedTransitionScope) {
                    with(animatedVisibilityScope) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f)
                                .animateEnterExit(
                                    enter = slideInHorizontally(tween(400)) { it / 4 } + fadeIn(tween(400)),
                                    exit = slideOutHorizontally(tween(400)) { it / 4 } + fadeOut(tween(400))
                                )
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
                            Spacer(modifier = Modifier.height(screenHeight * 0.4f)) // Wypycha treść na dół

                            Text(
                                text = fact.title,
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Szklana karta
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(32.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp)),
                                color = Color.Transparent
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(glassGradient)
                                        .padding(32.dp)
                                ) {
                                    GlossaryText(
                                        text = fact.details,
                                        glossary = fact.glossary,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Start,
                                        lineHeight = 28.sp,
                                        onTermClick = { term, definition ->
                                            selectedGlossaryTerm = term to definition
                                        }
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(48.dp))
                        }
                    }
                }
            }
        }
        
        // Lewitujący TopAppBar, niezależny od pagera (stoi w miejscu)
        with(sharedTransitionScope) {
            with(animatedVisibilityScope) {
                TopAppBar(
                    modifier = Modifier
                        .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 2f)
                        .animateEnterExit(
                            enter = slideInHorizontally(tween(400)) { it / 4 } + fadeIn(tween(400)),
                            exit = slideOutHorizontally(tween(400)) { it / 4 } + fadeOut(tween(400))
                        )
                        .statusBarsPadding(),
                    title = {
                        Text(
                            "${pagerState.currentPage + 1} / ${items.size}",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryAccent
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBackClick()
                            },
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(50))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.content_desc_back), tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    ),
                    actions = {
                        if (isTtsReady) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (isTtsPlaying) {
                                        ttsManager?.stop()
                                    } else {
                                        ttsManager?.speak(currentFact.details)
                                    }
                                },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(50))
                            ) {
                                Icon(
                                    imageVector = if (isTtsPlaying) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    // Opis musi odzwierciedlać stan, inaczej TalkBack czyta
                                    // "Czytaj fakt" także wtedy, gdy przycisk zatrzymuje lektora.
                                    contentDescription = stringResource(
                                        id = if (isTtsPlaying) R.string.content_desc_stop_reading
                                        else R.string.content_desc_read_fact
                                    ),
                                    tint = Color.White
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.toggleFavorite(category.id, currentFact.id, isFavorite)
                            },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(50))
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = stringResource(id = R.string.content_desc_favorites),
                                tint = if (isFavorite) PrimaryAccent else Color.White
                            )
                        }
                    }
                )
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
