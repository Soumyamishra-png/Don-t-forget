package com.thingstoorder.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thingstoorder.app.voice.VoiceSheet
import com.thingstoorder.app.voice.rememberVoiceController

@Composable
fun OnboardingHost(onFinished: () -> Unit, vm: OnboardingViewModel = viewModel()) {
    BackHandler(enabled = vm.canGoBack) { vm.back() }
    val voice = rememberVoiceController()
    Box(Modifier.fillMaxSize()) {
    Box(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
    when (vm.step) {
        Step.Welcome -> WelcomeScreen(vm)
        Step.HowItWorks -> HowItWorksScreen(vm)
        Step.AddItems -> AddItemsScreen(vm, voice)
        Step.Reminder -> ReminderScreen(vm)
        Step.Widget -> WidgetScreen(vm)
        Step.Notifications -> NotificationsScreen(vm)
        Step.ConnectApps -> ConnectAppsScreen(vm)
        Step.UsageAccess -> UsageAccessScreen(vm)
        Step.Overlay -> OverlayScreen(vm)
        Step.AllSet -> AllSetScreen(vm, onFinished)
    }
    }
    if (vm.step == Step.AddItems) VoiceSheet(voice, onAdd = vm::addVoice)
    }
}
