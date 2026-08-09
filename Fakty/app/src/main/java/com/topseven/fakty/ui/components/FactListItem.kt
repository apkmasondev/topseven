package com.topseven.fakty.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topseven.fakty.data.models.Fact
import com.topseven.fakty.ui.theme.PrimaryAccent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FactListItem(
    fact: Fact,
    index: Int,
    categoryId: String,
    categoryIcon: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1f, label = "scale")
    val context = LocalContext.current

    val imagePath = if (!fact.imageUrl.isNullOrEmpty()) {
        "file:///android_asset/images/facts/${fact.imageUrl}"
    } else {
        "file:///android_asset/images/$categoryIcon"
    }

    val glassGradient = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .semantics(mergeDescendants = true) {}
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            ),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .background(glassGradient)
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                with(sharedTransitionScope) {
                    AsyncImage(
                        model = imagePath,
                        // Dekoracyjne - tytuł faktu jest obok jako zwykły tekst.
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .sharedElement(
                                // Klucz zawiera id kategorii, żeby był jednoznaczny nawet
                                // gdyby dane ponownie zawierały ten sam numer faktu
                                // w dwóch kategoriach.
                                sharedContentState = rememberSharedContentState(
                                    key = "fact-image-$categoryId-${fact.id}"
                                ),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                    )
                }
                
                // Badge z numerkiem
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(20.dp)
                        .background(PrimaryAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fact.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = fact.shortDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
