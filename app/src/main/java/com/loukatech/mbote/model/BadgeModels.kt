package com.loukatech.mbote.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class BadgeType(
    val id: String,
    val title: String,
    val shortLabel: String,
    val emoji: String,
    val colorHex: Long,
    val priceFcfa: Long,
    val description: String
) {
    VIP(
        id = "badge_vip",
        title = "Badge VIP Prestige",
        shortLabel = "VIP",
        emoji = "👑",
        colorHex = 0xFFFFD700, // Gold
        priceFcfa = 10000L,
        description = "Statut VIP exclusif affiché sur votre profil, accès prioritaire et icône dorée lors des Lives"
    ),
    TOP_DONOR(
        id = "badge_top_donor",
        title = "Top Donateur Mécène",
        shortLabel = "Top Donateur",
        emoji = "💎",
        colorHex = 0xFF00E5FF, // Cyan / Diamond Blue
        priceFcfa = 15000L,
        description = "Distingue les plus généreux bienfaiteurs avec effet néon et mise en avant dans les flux en direct"
    ),
    CERTIFIED_CREATOR(
        id = "badge_certified_creator",
        title = "Créateur Certifié MBoté",
        shortLabel = "Certifié ⭐",
        emoji = "⭐",
        colorHex = 0xFF8B5CF6, // Purple Star
        priceFcfa = 20000L,
        description = "Badge officiel de certification pour créateurs de contenus vérifiés, accès aux analytics avancés"
    )
}

@Serializable
data class UserBadge(
    val type: BadgeType,
    val acquiredAt: String,
    val isActive: Boolean = true
)

@Serializable
data class WithdrawalTransaction(
    val id: String = UUID.randomUUID().toString(),
    val amountFcfa: Long,
    val provider: String, // MTN Mobile Money, Airtel Money, MBoté Pay, Virement
    val destinationAccount: String, // e.g. +242 06 400 00 00
    val timestamp: String,
    val status: WithdrawalStatus = WithdrawalStatus.COMPLETED,
    val referenceCode: String = "RET-${(100000..999999).random()}"
)

@Serializable
enum class WithdrawalStatus(val label: String, val colorHex: Long) {
    COMPLETED("Validé & Transféré", 0xFF10B981), // Green
    PENDING("En cours de traitement", 0xFFF59E0B), // Amber
    PROCESSING("Vérification bancaire", 0xFF3B82F6), // Blue
    FAILED("Échoué / Annulé", 0xFFEF4444) // Red
}

@Serializable
data class RevenueDataPoint(
    val label: String, // "Lun", "Mar", "Sem 1", "Août"
    val fullDate: String,
    val amountFcfa: Long,
    val giftCount: Int,
    val viewerPeak: Int = 0
)

@Serializable
data class TopDonor(
    val name: String,
    val avatar: String,
    val totalGiftedFcfa: Long,
    val badgeType: BadgeType,
    val giftCount: Int
)

@Serializable
data class GiftCategoryBreakdown(
    val giftId: String,
    val giftName: String,
    val emoji: String,
    val count: Int,
    val totalRevenueFcfa: Long,
    val percentage: Float
)

@Serializable
data class CreatorAnalyticsData(
    val period: AnalyticsPeriod = AnalyticsPeriod.WEEKLY,
    val totalEarnedPeriodFcfa: Long = 148000L,
    val previousPeriodEarnedFcfa: Long = 110000L,
    val growthPercentage: Float = 34.5f,
    val totalGiftsCount: Int = 54,
    val donorConversionRate: Float = 14.8f,
    val bestGiftName: String = "Couronne royale",
    val bestGiftEmoji: String = "👑",
    val bestGiftRevenueFcfa: Long = 75000L,
    val dailyData: List<RevenueDataPoint> = defaultDailyRevenue(),
    val weeklyData: List<RevenueDataPoint> = defaultWeeklyRevenue(),
    val monthlyData: List<RevenueDataPoint> = defaultMonthlyRevenue(),
    val topDonors: List<TopDonor> = defaultTopDonors(),
    val giftBreakdown: List<GiftCategoryBreakdown> = defaultGiftBreakdown()
)

@Serializable
enum class AnalyticsPeriod(val label: String) {
    DAILY("7 Derniers Jours"),
    WEEKLY("4 Dernières Semaines"),
    MONTHLY("6 Derniers Mois")
}

fun defaultDailyRevenue(): List<RevenueDataPoint> = emptyList()

fun defaultWeeklyRevenue(): List<RevenueDataPoint> = emptyList()

fun defaultMonthlyRevenue(): List<RevenueDataPoint> = emptyList()

fun defaultTopDonors(): List<TopDonor> = emptyList()

fun defaultGiftBreakdown(): List<GiftCategoryBreakdown> = emptyList()

fun defaultWithdrawalTransactions(): List<WithdrawalTransaction> = emptyList()
