package com.loukatech.mbote.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loukatech.mbote.R
import com.loukatech.mbote.ui.theme.MbotePurplePrimary

private data class LaunchOnboardingSlide(
    val title: String,
    val description: String,
    @DrawableRes val imageRes: Int
)

private val launchOnboardingSlides = listOf(
    LaunchOnboardingSlide(
        title = "Bienvenue sur MBoté",
        description = "Discutez, publiez et retrouvez votre communauté dans une seule application.",
        imageRes = R.drawable.mbote_login_watermark
    ),
    LaunchOnboardingSlide(
        title = "Connectez-vous à vos proches",
        description = "Messages privés, amis, statuts et contenus publics restent séparés et sécurisés.",
        imageRes = R.drawable.mbote_onboarding_slide_2
    ),
    LaunchOnboardingSlide(
        title = "Créez votre espace",
        description = "Partagez des actus, shorts, chaînes et réunions avec une expérience mobile fluide.",
        imageRes = R.drawable.mbote_onboarding_slide_3
    )
)

@Composable
fun MboteLaunchSplashScreen(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mbote-splash")
    val scale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mbote-splash-scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "MBoté",
            color = MbotePurplePrimary,
            fontSize = 58.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.scale(scale)
        )
    }
}

@Composable
fun MboteLaunchOnboardingScreen(
    onFinish: () -> Unit,
    onSkip: () -> Unit = onFinish,
    modifier: Modifier = Modifier
) {
    var activeIndex by remember { mutableIntStateOf(0) }
    val slide = launchOnboardingSlides.getOrElse(activeIndex) { launchOnboardingSlides.first() }
    val isLast = activeIndex >= launchOnboardingSlides.lastIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF2E1065), Color(0xFF13061F))
                )
            )
    ) {
        AnimatedContent(
            targetState = slide,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "mbote-onboarding-slide"
        ) { currentSlide ->
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White)
                ) {
                    Image(
                        painter = painterResource(currentSlide.imageRes),
                        contentDescription = currentSlide.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x33A78BFA), Color.Transparent, Color.White)
                                )
                            )
                    )
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(18.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.88f),
                            tonalElevation = 6.dp
                        ) {
                            Text(
                                text = "Passer",
                                color = MbotePurplePrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .navigationBarsPadding()
                        .padding(horizontal = 28.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LOUKATECH",
                        color = Color(0xFF8B5CF6),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.4.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = currentSlide.title,
                        color = Color(0xFF0F172A),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = currentSlide.description,
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        launchOnboardingSlides.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(if (index == activeIndex) 32.dp else 8.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (index == activeIndex) MbotePurplePrimary else Color(0xFFE9D5FF))
                            )
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = {
                            if (isLast) onFinish() else activeIndex += 1
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = if (isLast) "Commencer" else "Suivant",
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.size(10.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
