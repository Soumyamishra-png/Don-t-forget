@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.thingstoorder.app.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thingstoorder.app.voice.MicButton
import com.thingstoorder.app.voice.VoiceButton
import com.thingstoorder.app.voice.VoiceController
import com.thingstoorder.app.R
import com.thingstoorder.app.data.QcApp
import com.thingstoorder.app.data.ReminderMode
import com.thingstoorder.app.permissions.Permissions
import com.thingstoorder.app.permissions.rememberPermissionState
import com.thingstoorder.app.ui.components.Chip
import com.thingstoorder.app.ui.components.DashedDivider
import com.thingstoorder.app.ui.components.NotNowButton
import com.thingstoorder.app.ui.components.PrivacyCard
import com.thingstoorder.app.ui.components.AddField
import com.thingstoorder.app.ui.components.FaceMood
import com.thingstoorder.app.ui.components.FaceTile
import com.thingstoorder.app.ui.components.ProgressDots
import com.thingstoorder.app.ui.components.RoundCheck
import com.thingstoorder.app.ui.components.TagPill
import com.thingstoorder.app.ui.components.StepFooter
import com.thingstoorder.app.ui.components.Txt
import com.thingstoorder.app.ui.components.tap
import com.thingstoorder.app.ui.theme.TtoColor
import com.thingstoorder.app.ui.theme.TtoRadius
import com.thingstoorder.app.ui.theme.TtoType
import com.thingstoorder.app.widget.WidgetPinning

private val body14 = TtoType.BodySmall
private val sub14 = TtoType.BodySmall

@Composable
private fun StepColumn(dots: Int?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)) {
        if (dots != null) ProgressDots(active = dots)
        content()
    }
}

@Composable
private fun Headline(text: String) = Txt(text, TtoType.Headline, Modifier.padding(top = 24.dp))

@Composable
private fun SubText(text: String) = Txt(text, sub14, Modifier.padding(top = 8.dp), color = TtoColor.GrayBody)

@Composable
private fun Rule() = Box(Modifier.fillMaxWidth().height(1.dp).background(TtoColor.Row))

@Composable
private fun MinusMark(onClick: (() -> Unit)? = null) {
    Box(
        Modifier.size(48.dp).then(if (onClick != null) Modifier.tap(onClick = onClick) else Modifier),
        contentAlignment = Alignment.CenterEnd,
    ) { Image(painterResource(R.drawable.icon_minus), null, Modifier.size(20.dp)) }
}

// 01
@Composable
fun WelcomeScreen(vm: OnboardingViewModel) = StepColumn(dots = null) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)) {
        Image(painterResource(R.drawable.logo_orange), null, Modifier.size(width = 61.dp, height = 68.dp))
        Txt("No need to order multiple times a day.", TtoType.Display)
        Txt("Keep one list. Order once.", TtoType.Body.copy(fontWeight = FontWeight.Normal), color = TtoColor.GrayBody)
    }
    StepFooter("Get started", onPrimary = vm::next)
}

// 02
@Composable
fun HowItWorksScreen(vm: OnboardingViewModel) = StepColumn(dots = 2) {
    Headline("Write it down.\nWe'll remind you.")
    val rows = listOf("Oat milk", "Bananas", "Coffee beans")
    Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
        Txt("On your home screen", TtoType.Caption.copy(fontSize = 12.sp), color = TtoColor.GraySecondary)
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier.fillMaxWidth().clip(TtoRadius.xl).background(TtoColor.Green).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Txt("Things to Order", TtoType.ScreenTitle)
                    Txt("Ready when you are.", TtoType.BodySmall)
                }
                FaceTile(FaceMood.Eager, 44.dp)
            }
            rows.forEach {
                Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoundCheck(false, 18.dp)
                    Txt(it, TtoType.Body, Modifier.weight(1f))
                }
            }
        }
        Image(
            painterResource(R.drawable.icon_arrow_doodle), null,
            Modifier.padding(start = 72.dp, top = 8.dp, bottom = 8.dp).height(56.dp).graphicsLayer { scaleX = -1f },
        )
        Txt("When you open a shopping app", TtoType.Caption.copy(fontSize = 12.sp), Modifier.align(Alignment.End), color = TtoColor.GraySecondary)
        Spacer(Modifier.height(8.dp))
        PopupCard(rows, Modifier.fillMaxWidth())
    }
    StepFooter("Next", onPrimary = vm::next)
}

@Composable
private fun PopupCard(chips: List<String>, modifier: Modifier = Modifier) {
    Column(
        modifier.shadow(12.dp, TtoRadius.xl, ambientColor = TtoColor.Ink.copy(alpha = 0.28f), spotColor = TtoColor.Ink.copy(alpha = 0.28f))
            .background(TtoColor.Paper, TtoRadius.xl).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FaceTile(FaceMood.Eager, 44.dp)
            Txt("tap to add", TtoType.ScreenTitle.copy(fontSize = 16.sp, lineHeight = 19.sp), Modifier.padding(start = 12.dp).weight(1f))
            Image(painterResource(R.drawable.icon_chevron), null, Modifier.size(20.dp))
        }
        FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            chips.forEach { Chip(it) }
        }
    }
}

// 03
@Composable
fun AddItemsScreen(vm: OnboardingViewModel, voice: VoiceController) = StepColumn(dots = 3) {
    var text by rememberSaveable { mutableStateOf("") }
    val submit = { if (vm.addItem(text)) text = "" }
    val empty = vm.draftItems.isEmpty()

    Headline(if (empty) "What's running low?" else "Nice, got ${vm.draftItems.size}.")
    if (empty) {
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            MicButton(onClick = voice.begin, size = 120)
            Txt("Tap and say it", TtoType.BodyLarge.copy(fontWeight = FontWeight.SemiBold), Modifier.padding(top = 16.dp))
            Txt(
                "\"2 litre doodh, eggs aur bread\"",
                TtoType.BodySmall,
                Modifier.padding(top = 8.dp), color = TtoColor.GrayBody, align = TextAlign.Center,
            )
        }
    } else {
        LazyColumn(Modifier.weight(1f).padding(top = 16.dp)) {
            itemsIndexed(vm.draftItems) { index, draft ->
                Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoundCheck(false, 22.dp)
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Txt(draft.name, TtoType.BodyLarge, Modifier.weight(1f, fill = false))
                        if (draft.quantity != null) TagPill(draft.quantity)
                    }
                    MinusMark { vm.removeItem(index) }
                }
            }
        }
    }

    AddField(
        value = text,
        onValueChange = { text = it },
        onSubmit = { submit() },
        placeholder = if (empty) "or type an item…" else "Type an item…",
        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
        onMic = if (empty) null else voice.begin,
    )

    if (!empty) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoiceButton("Say more", filled = false, weight = 1f, onClick = voice.begin, icon = true)
            VoiceButton("Next", filled = true, weight = 2f, onClick = vm::saveList)
        }
    }
    NotNowButton(onClick = vm::skipList)
}

// 04
@Composable
internal fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(TtoRadius.md)
            .border(2.dp, if (selected) TtoColor.Ink else TtoColor.Line, TtoRadius.md)
            .tap(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(label, TtoType.Body.copy(fontWeight = FontWeight.SemiBold), Modifier.weight(1f))
        Box(Modifier.size(22.dp).border(2.dp, if (selected) TtoColor.Ink else TtoColor.LineStrong, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(TtoColor.Ink))
        }
    }
}

@Composable
fun ReminderScreen(vm: OnboardingViewModel) = StepColumn(dots = 4) {
    Headline("How often should we remind you?")
    Column(Modifier.padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RadioRow("Once a day", vm.reminder == ReminderMode.Once) { vm.selectReminder(ReminderMode.Once) }
        RadioRow("Twice a day", vm.reminder == ReminderMode.Twice) { vm.selectReminder(ReminderMode.Twice) }
        RadioRow("Custom", vm.reminder == ReminderMode.Custom) { vm.selectReminder(ReminderMode.Custom) }
    }
    if (vm.reminder == ReminderMode.Custom) {
        Txt("EVERY", TtoType.Caption, Modifier.padding(top = 16.dp), color = TtoColor.InkMuted)
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2, 4, 6, 8).forEach { h ->
                val selected = vm.reminderHours == h
                Box(
                    Modifier.weight(1f).height(44.dp).clip(TtoRadius.pill)
                        .background(if (selected) TtoColor.Ink else TtoColor.Paper)
                        .border(2.dp, TtoColor.Ink, TtoRadius.pill)
                        .tap { vm.reminderHours = h },
                    contentAlignment = Alignment.Center,
                ) { Txt("${h}h", TtoType.Chip, color = if (selected) TtoColor.Paper else TtoColor.Ink) }
            }
        }
    }
    Spacer(Modifier.weight(1f))
    StepFooter("Set reminder", primaryEnabled = vm.reminder != null, onPrimary = vm::setReminder, onNotNow = vm::skipReminder)
}

// 05
@Composable
fun WidgetScreen(vm: OnboardingViewModel) = StepColumn(dots = 5) {
    Headline("Put it on your home screen.")
    val circles = listOf(R.drawable.mark_step_circle_1, R.drawable.mark_step_circle_2, R.drawable.mark_step_circle_3)
    val steps = listOf("Long-press your home screen", "Tap Widgets", "Drag Don't Forget onto it")
    Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        steps.forEachIndexed { i, text ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    Image(painterResource(circles[i]), null, Modifier.size(32.dp))
                    Txt("${i + 1}", TtoType.Headline.copy(fontSize = 14.sp, lineHeight = 16.sp))
                }
                Txt(text, body14)
            }
        }
    }
    Column(
        Modifier.weight(1f).fillMaxWidth().padding(top = 16.dp, bottom = 16.dp)
            .clip(TtoRadius.xl).background(TtoColor.Teal).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.width(40.dp).height(4.dp).clip(TtoRadius.pill).background(TtoColor.Paper).align(Alignment.CenterHorizontally))
        Txt("Widgets", TtoType.Caption, color = TtoColor.InkMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).height(56.dp).clip(TtoRadius.md).background(TtoColor.TealSoft))
            Box(Modifier.weight(1f).height(56.dp).clip(TtoRadius.md).background(TtoColor.TealSoft))
        }
        Column(Modifier.fillMaxWidth().clip(TtoRadius.xl).background(TtoColor.Green).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt("Things to Order", TtoType.ScreenTitle.copy(fontSize = 17.sp, lineHeight = 20.sp), Modifier.weight(1f))
                FaceTile(FaceMood.Eager, 44.dp)
            }
            listOf("Oat milk", "Bananas").forEachIndexed { i, name ->
                Row(Modifier.padding(top = if (i == 0) 16.dp else 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoundCheck(false, 18.dp)
                    Txt(name, TtoType.Body)
                }
            }
        }
        Txt("4×2 · drag to place", TtoType.BodySmall.copy(fontSize = 12.sp), Modifier.align(Alignment.CenterHorizontally), color = TtoColor.InkMuted)
    }
    val context = LocalContext.current
    val installed by rememberPermissionState(WidgetPinning::isInstalled)
    StepFooter(
        if (installed) "Continue" else "Add widget",
        onPrimary = {
            when {
                installed -> vm.next()
                WidgetPinning.requestPin(context) -> vm.widgetRequested = true
                else -> vm.next()
            }
        },
        onNotNow = if (installed) null else ({ vm.widgetRequested = false; vm.next() }),
    )
}

// 06
@Composable
fun NotificationsScreen(vm: OnboardingViewModel) = StepColumn(dots = 6) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.next() }
    Headline("Can we nudge you?")
    SubText("Only when something's still on your list.")
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
        Image(painterResource(R.drawable.icon_bell), null, Modifier.height(24.dp).align(Alignment.CenterHorizontally))
        Row(
            Modifier.fillMaxWidth().shadow(10.dp, TtoRadius.xl, ambientColor = TtoColor.Ink.copy(alpha = 0.16f), spotColor = TtoColor.Ink.copy(alpha = 0.16f))
                .background(TtoColor.Paper, TtoRadius.xl).padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FaceTile(FaceMood.Eager, 44.dp)
            Column(Modifier.weight(1f)) {
                Row {
                    Txt("Don't Forget", TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), Modifier.weight(1f))
                    Txt("now", TtoType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal), color = TtoColor.GraySecondary)
                }
                Txt("4 things still on your list. Order them in one go?", TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp), Modifier.padding(top = 4.dp), color = TtoColor.InkMuted)
            }
        }
    }
    StepFooter(
        "Turn on",
        onPrimary = {
            if (Build.VERSION.SDK_INT >= 33 && !Permissions.notificationsGranted(context)) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else vm.next()
        },
        onNotNow = vm::next,
    )
}

// 07
@Composable
fun ConnectAppsScreen(vm: OnboardingViewModel) = StepColumn(dots = 7) {
    Headline("Where do you shop?")
    SubText("Your list pops up when you open these.")
    Column(Modifier.weight(1f).padding(top = 16.dp)) {
        QcApp.entries.forEach { app ->
            Row(
                Modifier.fillMaxWidth().alpha(if (app.comingSoon) 0.5f else 1f)
                    .tap(enabled = !app.comingSoon) { vm.toggleApp(app) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(44.dp).clip(TtoRadius.sm).background(TtoColor.TealSoft).padding(4.dp), contentAlignment = Alignment.Center) {
                    Image(painterResource(app.logo), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
                Txt(app.label, TtoType.Body.copy(fontWeight = FontWeight.Normal), Modifier.weight(1f))
                if (app.comingSoon) {
                    Box(Modifier.border(1.3.dp, TtoColor.Line, TtoRadius.pill).padding(horizontal = 9.dp, vertical = 3.dp)) {
                        Txt("Coming soon", TtoType.Caption.copy(fontSize = 11.sp, letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified), color = TtoColor.GraySecondary)
                    }
                } else {
                    RoundCheck(app in vm.apps, 22.dp)
                }
            }
            Rule()
        }
    }
    val n = vm.apps.size
    StepFooter(
        if (n == 0) "Connect" else if (n == 1) "Connect 1 app" else "Connect $n apps",
        primaryEnabled = n > 0,
        onPrimary = vm::connectApps,
        onNotNow = vm::skipApps,
    )
}

// 08
@Composable
fun UsageAccessScreen(vm: OnboardingViewModel) = StepColumn(dots = 8) {
    val context = LocalContext.current
    val granted by rememberPermissionState(Permissions::usageAccessGranted)
    Headline("Let us see when you open a shopping app.")
    SubText("Android calls this Usage Access.")
    if (granted) {
        GrantedBlock("Usage Access is on.", "We'll know when a shopping app you picked opens.")
        StepFooter("Continue", onPrimary = vm::next)
    } else {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
            PrivacyCard()
            Txt("In Settings, you'll switch on:", TtoType.Caption.copy(fontSize = 11.sp), color = TtoColor.GraySecondary)
            SettingsMock()
        }
        StepFooter("Open settings", onPrimary = { Permissions.openUsageAccessSettings(context) }, onNotNow = vm::next)
    }
}

@Composable
private fun SettingsMock() {
    Row(
        Modifier.fillMaxWidth().clip(TtoRadius.md).background(TtoColor.Paper).border(2.dp, TtoColor.Line, TtoRadius.md).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(painterResource(R.drawable.logo_orange), null, Modifier.size(28.dp).padding(3.dp))
        Txt("Don't Forget", TtoType.Body.copy(fontWeight = FontWeight.SemiBold), Modifier.weight(1f))
        Box(Modifier.width(48.dp).height(28.dp).clip(TtoRadius.pill).background(TtoColor.Line)) {
            Box(Modifier.padding(3.dp).size(22.dp).clip(CircleShape).background(TtoColor.Paper))
        }
    }
}

@Composable
private fun ColumnScope.GrantedBlock(title: String, body: String) {
    Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
        Image(painterResource(R.drawable.mark_confetti), null, Modifier.height(44.dp))
        Txt(title, TtoType.ScreenTitle)
        Txt(body, sub14, Modifier.widthIn(max = 240.dp), color = TtoColor.GrayBody, align = TextAlign.Center)
    }
}

// 09
@Composable
fun OverlayScreen(vm: OnboardingViewModel) = StepColumn(dots = 9) {
    val context = LocalContext.current
    val granted by rememberPermissionState(Permissions::overlayGranted)
    Headline("Let your list sit on top.")
    SubText("So it appears over the shopping app, not instead of it.")
    if (granted) {
        GrantedBlock("Pop-up is ready.", "Open Blinkit and your list will be waiting.")
        StepFooter("Continue", onPrimary = vm::next)
    } else {
        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 16.dp).clip(TtoRadius.xl).background(TtoColor.Teal)) {
            Image(painterResource(R.drawable.screenshot_blinkit_home), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
            PopupCard(listOf("Oat milk", "Bananas", "Coffee beans"), Modifier.align(Alignment.BottomCenter).padding(8.dp).fillMaxWidth())
        }
        Txt(
            "Never blocks anything underneath. Minimize it any time.",
            TtoType.Body.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal),
            Modifier.fillMaxWidth().padding(top = 8.dp), color = TtoColor.GrayBody, align = TextAlign.Center,
        )
        Txt(
            "On some phones this opens a long list. Find Don't Forget and switch it on.",
            TtoType.Body.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal),
            Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp), color = TtoColor.GraySecondary, align = TextAlign.Center,
        )
        StepFooter("Open settings", onPrimary = { Permissions.openOverlaySettings(context) }, onNotNow = vm::next)
    }
}

// 10
@Composable
fun AllSetScreen(vm: OnboardingViewModel, onFinished: () -> Unit) = StepColumn(dots = 10) {
    val usage by rememberPermissionState(Permissions::usageAccessGranted)
    val overlay by rememberPermissionState(Permissions::overlayGranted)
    val notifications by rememberPermissionState(Permissions::notificationsGranted)
    val widgetInstalled by rememberPermissionState(WidgetPinning::isInstalled)

    val reminderText = when {
        !vm.reminderSet -> "Off"
        vm.reminder == ReminderMode.Once -> "Once a day"
        vm.reminder == ReminderMode.Twice -> "Twice a day"
        else -> "Every ${vm.reminderHours} hours"
    }
    val count = if (vm.listSaved) vm.draftItems.size else 0
    val rows = listOf(
        "List" to if (count == 1) "1 thing" else "$count things",
        "Reminder" to reminderText,
        "Widget" to if (widgetInstalled) "Added" else "Not added",
        "Notifications" to if (notifications) "On" else "Off",
        "Apps" to if (vm.apps.isEmpty()) "None" else vm.apps.joinToString { it.label },
        "Pop-up" to if (vm.apps.isNotEmpty() && usage && overlay) "On" else "Off",
    )
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
        Image(painterResource(R.drawable.mark_confetti), null, Modifier.height(44.dp).align(Alignment.Start))
        Txt("You're all set.", TtoType.Display, Modifier.padding(top = 16.dp))
        Column(Modifier.padding(top = 24.dp)) {
            rows.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Txt(k, body14, color = TtoColor.GrayBody)
                    Txt(v, body14.copy(fontWeight = FontWeight.SemiBold), Modifier.weight(1f), align = TextAlign.End)
                }
                DashedDivider()
            }
        }
    }
    StepFooter("Go to home screen", onPrimary = { vm.finish(onFinished) })
}
