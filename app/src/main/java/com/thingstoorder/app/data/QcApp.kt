package com.thingstoorder.app.data

import androidx.annotation.DrawableRes
import com.thingstoorder.app.R

enum class QcApp(
    val id: String,
    val label: String,
    @DrawableRes val logo: Int,
    val packages: List<String> = emptyList(),
    val comingSoon: Boolean = false,
) {
    Blinkit("blinkit", "Blinkit", R.drawable.logo_blinkit, listOf("com.grofers.customerapp")),
    Zepto("zepto", "Zepto", R.drawable.logo_zepto, listOf("com.zeptoconsumerapp")),
    Instamart("instamart", "Instamart", R.drawable.logo_instamart, listOf("in.swiggy.android")),
    BigBasket("bigbasket", "BigBasket", R.drawable.logo_bigbasket, listOf("com.bigbasket.mobileapp")),
    AmazonFresh("amazon_fresh", "Amazon Fresh", R.drawable.logo_amazon_fresh, comingSoon = true),
    FlipkartMinutes("flipkart_minutes", "Flipkart Minutes", R.drawable.logo_flipkart_minutes, comingSoon = true);

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id }

        fun matching(packageName: String?, watched: Set<QcApp>): QcApp? =
            if (packageName == null) null else watched.firstOrNull { packageName in it.packages }
    }
}

enum class ReminderMode { Once, Twice, Custom }
