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

fun defaultDailyRevenue(): List<RevenueDataPoint> = listOf(
    RevenueDataPoint("Ven", "21 Août", 18000L, 7, 185),
    RevenueDataPoint("Sam", "22 Août", 42000L, 16, 310),
    RevenueDataPoint("Dim", "23 Août", 35000L, 12, 280),
    RevenueDataPoint("Lun", "24 Août", 12000L, 4, 140),
    RevenueDataPoint("Mar", "25 Août", 15000L, 5, 165),
    RevenueDataPoint("Mer", "26 Août", 29000L, 9, 230),
    RevenueDataPoint("Jeu", "27 Août", 38000L, 11, 295)
)

fun defaultWeeklyRevenue(): List<RevenueDataPoint> = listOf(
    RevenueDataPoint("Sem 31", "1-7 Août", 95000L, 34, 450),
    RevenueDataPoint("Sem 32", "8-14 Août", 120000L, 42, 580),
    RevenueDataPoint("Sem 33", "15-21 Août", 145000L, 49, 620),
    RevenueDataPoint("Sem 34", "22-27 Août", 189000L, 64, 780)
)

fun defaultMonthlyRevenue(): List<RevenueDataPoint> = listOf(
    RevenueDataPoint("Mars", "Mars 2026", 240000L, 92, 1200),
    RevenueDataPoint("Avr", "Avril 2026", 310000L, 115, 1450),
    RevenueDataPoint("Mai", "Mai 2026", 390000L, 142, 1800),
    RevenueDataPoint("Juin", "Juin 2026", 450000L, 165, 2100),
    RevenueDataPoint("Juil", "Juillet 2026", 520000L, 190, 2400),
    RevenueDataPoint("Août", "Août 2026", 640000L, 235, 2950)
)

fun defaultTopDonors(): List<TopDonor> = listOf(
    TopDonor("Christian Loubassou", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150", 65000L, BadgeType.VIP, 4),
    TopDonor("Sarah Okemba", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", 40000L, BadgeType.TOP_DONOR, 6),
    TopDonor("Arnold Makaya", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150", 28000L, BadgeType.TOP_DONOR, 3),
    TopDonor("Destin Moungali", "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=150", 15000L, BadgeType.CERTIFIED_CREATOR, 2)
)

fun defaultGiftBreakdown(): List<GiftCategoryBreakdown> = listOf(
    GiftCategoryBreakdown("g_crown", "Couronne royale", "👑", 3, 75000L, 40.5f),
    GiftCategoryBreakdown("g_gold_bar", "Lingot d'or pur", "🪙", 4, 40000L, 21.6f),
    GiftCategoryBreakdown("g_diamond", "Diamant étincelant", "💎", 7, 35000L, 18.9f),
    GiftCategoryBreakdown("g_gold_ring", "Bague en or", "💍", 8, 24000L, 13.0f),
    GiftCategoryBreakdown("g_bronze", "Médaille de bronze", "🥉", 11, 11000L, 6.0f)
)

fun defaultWithdrawalTransactions(): List<WithdrawalTransaction> = listOf(
    WithdrawalTransaction(
        id = "w_1",
        amountFcfa = 25000L,
        provider = "MTN Mobile Money",
        destinationAccount = "+242 06 400 12 34",
        timestamp = "Hier à 14:30",
        status = WithdrawalStatus.COMPLETED,
        referenceCode = "RET-892341"
    ),
    WithdrawalTransaction(
        id = "w_2",
        amountFcfa = 15000L,
        provider = "Airtel Money",
        destinationAccount = "+242 05 555 88 99",
        timestamp = "25 Août à 09:15",
        status = WithdrawalStatus.PENDING,
        referenceCode = "RET-481902"
    ),
    WithdrawalTransaction(
        id = "w_3",
        amountFcfa = 30000L,
        provider = "MBoté Pay (Solde)",
        destinationAccount = "@marcloutala",
        timestamp = "20 Août à 18:00",
        status = WithdrawalStatus.COMPLETED,
        referenceCode = "RET-109382"
    )
)
