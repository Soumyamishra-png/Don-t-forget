package com.thingstoorder.app.data

import androidx.annotation.DrawableRes
import com.thingstoorder.app.R

enum class QcApp(val id: String, val label: String, @DrawableRes val logo: Int, val comingSoon: Boolean = false) {
    Blinkit("blinkit", "Blinkit", R.drawable.logo_blinkit),
    Zepto("zepto", "Zepto", R.drawable.logo_zepto),
    Instamart("instamart", "Instamart", R.drawable.logo_instamart),
    BigBasket("bigbasket", "BigBasket", R.drawable.logo_bigbasket),
    AmazonFresh("amazon_fresh", "Amazon Fresh", R.drawable.logo_amazon_fresh, comingSoon = true),
    FlipkartMinutes("flipkart_minutes", "Flipkart Minutes", R.drawable.logo_flipkart_minutes, comingSoon = true);

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id }
    }
}

enum class ReminderMode { Once, Twice, Custom }
