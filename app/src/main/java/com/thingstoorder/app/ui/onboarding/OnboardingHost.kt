package com.thingstoorder.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun OnboardingHost(onFinished: () -> Unit, vm: OnboardingViewModel = viewModel()) {
    BackHandler(enabled = vm.canGoBack) { vm.back() }
    Box(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
    when (vm.step) {
        Step.Welcome -> WelcomeScreen(vm)
        Step.HowItWorks -> HowItWorksScreen(vm)
        Step.AddItems -> AddItemsScreen(vm)
        Step.Reminder -> ReminderScreen(vm)
        Step.Widget -> WidgetScreen(vm)
        Step.Notifications -> NotificationsScreen(vm)
        Step.ConnectApps -> ConnectAppsScreen(vm)
        Step.UsageAccess -> UsageAccessScreen(vm)
        Step.Overlay -> OverlayScreen(vm)
        Step.AllSet -> AllSetScreen(vm, onFinished)
    }
    }
}
