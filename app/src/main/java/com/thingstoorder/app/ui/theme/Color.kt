package com.thingstoorder.app.ui.theme

import androidx.compose.ui.graphics.Color

/** Mint Line palette (things-to-order-design-system/tokens.json). Names below the divider are older names kept so existing call sites map onto the new palette. */
object TtoColor {
    val Ink = Color(0xFF14293A)
    val InkMuted = Color(0xFF4A5F6B)
    val InkSubtle = Color(0xFF5F7380)
    val Paper = Color(0xFFFFFFFF)
    val Teal = Color(0xFFA9D6DB)
    val TealSoft = Color(0xFFEAF4F5)
    val Mint = Color(0xFFBDF2D5)
    val MintSoft = Color(0xFFE3FAEE)
    val Green = Color(0xFF52B76B)
    val GreenDeep = Color(0xFF1F6B3B)
    val Coral = Color(0xFFFF6B5B)
    val CoralSoft = Color(0xFFFFDDD8)
    val CoralDeep = Color(0xFFA3271A)
    val AmberSoft = Color(0xFFFFEDB8)
    val AmberDeep = Color(0xFF7A5200)
    val Line = Color(0xFFD6E6E8)
    val LineStrong = Color(0xFF8FB8BE)
    val Scrim = Color(0x6B14293A)

    // Shared-list people, always paired with a name.
    val PersonYou = Color(0xFFFF6B5B)
    val Person2 = Color(0xFF3E8FD6)
    val Person3 = Color(0xFF2E9E57)
    val Person4 = Color(0xFF9B6FE0)

    // Stale-item age tags: 1 day, 3 to 6 days, a week or more.
    val StaleDaySoft = TealSoft
    val StaleDayText = InkMuted
    val StaleThreeSoft = AmberSoft
    val StaleThreeText = AmberDeep
    val StaleWeekSoft = CoralSoft
    val StaleWeekText = CoralDeep

    // ---- older names, mapped onto Mint Line
    val OffWhite = TealSoft
    val Row = Line
    val GrayBody = InkMuted
    val GraySecondary = InkMuted
    val Faint = InkSubtle
    val Orange = GreenDeep
    val OrangeSoft = CoralSoft
    val OrangeText = CoralDeep
}
