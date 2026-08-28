package com.loukatech.mbote.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.loukatech.mbote.model.*
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class EphemeralReaction(
    val id: Long,
    val emoji: String,
    val xOffsetFraction: Float, // relative horizontal drift
    val size: Float = 32f
)

data class LiveGiftBannerData(
    val id: Long,
    val senderName: String,
    val giftName: String,
    val emoji: String,
    val valueFcfa: Long,
    val isPremiumCelebration: Boolean = false
)

data class LiveComment(
    val id: String,
    val senderName: String,
    val text: String,
    val timestamp: String,
    val isGift: Boolean = false,
    val badgeType: BadgeType? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveBroadcastDialog(
    currentUserAvatar: String,
    userGiftState: UserGiftState = UserGiftState(),
    isPremiumUser: Boolean = true,
    userBadges: List<BadgeType> = listOf(BadgeType.VIP, BadgeType.CERTIFIED_CREATOR),
    onSendGift: (giftId: String, multiplier: Int) -> Boolean = { _, _ -> true },
    onBuyBundle: (GiftBundle, String) -> Unit = { _, _ -> },
    onBuySingleGift: (GiftItem, Int, String) -> Unit = { _, _, _ -> },
    onSimulateReceivedGift: (giftId: String, sender: String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isLiveStarted by remember { mutableStateOf(false) }
    var liveTitle by remember { mutableStateOf("🔴 Live MBoté - Échange en direct") }
    var viewerCount by remember { mutableStateOf(142) }
    var commentText by remember { mutableStateOf("") }

    // Dialog state
    var showGiftSheetInLive by remember { mutableStateOf(false) }
    var showStoreDialog by remember { mutableStateOf(false) }
    var showQuickReactionsBar by remember { mutableStateOf(true) }

    // Real-time gift banner overlay for broadcaster and viewers
    var activeGiftBanner by remember { mutableStateOf<LiveGiftBannerData?>(null) }
    var activeCelebrationGift by remember { mutableStateOf<LiveGiftBannerData?>(null) }

    // Fast-Send / Combo multiplier state for Premium accounts
    var fastSendMultiplier by remember { mutableStateOf(1) } // 1, 5, 10, 20
    var selectedFastSendGiftId by remember { mutableStateOf("g_diamond") }
    var comboCount by remember { mutableStateOf(0) }
    var lastComboTime by remember { mutableStateOf(0L) }

    val quickReactionEmojis = remember {
        listOf("❤️", "🔥", "👏", "🇨🇬", "🚀", "😍", "🥳", "😂", "💎", "🪙")
    }

    val floatingReactions = remember { mutableStateListOf<EphemeralReaction>() }

    val comments = remember {
        mutableStateListOf(
            LiveComment("1", "Merveille K.", "Salut tout le monde ! Bienvenue sur le live 🇨🇬", "19:22", false, BadgeType.VIP),
            LiveComment("2", "Grâce N.", "Superbe initiative MBoté Live 🔥", "19:22", false, BadgeType.CERTIFIED_CREATOR),
            LiveComment("3", "Arnold M.", "A envoyé un 💎 Diamant !", "19:23", true, BadgeType.TOP_DONOR)
        )
    }

    val coroutineScope = rememberCoroutineScope()

    // Reset combo if idle for 3 seconds
    LaunchedEffect(comboCount) {
        if (comboCount > 0) {
            delay(3000)
            if (System.currentTimeMillis() - lastComboTime >= 2800) {
                comboCount = 0
            }
        }
    }

    // Helper to spawn floating reaction
    fun triggerReaction(emoji: String) {
        val newReaction = EphemeralReaction(
            id = System.currentTimeMillis() + Random.nextLong(10000),
            emoji = emoji,
            xOffsetFraction = (Random.nextFloat() - 0.5f) * 120f,
            size = (28..40).random().toFloat()
        )
        floatingReactions.add(newReaction)
        com.loukatech.mbote.service.MboteSocketManager.sendLiveReaction("default_live", "Moi", emoji)

        // Automatically clean up after animation duration
        coroutineScope.launch {
            delay(2600)
            floatingReactions.remove(newReaction)
        }
    }

    // Helper to trigger high-visibility Gift Notification Overlay and Fullscreen celebration for high-value gifts
    fun triggerGiftOverlay(senderName: String, giftName: String, emoji: String, valueFcfa: Long) {
        val isPremium = valueFcfa >= 3000L || giftName.contains("Couronne", ignoreCase = true) || giftName.contains("Diamant", ignoreCase = true)
        val banner = LiveGiftBannerData(
            id = System.currentTimeMillis(),
            senderName = senderName,
            giftName = giftName,
            emoji = emoji,
            valueFcfa = valueFcfa,
            isPremiumCelebration = isPremium
        )
        activeGiftBanner = banner

        if (isPremium) {
            activeCelebrationGift = banner
        }

        // Show Toast
        Toast.makeText(context, "🎁 $senderName a offert $emoji $giftName ($valueFcfa FCFA) !", Toast.LENGTH_SHORT).show()

        // Multiple floating reaction bursts
        repeat(if (isPremium) 8 else 4) {
            triggerReaction(emoji)
        }

        coroutineScope.launch {
            delay(3800)
            if (activeGiftBanner?.id == banner.id) {
                activeGiftBanner = null
            }
        }
    }

    // Observe incoming real-time WebSocket live-stream events
    LaunchedEffect(Unit) {
        com.loukatech.mbote.service.MboteSocketManager.liveStreamEvents.collect { event ->
            when (event.type) {
                "LIVE_COMMENT" -> {
                    val txt = event.payloadText
                    if (!txt.isNullOrBlank()) {
                        comments.add(
                            LiveComment(
                                id = event.timestamp.toString(),
                                senderName = event.senderName,
                                text = txt,
                                timestamp = "Maintenant"
                            )
                        )
                    }
                }
                "LIVE_REACTION" -> {
                    val emo = event.emoji
                    if (!emo.isNullOrBlank()) {
                        val newReaction = EphemeralReaction(
                            id = System.currentTimeMillis() + Random.nextLong(10000),
                            emoji = emo,
                            xOffsetFraction = (Random.nextFloat() - 0.5f) * 120f,
                            size = (28..40).random().toFloat()
                        )
                        floatingReactions.add(newReaction)
                        coroutineScope.launch {
                            delay(2600)
                            floatingReactions.remove(newReaction)
                        }
                    }
                }
                "LIVE_GIFT" -> {
                    val giftName = event.giftName ?: "Cadeau Virtuel"
                    val emoji = event.giftEmoji ?: "🎁"
                    val sender = event.senderName
                    val valFcfa = event.giftValueFcfa
                    comments.add(
                        LiveComment(
                            id = event.timestamp.toString(),
                            senderName = sender,
                            text = "A offert $emoji $giftName !",
                            timestamp = "Maintenant",
                            isGift = true
                        )
                    )
                    triggerGiftOverlay(sender, giftName, emoji, valFcfa)
                }
                "LIVE_VIEWER_COUNT" -> {
                    if (event.viewerCount > 0) {
                        viewerCount = event.viewerCount
                    }
                }
            }
        }
    }

    // Fast-Send handler without reopening menu
    fun executeFastSend(giftId: String, multiplier: Int) {
        val count = userGiftState.inventory[giftId] ?: 0
        val selectedGift = defaultGiftItems().find { it.id == giftId } ?: defaultGiftItems().first()

        if (count >= multiplier) {
            val success = onSendGift(giftId, multiplier)
            if (success) {
                comboCount += multiplier
                lastComboTime = System.currentTimeMillis()

                val giftLabel = if (multiplier > 1) "${selectedGift.name} (x$multiplier)" else selectedGift.name
                val totalCost = selectedGift.priceFcfa * multiplier

                comments.add(
                    LiveComment(
                        System.currentTimeMillis().toString(),
                        "Moi",
                        "A envoyé ${selectedGift.emoji} $giftLabel ! ${if (comboCount > 1) "🔥 x$comboCount COMBO" else ""}",
                        "Maintenant",
                        true,
                        userBadges.firstOrNull()
                    )
                )

                triggerGiftOverlay("Moi", giftLabel, selectedGift.emoji, totalCost)
                com.loukatech.mbote.service.MboteSocketManager.sendLiveGift(
                    streamId = "default_live",
                    senderName = "Moi",
                    giftId = giftId,
                    giftName = giftLabel,
                    emoji = selectedGift.emoji,
                    valueFcfa = totalCost
                )
            }
        } else {
            Toast.makeText(context, "Stock insuffisant pour $multiplier ${selectedGift.name}. Ouverture de la boutique...", Toast.LENGTH_SHORT).show()
            showStoreDialog = true
        }
    }

    // Calculate total gifts owned by the user
    val totalGiftsOwned = remember(userGiftState.inventory) {
        userGiftState.inventory.values.sum()
    }

    // Simulate incoming viewers, comments and simulated community gifts
    LaunchedEffect(isLiveStarted) {
        if (isLiveStarted) {
            var counter = 0
            while (true) {
                delay(3500)
                counter++
                viewerCount += (1..4).random()
                val randomNames = listOf("Christian L.", "Prisca B.", "Destin M.", "Sarah O.", "Junior K.", "Yannick M.", "Arnold M.")
                val randomMsgs = listOf("Incroyable ce direct !", "Force à vous 🇨🇬💪", "Joyeux live à tous", "Top qualité vidéo", "Toujours présent !")
                val randomSender = randomNames.random()
                
                // Occasional live gift from audience
                if (counter % 5 == 0) {
                    val highGifts = listOf(
                        Triple("g_diamond", "Diamant étincelant", "💎"),
                        Triple("g_gold_ring", "Bague en or", "💍"),
                        Triple("g_gold_bar", "Lingot d'or pur", "🪙")
                    )
                    val picked = highGifts.random()
                    val value = if (picked.first == "g_gold_bar") 10000L else if (picked.first == "g_diamond") 5000L else 3000L
                    
                    comments.add(
                        LiveComment(
                            System.currentTimeMillis().toString(),
                            randomSender,
                            "A envoyé un ${picked.third} ${picked.second} !",
                            "Maintenant",
                            true
                        )
                    )
                    onSimulateReceivedGift(picked.first, randomSender)
                    triggerGiftOverlay(randomSender, picked.second, picked.third, value)
                } else {
                    comments.add(
                        LiveComment(
                            System.currentTimeMillis().toString(),
                            randomSender,
                            randomMsgs.random(),
                            "Maintenant"
                        )
                    )
                    // Periodic automated ambient reactions
                    val ambientEmoji = listOf("❤️", "🔥", "👏", "🇨🇬", "😍").random()
                    triggerReaction(ambientEmoji)
                }
            }
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Background Live Video placeholder / simulated feed
            AsyncImage(
                model = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=800",
                contentDescription = "Live Video",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            // 1) TOP BAR: LIVE Badge, Viewers, GIFT BALANCE INDICATOR, Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Red
                    ) {
                        Text(
                            text = " en direct ",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Text(text = "$viewerCount", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // GIFT BALANCE INDICATOR (Requirement 1)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f)),
                    modifier = Modifier
                        .clickable { showGiftSheetInLive = true }
                        .testTag("gift_balance_indicator")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🎁", fontSize = 13.sp)
                        Text(
                            text = "$totalGiftsOwned Cadeaux",
                            color = Color(0xFFFFD700),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = CircleShape,
                            color = MbotePurplePrimary,
                            modifier = Modifier.size(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("+", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Quitter le Live", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            // 3) REAL-TIME GIFT NOTIFICATION OVERLAY (Requirement 3)
            AnimatedVisibility(
                visible = activeGiftBanner != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 54.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopCenter)
            ) {
                activeGiftBanner?.let { banner ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E1B4B).copy(alpha = 0.92f), // Deep indigo
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD700)),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("live_gift_overlay_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFFD700).copy(alpha = 0.25f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(banner.emoji, fontSize = 24.sp)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "🎉 ${banner.senderName}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "a offert un cadeau !",
                                        color = Color(0xFFFFD700),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "${banner.giftName} • ${banner.valueFcfa} FCFA",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text("✨", fontSize = 20.sp)
                        }
                    }
                }
            }

            // Center Content if not started
            if (!isLiveStarted) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MbotePurplePrimary.copy(alpha = 0.9f),
                        modifier = Modifier.size(90.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Sensors, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Lancer votre Live MBoté",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Diffusez en direct auprès de votre communauté, recevez des réactions éphémères en direct et des cadeaux virtuels (lingots d'or, diamants).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = liveTitle,
                        onValueChange = { liveTitle = it },
                        label = { Text("Titre du Live", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MbotePurplePrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            isLiveStarted = true
                            Toast.makeText(context, "Direct MBoté démarré avec succès !", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_live_button")
                    ) {
                        Text("Commencer la diffusion en direct", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            } else {
                // Floating Ephemeral Reactions Canvas Overlay (Bottom Right anchor)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp, end = 24.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    floatingReactions.forEach { reaction ->
                        FloatingReactionBubble(
                            key = reaction.id,
                            reaction = reaction
                        )
                    }
                }

                // Active Live Chat, Quick Reactions Bar & Controls
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Comments List
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(210.dp)
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(comments) { comment ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (comment.isGift) Color(0xFFFFD700).copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.45f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = comment.senderName,
                                            color = if (comment.isGift) Color(0xFFFFD700) else MbotePurplePrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        comment.badgeType?.let { badge ->
                                            UserBadgeChip(badge = badge, compact = true)
                                        }
                                        Text(
                                            text = comment.text,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Combo Multiplier Alert Banner when rapid tapping
                    AnimatedVisibility(
                        visible = comboCount > 1,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFFD700),
                            border = BorderStroke(2.dp, Color.White),
                            shadowElevation = 8.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(if (comboCount >= 10) "👑" else "⚡", fontSize = 16.sp)
                                Text(
                                    text = "x$comboCount COMBO ENVOI RAPIDE !",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.5.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    // --- FAST-SEND COMBO BAR (For Premium Users) ---
                    val fastGift = defaultGiftItems().find { it.id == selectedFastSendGiftId } ?: defaultGiftItems().first()
                    val fastCount = userGiftState.inventory[selectedFastSendGiftId] ?: 0

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, if (isPremiumUser) Color(0xFFFFD700).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(fastGift.emoji, fontSize = 20.sp)
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = fastGift.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        )
                                        if (isPremiumUser) {
                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFFD700)) {
                                                Text("⚡ RAPIDE", color = Color.Black, fontSize = 8.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                            }
                                        }
                                    }
                                    Text(
                                        text = "$fastCount en stock (${fastGift.priceFcfa} F)",
                                        color = if (fastCount > 0) Color(0xFF10B981) else Color.Red,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Multiplier Chips: x1, x5, x10, x20
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(1, 5, 10, 20).forEach { mult ->
                                    val isSelected = fastSendMultiplier == mult
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.15f),
                                        modifier = Modifier
                                            .clickable { fastSendMultiplier = mult }
                                    ) {
                                        Text(
                                            text = "x$mult",
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // Fast-Send Rapid Tap Fire Button
                                Button(
                                    onClick = {
                                        executeFastSend(selectedFastSendGiftId, fastSendMultiplier)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (fastCount >= fastSendMultiplier) Color(0xFFFFD700) else Color.Gray),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(32.dp).testTag("fast_send_button")
                                ) {
                                    Text(
                                        text = "⚡ Envoyer",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // --- QUICK REACTION BAR ---
                    AnimatedVisibility(
                        visible = showQuickReactionsBar,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("quick_reaction_bar")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Réagir :",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                                )

                                LazyRow(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(quickReactionEmojis) { emoji ->
                                        QuickReactionItem(
                                            emoji = emoji,
                                            onClick = {
                                                triggerReaction(emoji)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input & Action button row (Comment, Send, Quick Reactions Toggle, Gift, Store)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Écrire un commentaire...", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MbotePurplePrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Send Comment Button
                        if (commentText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        val textToSend = commentText.trim()
                                        comments.add(
                                            LiveComment(
                                                System.currentTimeMillis().toString(),
                                                "Moi",
                                                textToSend,
                                                "Maintenant",
                                                false,
                                                userBadges.firstOrNull()
                                            )
                                        )
                                        com.loukatech.mbote.service.MboteSocketManager.sendLiveComment(
                                            streamId = "default_live",
                                            senderName = "Moi",
                                            text = textToSend,
                                            badgeType = userBadges.firstOrNull()?.name
                                        )
                                        commentText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MbotePurplePrimary)
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = "Envoyer", tint = Color.White)
                            }
                        }

                        // Toggle Quick Reactions Bar
                        IconButton(
                            onClick = { showQuickReactionsBar = !showQuickReactionsBar },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (showQuickReactionsBar) MbotePurplePrimary.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.5f))
                        ) {
                            Text("⚡", fontSize = 18.sp)
                        }

                        // Send Virtual Gift Button (Opens Gift Selection / Inventory Sheet)
                        IconButton(
                            onClick = { showGiftSheetInLive = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD700))
                                .testTag("live_gift_button")
                        ) {
                            Text("🪙", fontSize = 20.sp)
                        }
                    }
                }
            }

            // Full-screen Spectacular Premium Gift Celebration Overlay
            activeCelebrationGift?.let { celebration ->
                LivePremiumGiftCelebrationOverlay(
                    senderName = celebration.senderName,
                    giftName = celebration.giftName,
                    emoji = celebration.emoji,
                    valueFcfa = celebration.valueFcfa,
                    onFinished = { activeCelebrationGift = null }
                )
            }
        }
    }

    // Live Gift Selection & Inventory Dialog
    if (showGiftSheetInLive) {
        val availableGifts = defaultGiftItems()
        AlertDialog(
            onDismissRequest = { showGiftSheetInLive = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎁 Envoyer un Cadeau en Direct", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                        modifier = Modifier.clickable {
                            showGiftSheetInLive = false
                            showStoreDialog = true
                        }
                    ) {
                        Text(
                            text = "+ Boutique",
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sélectionnez un cadeau de votre inventaire à offrir au diffuseur :",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    availableGifts.forEach { gift ->
                        val count = userGiftState.inventory[gift.id] ?: 0
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (count > 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            border = if (count > 0) androidx.compose.foundation.BorderStroke(1.dp, MbotePurplePrimary.copy(alpha = 0.4f)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (count > 0) {
                                        val success = onSendGift(gift.id, 1)
                                        if (success) {
                                            comments.add(
                                                LiveComment(
                                                    System.currentTimeMillis().toString(),
                                                    "Moi",
                                                    "A envoyé un ${gift.emoji} ${gift.name} !",
                                                    "Maintenant",
                                                    true,
                                                    userBadges.firstOrNull()
                                                )
                                            )
                                            triggerGiftOverlay("Moi", gift.name, gift.emoji, gift.priceFcfa)
                                            showGiftSheetInLive = false
                                        }
                                    } else {
                                        Toast.makeText(context, "Vous n'avez plus de ${gift.name}. Rendez-vous en boutique pour en acheter !", Toast.LENGTH_SHORT).show()
                                        showGiftSheetInLive = false
                                        showStoreDialog = true
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(gift.emoji, fontSize = 24.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = gift.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = "${gift.priceFcfa} FCFA", fontSize = 11.sp, color = MbotePurplePrimary)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (count > 0) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (count > 0) "$count en stock" else "0 (Acheter)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (count > 0) Color(0xFF10B981) else Color.Red,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGiftSheetInLive = false
                        showStoreDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)
                ) {
                    Text("Ouvrir la Boutique de Cadeaux", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGiftSheetInLive = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    // Gift Store Dialog (Requirement 4)
    if (showStoreDialog) {
        GiftStoreDialog(
            userGiftState = userGiftState,
            onBuyBundle = onBuyBundle,
            onBuySingleGift = onBuySingleGift,
            onDismiss = { showStoreDialog = false }
        )
    }
}

/**
 * Individual Quick Reaction button inside the horizontal reaction bar
 */
@Composable
private fun QuickReactionItem(
    emoji: String,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.35f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "reaction_scale"
    )

    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.12f),
        modifier = Modifier
            .size(36.dp)
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onClick()
            }
    ) {
        LaunchedEffect(isPressed) {
            if (isPressed) {
                delay(120)
                isPressed = false
            }
        }
        Box(contentAlignment = Alignment.Center) {
            Text(text = emoji, fontSize = 18.sp)
        }
    }
}

/**
 * Animated floating reaction that travels upwards with sway and fade-out
 */
@Composable
private fun FloatingReactionBubble(
    key: Long,
    reaction: EphemeralReaction
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(key) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 2400,
                easing = FastOutSlowInEasing
            )
        )
    }

    val currentProgress = progress.value
    // Float upwards up to 340dp
    val yOffset = -340.dp * currentProgress
    // Horizontal sway using sine wave
    val xOffset = (kotlin.math.sin(currentProgress * Math.PI.toFloat() * 3f) * 24f + reaction.xOffsetFraction).dp
    // Scale up at start then shrink slightly
    val scale = if (currentProgress < 0.2f) {
        currentProgress / 0.2f * 1.25f
    } else {
        1.25f - (currentProgress - 0.2f) * 0.4f
    }
    // Fade out as it reaches the top
    val alpha = if (currentProgress > 0.65f) {
        1f - (currentProgress - 0.65f) / 0.35f
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .offset(x = xOffset, y = yOffset)
            .scale(scale)
            .alpha(alpha.coerceIn(0f, 1f))
    ) {
        Text(
            text = reaction.emoji,
            fontSize = reaction.size.sp
        )
    }
}
