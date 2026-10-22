package com.thingstoorder.app.data

import android.content.Intent
import android.net.Uri

/**
 * Opens a shopping app straight on a search for an item, so a tapped chip needs no copy and paste.
 * Only apps with a search link we have tested return an intent; for the rest the caller falls back to copying the name.
 */
object SearchLinks {
    fun intentFor(app: QcApp, query: String): Intent? = when (app) {
        // Tested on a real phone: blinkit.com/s/?q= opens the app's own search results.
        QcApp.Blinkit -> Intent(Intent.ACTION_VIEW, Uri.parse("https://blinkit.com/s/?q=" + Uri.encode(query)))
            .setPackage(app.packages.first())
        // Tested on a real phone: zepto.com/search?query= opens the app's own search results.
        QcApp.Zepto -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.zepto.com/search?query=" + Uri.encode(query)))
            .setPackage(app.packages.first())
        // Tested on a real phone: the swiggy:// scheme opens Instamart search (the https link lands in Food search).
        QcApp.Instamart -> Intent(Intent.ACTION_VIEW, Uri.parse("swiggy://instamart/search?query=" + Uri.encode(query)))
            .setPackage(app.packages.first())
        else -> null
    }
}
