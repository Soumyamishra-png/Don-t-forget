package com.thingstoorder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thingstoorder.app.data.AppDatabase
import com.thingstoorder.app.data.SettingsRepository
import com.thingstoorder.app.ui.components.Txt
import com.thingstoorder.app.ui.onboarding.OnboardingHost
import com.thingstoorder.app.ui.theme.TtoColor
import com.thingstoorder.app.ui.theme.TtoSpacing
import com.thingstoorder.app.ui.theme.TtoType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val settings = remember { SettingsRepository(context) }
            val done by settings.onboardingDone.collectAsStateWithLifecycle(initialValue = null)

            Box(Modifier.fillMaxSize().background(TtoColor.Paper).systemBarsPadding().imePadding()) {
                when (done) {
                    null -> Unit
                    false -> OnboardingHost(onFinished = {})
                    true -> HomePlaceholder()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun HomePlaceholder() {
    val dao = AppDatabase.get(LocalContext.current).itemDao()
    val items by remember { dao.observeAll() }.collectAsStateWithLifecycle(emptyList())
    Column(Modifier.fillMaxSize().padding(TtoSpacing.s24)) {
        Txt("Don't Forget", TtoType.ScreenTitle)
        Txt("${items.size} items in the list", TtoType.Body, color = TtoColor.GrayBody)
        items.forEach { Txt(it.name, TtoType.Body, Modifier.padding(top = TtoSpacing.s8)) }
    }
}
