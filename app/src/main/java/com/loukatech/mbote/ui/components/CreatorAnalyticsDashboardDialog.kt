package com.loukatech.mbote.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.loukatech.mbote.model.*
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorAnalyticsDashboardDialog(
    userProfile: UserProfile,
    analyticsData: CreatorAnalyticsData = CreatorAnalyticsData(),
    onTogglePremium: (Boolean) -> Unit = {},
    onDismiss: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf(AnalyticsPeriod.WEEKLY) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val currentDataPoints = remember(selectedPeriod) {
        when (selectedPeriod) {
            AnalyticsPeriod.DAILY -> analyticsData.dailyData
            AnalyticsPeriod.WEEKLY -> analyticsData.weeklyData
            AnalyticsPeriod.MONTHLY -> analyticsData.monthlyData
        }
    }

    val totalRevenueCurrentPeriod = remember(currentDataPoints) {
        currentDataPoints.sumOf { it.amountFcfa }
    }

    val totalGiftsCurrentPeriod = remember(currentDataPoints) {
        currentDataPoints.sumOf { it.giftCount }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📊", fontSize = 22.sp)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Analytique Créateur",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (userProfile.isPremium) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFD700)
                                ) {
                                    Text(
                                        text = "PRO ⭐",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Monétisation des Lives & Cadeaux Virtuels",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                }
            }
        },
        text = {
            if (!userProfile.isPremium) {
                // Premium Gating Screen
                PremiumUnlockGateView(
                    onUnlockPremium = {
                        onTogglePremium(true)
                    }
                )
            } else {
                // Premium Analytics Dashboard
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Period Selector Tabs (Jour, Semaine, Mois)
                    item {
                        SingleChoiceSegmentedRow(
                            selectedPeriod = selectedPeriod,
                            onPeriodSelected = {
                                selectedPeriod = it
                                selectedPointIndex = null
                            }
                        )
                    }

                    // Key Metric KPI Cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCard(
                                title = "Revenus Période",
                                value = "$totalRevenueCurrentPeriod F",
                                subtitle = "+${analyticsData.growthPercentage}% vs préc.",
                                icon = Icons.Outlined.MonetizationOn,
                                accentColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "Cadeaux Reçus",
                                value = "$totalGiftsCurrentPeriod",
                                subtitle = "Taux conv. ${analyticsData.donorConversionRate}%",
                                icon = Icons.Outlined.CardGiftcard,
                                accentColor = MbotePurplePrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Interactive Revenue Chart (Bar & Line Curve Canvas)
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Évolution des Revenus (FCFA)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = selectedPeriod.label,
                                        fontSize = 11.sp,
                                        color = MbotePurplePrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Interactive Canvas Chart
                                RevenueInteractiveCanvasChart(
                                    dataPoints = currentDataPoints,
                                    selectedIndex = selectedPointIndex,
                                    onSelectIndex = { selectedPointIndex = it }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Selected Point Detail Card
                                val selectedPoint = selectedPointIndex?.let { currentDataPoints.getOrNull(it) }
                                if (selectedPoint != null) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MbotePurpleSoft,
                                        border = BorderStroke(1.dp, MbotePurplePrimary.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = selectedPoint.fullDate,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MbotePurplePrimary
                                                )
                                                Text(
                                                    text = "${selectedPoint.giftCount} cadeaux reçus • Pic : ${selectedPoint.viewerPeak} spectateurs",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "${selectedPoint.amountFcfa} FCFA",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Touchez une barre du graphique pour inspecter une date précise",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Répartition des Types de Cadeaux
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Répartition par Cadeau Virtuel",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                analyticsData.giftBreakdown.forEach { item ->
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(item.emoji, fontSize = 16.sp)
                                                Text(
                                                    text = "${item.giftName} (x${item.count})",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Text(
                                                text = "${item.totalRevenueFcfa} F (${item.percentage}%)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MbotePurplePrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { item.percentage / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = when (item.giftId) {
                                                "g_crown" -> Color(0xFFFFD700)
                                                "g_gold_bar" -> Color(0xFFFFB300)
                                                "g_diamond" -> Color(0xFF00E5FF)
                                                "g_gold_ring" -> Color(0xFFF59E0B)
                                                else -> Color(0xFFCD7F32)
                                            },
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Top Donateurs Leaderboard
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Top Mécènes & Donateurs 🏆",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${analyticsData.topDonors.size} mécènes",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                analyticsData.topDonors.forEachIndexed { index, donor ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = if (index == 0) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        AsyncImage(
                                            model = donor.avatar,
                                            contentDescription = donor.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = donor.name,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                UserBadgeChip(badge = donor.badgeType, compact = true)
                                            }
                                            Text(
                                                text = "${donor.giftCount} cadeaux envoyés en live",
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${donor.totalGiftedFcfa} F",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)
            ) {
                Text("Fermer")
            }
        }
    )
}

@Composable
private fun PremiumUnlockGateView(
    onUnlockPremium: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFD700).copy(alpha = 0.3f), Color(0xFF8B5CF6).copy(alpha = 0.2f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("👑", fontSize = 38.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Fonctionnalité MBoté Premium",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Le tableau de bord analytique détaillé (Revenus par jour/semaine/mois, graphiques de conversion, statistiques mécènes) est exclusivement réservé aux Créateurs et Abonnés Premium.",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MbotePurpleSoft),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("✨ Inclus avec MBoté Premium :", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = MbotePurplePrimary)
                Text("• Graphiques interactifs d'évolution des gains", fontSize = 11.5.sp)
                Text("• Analyse du pic de spectateurs et conversion", fontSize = 11.5.sp)
                Text("• Option d'Envoi Rapide Multi-Cadeaux en Live", fontSize = 11.5.sp)
                Text("• Badge VIP Créateur offert", fontSize = 11.5.sp)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onUnlockPremium,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(
                text = "Débloquer MBoté Premium (9 900 F / mois)",
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun SingleChoiceSegmentedRow(
    selectedPeriod: AnalyticsPeriod,
    onPeriodSelected: (AnalyticsPeriod) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AnalyticsPeriod.values().forEach { period ->
                val isSelected = selectedPeriod == period
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MbotePurplePrimary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPeriodSelected(period) }
                ) {
                    Text(
                        text = when (period) {
                            AnalyticsPeriod.DAILY -> "Jour"
                            AnalyticsPeriod.WEEKLY -> "Semaine"
                            AnalyticsPeriod.MONTHLY -> "Mois"
                        },
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.5.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RevenueInteractiveCanvasChart(
    dataPoints: List<RevenueDataPoint>,
    selectedIndex: Int?,
    onSelectIndex: (Int) -> Unit
) {
    if (dataPoints.isEmpty()) return

    val maxAmount = remember(dataPoints) {
        (dataPoints.maxOfOrNull { it.amountFcfa } ?: 1L).coerceAtLeast(1000L).toFloat()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(dataPoints) {
                    detectTapGestures { offset ->
                        val barSlotWidth = size.width / dataPoints.size
                        val tappedIndex = (offset.x / barSlotWidth).toInt().coerceIn(0, dataPoints.size - 1)
                        onSelectIndex(tappedIndex)
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val bottomPadding = 36f
            val topPadding = 20f
            val chartHeight = height - bottomPadding - topPadding
            val barSlotWidth = width / dataPoints.size
            val barWidth = barSlotWidth * 0.52f

            // Grid Lines (25%, 50%, 75%, 100%)
            val gridColor = Color.Gray.copy(alpha = 0.15f)
            for (i in 1..3) {
                val y = topPadding + chartHeight * (i / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.5f
                )
            }

            // Draw Bar Rectangles and Curve Points
            val linePoints = mutableListOf<Offset>()

            dataPoints.forEachIndexed { index, dp ->
                val fraction = (dp.amountFcfa / maxAmount).coerceIn(0.05f, 1f)
                val barH = chartHeight * fraction
                val xLeft = index * barSlotWidth + (barSlotWidth - barWidth) / 2f
                val yTop = topPadding + (chartHeight - barH)
                val isSelected = selectedIndex == index

                // Bar Gradient
                val barBrush = Brush.verticalGradient(
                    colors = if (isSelected) {
                        listOf(Color(0xFFFFD700), Color(0xFFFFA000))
                    } else {
                        listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                    },
                    startY = yTop,
                    endY = yTop + barH
                )

                // Draw Bar
                drawRoundRect(
                    brush = barBrush,
                    topLeft = Offset(xLeft, yTop),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Collect point for curve
                val centerX = xLeft + (barWidth / 2f)
                linePoints.add(Offset(centerX, yTop))
            }

            // Draw Connecting Line on top of bars
            if (linePoints.size > 1) {
                val path = Path().apply {
                    moveTo(linePoints.first().x, linePoints.first().y)
                    for (i in 1 until linePoints.size) {
                        val prev = linePoints[i - 1]
                        val curr = linePoints[i]
                        val midX = (prev.x + curr.x) / 2f
                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                }
                drawPath(
                    path = path,
                    color = Color(0xFF00E5FF).copy(alpha = 0.75f),
                    style = Stroke(width = 4f)
                )
            }
        }

        // X-Axis Labels Row below Canvas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dataPoints.forEachIndexed { index, dp ->
                val isSelected = selectedIndex == index
                Text(
                    text = dp.label,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                    color = if (isSelected) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
