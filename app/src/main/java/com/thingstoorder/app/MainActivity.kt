package com.thingstoorder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thingstoorder.app.data.SettingsRepository
import com.thingstoorder.app.ui.home.HomeScreen
import com.thingstoorder.app.ui.onboarding.OnboardingHost
import com.thingstoorder.app.ui.settings.HelpScreen
import com.thingstoorder.app.ui.settings.PrivacyScreen
import com.thingstoorder.app.ui.settings.SettingsScreen
import com.thingstoorder.app.ui.theme.TtoColor

private enum class Screen { Home, Settings, Help, Privacy }

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
                    true -> MainNavigation()
                }
            }
        }
    }
}

@Composable
private fun MainNavigation() {
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    BackHandler(enabled = screen != Screen.Home) {
        screen = if (screen == Screen.Settings) Screen.Home else Screen.Settings
    }
    when (screen) {
        Screen.Home -> HomeScreen(onOpenSettings = { screen = Screen.Settings })
        Screen.Settings -> SettingsScreen(
            onBack = { screen = Screen.Home },
            onHelp = { screen = Screen.Help },
            onPrivacy = { screen = Screen.Privacy },
        )
        Screen.Help -> HelpScreen(onBack = { screen = Screen.Settings }, onCheckPermissions = { screen = Screen.Settings })
        Screen.Privacy -> PrivacyScreen(onBack = { screen = Screen.Settings })
    }
}
