package com.thingstoorder.app.ui.settings

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thingstoorder.app.R
import com.thingstoorder.app.data.QcApp
import com.thingstoorder.app.data.ReminderSetting
import com.thingstoorder.app.data.SettingsRepository
import com.thingstoorder.app.overlay.OverlayController
import com.thingstoorder.app.permissions.Permissions
import com.thingstoorder.app.permissions.rememberPermissionState
import com.thingstoorder.app.ui.components.HairlineRule
import com.thingstoorder.app.ui.components.PrimaryButton
import com.thingstoorder.app.ui.components.PrivacyCard
import com.thingstoorder.app.ui.components.ToggleSwitch
import com.thingstoorder.app.ui.components.Txt
import com.thingstoorder.app.ui.components.tap
import com.thingstoorder.app.ui.theme.TtoColor
import com.thingstoorder.app.ui.theme.TtoRadius
import com.thingstoorder.app.ui.theme.TtoType
import com.thingstoorder.app.widget.WidgetPinning
import com.thingstoorder.app.data.AppDatabase
import com.thingstoorder.app.data.ReminderMode
import com.thingstoorder.app.reminders.ReminderNotifier
import com.thingstoorder.app.ui.onboarding.RadioRow
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository(app)
    val watchedApps = repo.watchedApps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val reminder = repo.reminder.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderSetting(null, 4))

    fun setWatched(app: QcApp, watched: Boolean) {
        viewModelScope.launch { repo.setWatched(app, watched) }
    }

    fun setReminder(mode: ReminderMode?, hours: Int) {
        viewModelScope.launch { repo.setReminder(mode, hours) }
    }
}

private val rowText = TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal)

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).tap(onClick = onBack), contentAlignment = Alignment.CenterStart) {
            Txt("←", TtoType.ScreenTitle)
        }
        Txt(title, TtoType.ScreenTitle, Modifier.weight(1f))
    }
}

@Composable
private fun SectionLabel(text: String, first: Boolean = false) =
    Txt(text.uppercase(), TtoType.Caption, Modifier.padding(top = if (first) 20.dp else 26.dp, bottom = 4.dp), color = TtoColor.GraySecondary)

@Composable
private fun SettingRow(
    label: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Column {
        Row(
            modifier.fillMaxWidth().then(if (onClick != null) Modifier.tap(onClick = onClick) else Modifier).padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (leading != null) leading()
            Txt(label, rowText, Modifier.weight(1f))
            trailing()
        }
        HairlineRule()
    }
}

@Composable
private fun ValueText(text: String, bold: Boolean = false, color: Color = TtoColor.GrayBody) =
    Txt(text, TtoType.Body.copy(fontSize = 13.sp, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal), color = if (bold && color == TtoColor.GrayBody) TtoColor.Ink else color)

@Composable
fun SettingsScreen(onBack: () -> Unit, onHelp: () -> Unit, onPrivacy: () -> Unit, vm: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val apps by vm.watchedApps.collectAsStateWithLifecycle()
    val reminder by vm.reminder.collectAsStateWithLifecycle()
    val usage by rememberPermissionState(Permissions::usageAccessGranted)
    val overlay by rememberPermissionState(Permissions::overlayGranted)
    val notifications by rememberPermissionState(Permissions::notificationsGranted)

    var pickerOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)) {
        ScreenHeader("Settings", onBack)

        SectionLabel("Shopping apps", first = true)
        QcApp.entries.forEach { app ->
            SettingRow(
                app.label,
                Modifier.alpha(if (app.comingSoon) 0.5f else 1f),
                leading = {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).padding(2.dp), contentAlignment = Alignment.Center) {
                        Image(painterResource(app.logo), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                },
            ) {
                if (app.comingSoon) {
                    Box(Modifier.border(1.3.dp, TtoColor.Line, TtoRadius.pill).padding(horizontal = 9.dp, vertical = 3.dp)) {
                        Txt("Coming soon", TtoType.Body.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = TtoColor.GraySecondary)
                    }
                } else {
                    ToggleSwitch(checked = app in apps, onCheckedChange = { vm.setWatched(app, it) })
                }
            }
        }

        SectionLabel("Reminders")
        SettingRow(
            "Remind me",
            leading = { Image(painterResource(R.drawable.icon_bell), null, Modifier.size(width = 14.dp, height = 16.dp)) },
            onClick = { pickerOpen = true },
        ) {
            ValueText(reminder.label, bold = true)
            Txt("›", TtoType.Body.copy(fontSize = 22.sp, fontWeight = FontWeight.Normal), color = TtoColor.GrayBody)
        }
        SettingRow("At", leading = { Image(painterResource(R.drawable.icon_clock), null, Modifier.size(14.dp)) }) { ValueText(reminder.times) }
        SettingRow(
            "Send a test reminder",
            onClick = {
                scope.launch(Dispatchers.IO) {
                    ReminderNotifier.post(context, AppDatabase.get(context).itemDao().getRemaining())
                }
            },
        ) { Txt("›", TtoType.Body.copy(fontSize = 22.sp, fontWeight = FontWeight.Normal), color = TtoColor.GrayBody) }

        SectionLabel("Pop-up")
        SettingRow("After I minimize it") { ValueText("Hide until next open", bold = true) }
        SettingRow("Show when list is empty") { ValueText("Never") }

        SectionLabel("Permissions")
        SettingRow("Usage Access", onClick = { Permissions.openUsageAccessSettings(context) }) { PermissionValue(usage) }
        SettingRow("Draw over other apps", onClick = { Permissions.openOverlaySettings(context) }) { PermissionValue(overlay) }
        SettingRow("Notifications", onClick = { Permissions.openNotificationSettings(context) }) { PermissionValue(notifications) }

        SectionLabel("Home screen")
        SettingRow("Add widget to home screen", onClick = { WidgetPinning.requestPin(context) }) {
            Txt("›", TtoType.Body.copy(fontSize = 22.sp, fontWeight = FontWeight.Normal), color = TtoColor.GrayBody)
        }

        SectionLabel("Help")
        SettingRow("Pop-up not showing?", onClick = onHelp) { Txt("›", TtoType.Body.copy(fontSize = 22.sp, fontWeight = FontWeight.Normal), color = TtoColor.GrayBody) }
        SettingRow("What we can see", onClick = onPrivacy) { Txt("›", TtoType.Body.copy(fontSize = 22.sp, fontWeight = FontWeight.Normal), color = TtoColor.GrayBody) }

        Txt("Your list stays on this phone.", TtoType.BodySmall.copy(fontSize = 12.sp), Modifier.fillMaxWidth().padding(top = 28.dp), color = TtoColor.GraySecondary, align = TextAlign.Center)
    }

    if (pickerOpen) {
        ReminderPicker(
            current = reminder,
            onSave = { mode, hours -> vm.setReminder(mode, hours); pickerOpen = false },
            onDismiss = { pickerOpen = false },
        )
    }
    }
}

@Composable
private fun ReminderPicker(current: ReminderSetting, onSave: (ReminderMode?, Int) -> Unit, onDismiss: () -> Unit) {
    var mode by remember { mutableStateOf(current.mode) }
    var hours by remember { mutableIntStateOf(current.hours) }

    Box(Modifier.fillMaxSize().background(Color(0x6B171717)).tap(onClick = onDismiss)) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)).background(TtoColor.Paper)
                .tap { }.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
        ) {
            Box(Modifier.size(width = 36.dp, height = 4.dp).clip(TtoRadius.pill).background(TtoColor.Faint).align(Alignment.CenterHorizontally))
            Txt("Remind me", TtoType.SectionTitle, Modifier.padding(top = 14.dp))
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RadioRow("Off", mode == null) { mode = null }
                RadioRow("Once a day", mode == ReminderMode.Once) { mode = ReminderMode.Once }
                RadioRow("Twice a day", mode == ReminderMode.Twice) { mode = ReminderMode.Twice }
                RadioRow("Custom", mode == ReminderMode.Custom) { mode = ReminderMode.Custom }
            }
            if (mode == ReminderMode.Custom) {
                Txt("EVERY", TtoType.Caption, Modifier.padding(top = 16.dp), color = TtoColor.GraySecondary)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 4, 6, 8).forEach { h ->
                        val selected = hours == h
                        Box(
                            Modifier.clip(TtoRadius.pill).background(if (selected) TtoColor.Ink else Color.Transparent)
                                .border(1.5.dp, TtoColor.Ink, TtoRadius.pill).tap { hours = h }.padding(horizontal = 16.dp, vertical = 7.dp),
                        ) { Txt("${h}h", TtoType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = if (selected) Color.White else TtoColor.Ink) }
                    }
                }
            }
            Box(Modifier.padding(top = 20.dp)) { PrimaryButton("Save", onClick = { onSave(mode, hours) }) }
        }
    }
}

@Composable
private fun PermissionValue(granted: Boolean) {
    if (granted) ValueText("On", bold = true) else ValueText("Off · Turn on", bold = true, color = TtoColor.Orange)
}

@Composable
fun HelpScreen(onBack: () -> Unit, onCheckPermissions: () -> Unit) {
    val context = LocalContext.current
    val circles = listOf(R.drawable.mark_step_circle_1, R.drawable.mark_step_circle_2, R.drawable.mark_step_circle_3)
    Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)) {
        ScreenHeader("Pop-up not showing?", onBack)
        Txt(
            "Some phones stop background apps to save battery. Check these three, in order.",
            TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
            Modifier.padding(top = 14.dp), color = TtoColor.GrayBody,
        )
        Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            HelpStep(circles[0], 1, "Permissions still on", "Usage Access and Draw over other apps.", "Check permissions", onCheckPermissions)
            HelpStep(circles[1], 2, "Battery: set to Unrestricted", "So Android doesn't stop the watcher when the screen is off.", "Open battery settings") { Permissions.openBatterySettings(context) }
            HelpStep(circles[2], 3, "Lock it in recent apps", "On Samsung, Xiaomi and OnePlus: open recents, tap our icon, choose Keep open or Lock.", null, null)
        }
        Spacer(Modifier.weight(1f))
        val overlayOn by rememberPermissionState(Permissions::overlayGranted)
        PrimaryButton("Send a test pop-up", enabled = overlayOn, onClick = { OverlayController.showTest() })
        Txt(
            if (overlayOn) "Shows for 5 seconds over whatever is on screen." else "Turn on Draw over other apps first.",
            TtoType.BodySmall.copy(fontSize = 12.sp), Modifier.fillMaxWidth().padding(top = 8.dp), color = TtoColor.GraySecondary, align = TextAlign.Center,
        )
    }
}

@Composable
private fun HelpStep(circle: Int, number: Int, title: String, body: String, action: String?, onAction: (() -> Unit)?) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            Image(painterResource(circle), null, Modifier.size(32.dp))
            Txt("$number", TtoType.Headline.copy(fontSize = 14.sp, lineHeight = 16.sp))
        }
        Column(Modifier.weight(1f)) {
            Txt(title, TtoType.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
            Txt(body, TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp), Modifier.padding(top = 2.dp), color = TtoColor.GrayBody)
            if (action != null && onAction != null) {
                Txt(
                    action,
                    TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline),
                    Modifier.padding(top = 6.dp).tap(onClick = onAction).padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)) {
        ScreenHeader("What we can see", onBack)
        Spacer(Modifier.height(20.dp))
        PrivacyCard()
        Txt("Your list stays on this phone.", TtoType.BodySmall.copy(fontSize = 13.sp), Modifier.padding(top = 16.dp), color = TtoColor.GrayBody)
    }
}
