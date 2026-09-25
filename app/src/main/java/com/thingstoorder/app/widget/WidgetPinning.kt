package com.thingstoorder.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

object WidgetPinning {
    private fun provider(context: Context) = ComponentName(context, DontForgetWidgetReceiver::class.java)

    fun isInstalled(context: Context): Boolean =
        AppWidgetManager.getInstance(context).getAppWidgetIds(provider(context)).isNotEmpty()

    fun requestPin(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        return manager.requestPinAppWidget(provider(context), null, null)
    }
}
