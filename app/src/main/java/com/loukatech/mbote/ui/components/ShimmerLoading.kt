package com.loukatech.mbote.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Animated Shimmer Brush for loading skeleton views.
 */
@Composable
fun shimmerBrush(
    showShimmer: Boolean = true,
    targetValue: Float = 1200f
): Brush {
    return if (showShimmer) {
        val isDark = MaterialTheme.colorScheme.surface.hashCode() < 0 // adapt for dark theme
        val baseColor = if (isDark) Color.DarkGray.copy(alpha = 0.4f) else Color.LightGray.copy(alpha = 0.35f)
        val highlightColor = if (isDark) Color.Gray.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.85f)

        val shimmerColors = listOf(
            baseColor,
            highlightColor,
            baseColor
        )

        val transition = rememberInfiniteTransition(label = "shimmerTransition")
        val translateAnimation by transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerTranslate"
        )

        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnimation - 350f, translateAnimation - 350f),
            end = Offset(translateAnimation, translateAnimation)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent)
        )
    }
}

/**
 * Single Feed Post Skeleton item using shimmer brush
 */
@Composable
fun FeedItemSkeleton(brush: Brush = shimmerBrush()) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Avatar + Name & Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Post content text lines skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Post Image Skeleton Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Footer Action Row Skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brush)
                )
            }
        }
    }
}

/**
 * Column of Feed Skeleton Posts for Shimmer Loading
 */
@Composable
fun FeedSkeletonList(count: Int = 3, brush: Brush = shimmerBrush()) {
    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(count) {
            FeedItemSkeleton(brush = brush)
        }
    }
}

/**
 * Single Message List Row Skeleton item using shimmer brush
 */
@Composable
fun MessageItemSkeleton(brush: Brush = shimmerBrush()) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(brush)
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Name, Message & Time
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
    }
}

/**
 * Column of Message List Skeleton rows for Shimmer Loading
 */
@Composable
fun MessageListSkeleton(count: Int = 6, brush: Brush = shimmerBrush()) {
    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(count) { index ->
            MessageItemSkeleton(brush = brush)
            if (index < count - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 82.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            }
        }
    }
}
