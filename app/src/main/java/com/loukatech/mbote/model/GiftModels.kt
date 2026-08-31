package com.loukatech.mbote.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class GiftItem(
    val id: String,
    val name: String,
    val emoji: String,
    val priceFcfa: Long,
    val description: String = ""
)

@Serializable
data class GiftBundle(
    val id: String,
    val title: String,
    val description: String,
    val badge: String = "",
    val priceFcfa: Long,
    val originalPriceFcfa: Long = 0L,
    val contents: List<String>, // e.g. ["3x Médaille de bronze 🥉", "1x Bague en or 💍"]
    val giftCounts: Map<String, Int>, // map giftId to quantity
    val isPopular: Boolean = false
)

@Serializable
data class GiftTransaction(
    val id: String = UUID.randomUUID().toString(),
    val giftId: String,
    val giftName: String,
    val emoji: String,
    val amountFcfa: Long,
    val isReceived: Boolean, // true if received by user, false if sent by user
    val counterpartName: String, // sender or receiver name
    val timestamp: String,
    val status: String = "Complété" // "Complété", "Encaissé", "En attente"
)

@Serializable
data class UserGiftState(
    val inventory: Map<String, Int> = emptyMap(),
    val transactions: List<GiftTransaction> = emptyList(),
    val withdrawals: List<WithdrawalTransaction> = emptyList(),
    val totalVirtualEarnedFcfa: Long = 0L,
    val storeGifts: List<GiftItem> = emptyList(),
    val storeBundles: List<GiftBundle> = emptyList(),
    val adminPlatformGiftRevenueFcfa: Long = 0L,
    val adminPlatformBadgeRevenueFcfa: Long = 0L
)

fun defaultGiftItems(): List<GiftItem> = listOf(
    GiftItem("g_bronze", "Médaille de bronze", "🥉", 1000L, "Un geste chaleureux pour encourager le créateur"),
    GiftItem("g_gold_ring", "Bague en or", "💍", 3000L, "Une attention précieuse pleine d'élégance"),
    GiftItem("g_diamond", "Diamant étincelant", "💎", 5000L, "Un cadeau éclatant qui illumine le direct"),
    GiftItem("g_gold_bar", "Lingot d'or pur", "🪙", 10000L, "Le symbole ultime de prestige et de soutien"),
    GiftItem("g_crown", "Couronne royale", "👑", 25000L, "Récompense suprême pour les lives exceptionnels")
)

fun defaultGiftBundles(): List<GiftBundle> = listOf(
    GiftBundle(
        id = "bundle_starter",
        title = "Pack Découverte",
        description = "Idéal pour commencer à réagir et soutenir vos créateurs favoris",
        badge = "Populaire",
        priceFcfa = 5000L,
        originalPriceFcfa = 6000L,
        contents = listOf("3x Médaille de bronze 🥉", "1x Bague en or 💍"),
        giftCounts = mapOf("g_bronze" to 3, "g_gold_ring" to 1),
        isPopular = true
    ),
    GiftBundle(
        id = "bundle_vip",
        title = "Pack Diamant VIP",
        description = "Une sélection prestigieuse pour marquer les esprits en direct",
        badge = "-20% Promo",
        priceFcfa = 15000L,
        originalPriceFcfa = 19000L,
        contents = listOf("2x Diamants étincelants 💎", "3x Bagues en or 💍", "2x Médailles bronze 🥉"),
        giftCounts = mapOf("g_diamond" to 2, "g_gold_ring" to 3, "g_bronze" to 2),
        isPopular = false
    ),
    GiftBundle(
        id = "bundle_prestige",
        title = "Pack Lingots Prestige",
        description = "Le pack ultime pour les plus grands fans et mécènes de la communauté",
        badge = "Exclusif",
        priceFcfa = 50000L,
        originalPriceFcfa = 65000L,
        contents = listOf("3x Lingots d'or 🪙", "3x Diamants 💎", "1x Couronne royale 👑"),
        giftCounts = mapOf("g_gold_bar" to 3, "g_diamond" to 3, "g_crown" to 1),
        isPopular = true
    )
)

