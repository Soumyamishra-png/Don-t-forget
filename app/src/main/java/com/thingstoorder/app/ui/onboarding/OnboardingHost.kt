package com.thingstoorder.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun OnboardingHost(onFinished: () -> Unit, vm: OnboardingViewModel = viewModel()) {
    BackHandler(enabled = vm.canGoBack) { vm.back() }
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
