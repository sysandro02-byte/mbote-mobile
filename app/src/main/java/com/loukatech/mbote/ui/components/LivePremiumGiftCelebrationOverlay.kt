package com.loukatech.mbote.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

data class CelebrationParticle(
    val id: Int,
    val startX: Float,
    val startY: Float,
    val speedX: Float,
    val speedY: Float,
    val color: Color,
    val size: Float,
    val emoji: String
)

@Composable
fun LivePremiumGiftCelebrationOverlay(
    senderName: String,
    giftName: String,
    emoji: String,
    valueFcfa: Long,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulsing & scaling animation for the giant gift icon
    val infiniteTransition = rememberInfiniteTransition(label = "gift_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_rotation"
    )

    // Animated particles
    val particles = remember {
        val particleEmojis = listOf("✨", "⭐", "💎", "🪙", "🔥", "👑", "🎉")
        val particleColors = listOf(
            Color(0xFFFFD700),
            Color(0xFF00E5FF),
            Color(0xFFFFB300),
            Color(0xFF8B5CF6),
            Color.White
        )
        List(32) { i ->
            CelebrationParticle(
                id = i,
                startX = Random.nextFloat(),
                startY = Random.nextFloat(),
                speedX = (Random.nextFloat() - 0.5f) * 0.3f,
                speedY = -(Random.nextFloat() * 0.4f + 0.2f),
                color = particleColors.random(),
                size = (14..28).random().toFloat(),
                emoji = particleEmojis.random()
            )
        }
    }

    // Auto-dismiss after 4.5s
    LaunchedEffect(Unit) {
        delay(4500)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.38f),
                        Color.Black.copy(alpha = 0.75f),
                        Color.Black.copy(alpha = 0.90f)
                    ),
                    radius = 900f
                )
            )
            .testTag("live_premium_gift_celebration_overlay"),
        contentAlignment = Alignment.Center
    ) {
        // Rotating Sunburst Rays on Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            rotate(haloRotation, pivot = center) {
                val rayCount = 16
                for (i in 0 until rayCount) {
                    val angle = (i * 360f / rayCount)
                    rotate(angle, pivot = center) {
                        drawLine(
                            color = Color(0xFFFFD700).copy(alpha = 0.12f),
                            start = center,
                            end = Offset(center.x + size.width, center.y),
                            strokeWidth = 24f
                        )
                    }
                }
            }
        }

        // Particle Bursts Overlay
        Box(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val infiniteAnim = rememberInfiniteTransition(label = "particle_${p.id}")
                val animYOffset by infiniteAnim.animateFloat(
                    initialValue = 0f,
                    targetValue = -350f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2200 + (p.id * 80), easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "p_y_${p.id}"
                )
                val animAlpha by infiniteAnim.animateFloat(
                    initialValue = 1f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2200 + (p.id * 80), easing = FastOutLinearInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "p_alpha_${p.id}"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(
                            x = ((p.startX * 340) - 170).dp,
                            y = (((p.startY * 450) - 225) + (animYOffset / 4)).dp
                        )
                        .alpha(animAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = p.emoji, fontSize = p.size.sp)
                }
            }
        }

        // Central Spotlight Card & Pulsing Gift Emblem
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Golden Fanfare Tag
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFFD700),
                border = BorderStroke(2.dp, Color.White),
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🌟", fontSize = 14.sp)
                    Text(
                        text = "DONATION SPECTACULAIRE !",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.5.sp,
                        letterSpacing = 1.sp
                    )
                    Text("🌟", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Pulsing Giant 3D Gift Emblem with Halo
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.5f),
                                Color(0xFF8B5CF6).copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    border = BorderStroke(3.dp, Color(0xFFFFD700)),
                    modifier = Modifier.size(105.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = emoji, fontSize = 56.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Info Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.75f),
                border = BorderStroke(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.7f)),
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700),
                        fontSize = 22.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "a offert",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(emoji, fontSize = 24.sp)
                        Text(
                            text = giftName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "+ $valueFcfa FCFA",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
