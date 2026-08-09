package com.topseven.fakty.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.topseven.fakty.data.models.Category
import androidx.compose.ui.res.stringResource
import com.topseven.fakty.R
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.lazy.LazyListState

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CategoryCard(
    category: Category,
    listState: LazyListState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "scale")
    
    // Glassmorphism gradient overlay for text readability
    val overlayGradient = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.4f),
            Color.Black.copy(alpha = 0.8f)
        )
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(24.dp))
            .semantics(mergeDescendants = true) {}
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Coil AsyncImage loading from local assets
            with(sharedTransitionScope) {
                AsyncImage(
                    model = "file:///android_asset/images/${category.icon}",
                    // Obraz jest czysto dekoracyjny. Wcześniejsze "Tło dla <kategoria>"
                    // trafiało do scalonego opisu karty i czytnik ekranu powtarzał nazwę
                    // kategorii dwa razy.
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == category.id }
                            val itemOffset = itemInfo?.offset?.toFloat() ?: 0f
                            // Parallax effect: shift image slightly in opposite direction of scroll
                            translationY = itemOffset * -0.05f
                            // Scale up slightly to prevent edges from showing during translation
                            scaleX = 1.1f
                            scaleY = 1.1f
                        }
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "image-${category.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                )
            }

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayGradient)
            )

            // Text Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = category.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.seven_fascinating_facts),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
