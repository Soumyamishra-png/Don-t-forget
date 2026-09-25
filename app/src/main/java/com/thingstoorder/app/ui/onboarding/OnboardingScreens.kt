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
import com.thingstoorder.app.R
import com.thingstoorder.app.data.QcApp
import com.thingstoorder.app.data.ReminderMode
import com.thingstoorder.app.permissions.Permissions
import com.thingstoorder.app.permissions.rememberPermissionState
import com.thingstoorder.app.ui.components.Chip
import com.thingstoorder.app.ui.components.DashedDivider
import com.thingstoorder.app.ui.components.PrivacyCard
import com.thingstoorder.app.ui.components.ProgressDots
import com.thingstoorder.app.ui.components.StepFooter
import com.thingstoorder.app.ui.components.Txt
import com.thingstoorder.app.ui.components.tap
import com.thingstoorder.app.ui.theme.TtoColor
import com.thingstoorder.app.ui.theme.TtoRadius
import com.thingstoorder.app.ui.theme.TtoType

private val body14 = TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal)
private val sub14 = TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp)

@Composable
private fun StepColumn(dots: Int?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)) {
        if (dots != null) ProgressDots(active = dots)
        content()
    }
}

@Composable
private fun Headline(text: String) = Txt(text, TtoType.Headline, Modifier.padding(top = 22.dp))

@Composable
private fun SubText(text: String) = Txt(text, sub14, Modifier.padding(top = 8.dp), color = TtoColor.GrayBody)

@Composable
private fun Rule() = Box(Modifier.fillMaxWidth().height(1.dp).background(TtoColor.Row))

@Composable
private fun MinusMark(onClick: (() -> Unit)? = null) {
    Box(
        Modifier.size(32.dp).then(if (onClick != null) Modifier.tap(onClick = onClick) else Modifier),
        contentAlignment = Alignment.CenterEnd,
    ) { Box(Modifier.width(10.dp).height(1.6.dp).background(TtoColor.Ink)) }
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
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(TtoColor.Paper)
                .border(1.5.dp, TtoColor.Ink, RoundedCornerShape(20.dp)).padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Txt("Things to Order", TtoType.Headline.copy(fontSize = 17.sp, lineHeight = 20.sp))
            Spacer(Modifier.height(6.dp))
            rows.forEach {
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(painterResource(R.drawable.icon_checkbox_empty), null, Modifier.size(16.dp))
                    Txt(it, TtoType.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.Normal), Modifier.weight(1f))
                    Box(Modifier.width(10.dp).height(1.6.dp).background(TtoColor.Ink))
                }
            }
        }
        Image(
            painterResource(R.drawable.icon_arrow_doodle), null,
            Modifier.padding(start = 72.dp, top = 10.dp, bottom = 10.dp).height(54.dp).graphicsLayer { scaleX = -1f },
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
        modifier.shadow(10.dp, RoundedCornerShape(20.dp)).background(TtoColor.Paper, RoundedCornerShape(20.dp)).padding(16.dp),
    ) {
        Txt("tap to add", TtoType.Headline.copy(fontSize = 16.sp, lineHeight = 19.sp))
        FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            chips.forEach { Chip(it) }
        }
    }
}

// 03
@Composable
fun AddItemsScreen(vm: OnboardingViewModel) = StepColumn(dots = 3) {
    var text by rememberSaveable { mutableStateOf("") }
    val submit = { if (vm.addItem(text)) text = "" }

    Headline("What's running low?")
    Row(
        Modifier.padding(top = 22.dp).fillMaxWidth().border(1.5.dp, TtoColor.Ink, TtoRadius.md).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (text.isEmpty()) Txt("Type an item…", TtoType.Body.copy(fontWeight = FontWeight.Normal), color = TtoColor.Faint)
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TtoType.Body.copy(fontWeight = FontWeight.Normal, color = TtoColor.Ink),
                cursorBrush = SolidColor(TtoColor.Orange),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Image(painterResource(R.drawable.icon_plus), null, Modifier.height(14.dp).tap(onClick = { submit() }))
    }

    if (vm.draftItems.isEmpty()) {
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(painterResource(R.drawable.icon_arrow_doodle), null, Modifier.height(40.dp).rotate(150f))
            Spacer(Modifier.height(10.dp))
            Txt(
                "Your list shows up here. You can add more later.",
                TtoType.BodySmall, Modifier.widthIn(max = 200.dp), color = TtoColor.GraySecondary, align = TextAlign.Center,
            )
        }
    } else {
        LazyColumn(Modifier.weight(1f).padding(top = 18.dp)) {
            itemsIndexed(vm.draftItems) { index, name ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(painterResource(R.drawable.icon_checkbox_empty), null, Modifier.size(16.dp))
                    Txt(name, TtoType.Body.copy(fontWeight = FontWeight.Normal), Modifier.weight(1f))
                    MinusMark { vm.removeItem(index) }
                }
                Rule()
            }
        }
    }
    StepFooter("Save list", primaryEnabled = vm.draftItems.isNotEmpty(), onPrimary = vm::saveList, onNotNow = vm::skipList)
}

// 04
@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(TtoRadius.md)
            .border(1.5.dp, if (selected) TtoColor.Ink else TtoColor.Line, TtoRadius.md)
            .tap(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(label, TtoType.Body.copy(fontWeight = FontWeight.SemiBold), Modifier.weight(1f))
        Box(Modifier.size(16.dp).border(1.5.dp, if (selected) TtoColor.Ink else TtoColor.Line, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(8.dp).clip(CircleShape).background(TtoColor.Ink))
        }
    }
}

@Composable
fun ReminderScreen(vm: OnboardingViewModel) = StepColumn(dots = 4) {
    Headline("How often should we remind you?")
    Column(Modifier.padding(top = 26.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RadioRow("Once a day", vm.reminder == ReminderMode.Once) { vm.selectReminder(ReminderMode.Once) }
        RadioRow("Twice a day", vm.reminder == ReminderMode.Twice) { vm.selectReminder(ReminderMode.Twice) }
        RadioRow("Custom", vm.reminder == ReminderMode.Custom) { vm.selectReminder(ReminderMode.Custom) }
    }
    if (vm.reminder == ReminderMode.Custom) {
        Txt("EVERY", TtoType.Caption, Modifier.padding(top = 16.dp), color = TtoColor.GraySecondary)
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2, 4, 6, 8).forEach { h ->
                val selected = vm.reminderHours == h
                Box(
                    Modifier.clip(TtoRadius.pill)
                        .background(if (selected) TtoColor.Ink else Color.Transparent)
                        .border(1.5.dp, TtoColor.Ink, TtoRadius.pill)
                        .tap { vm.reminderHours = h }.padding(horizontal = 16.dp, vertical = 7.dp),
                ) { Txt("${h}h", TtoType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = if (selected) Color.White else TtoColor.Ink) }
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
                Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                    Image(painterResource(circles[i]), null, Modifier.size(32.dp))
                    Txt("${i + 1}", TtoType.Headline.copy(fontSize = 14.sp, lineHeight = 16.sp))
                }
                Txt(text, body14)
            }
        }
    }
    Column(
        Modifier.weight(1f).fillMaxWidth().padding(top = 18.dp, bottom = 14.dp)
            .clip(RoundedCornerShape(18.dp)).background(TtoColor.OffWhite).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.width(36.dp).height(4.dp).clip(TtoRadius.pill).background(TtoColor.Faint).align(Alignment.CenterHorizontally))
        Txt("Widgets", TtoType.Caption.copy(fontSize = 12.sp), color = TtoColor.GraySecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(12.dp)).background(TtoColor.Row))
            Box(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(12.dp)).background(TtoColor.Row))
        }
        Column(Modifier.fillMaxWidth().clip(TtoRadius.md).background(TtoColor.Paper).border(1.5.dp, TtoColor.Ink, TtoRadius.md).padding(horizontal = 16.dp, vertical = 14.dp)) {
            Txt("Things to Order", TtoType.Headline.copy(fontSize = 15.sp, lineHeight = 18.sp))
            listOf("Oat milk", "Bananas").forEach {
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(painterResource(R.drawable.icon_checkbox_empty), null, Modifier.size(14.dp))
                    Txt(it, TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal))
                }
            }
        }
        Txt("4×2 · drag to place", TtoType.Body.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal), Modifier.align(Alignment.CenterHorizontally), color = TtoColor.GrayBody)
    }
    StepFooter(
        "Add widget",
        onPrimary = { vm.widgetRequested = true; vm.next() },
        onNotNow = { vm.widgetRequested = false; vm.next() },
    )
}

// 06
@Composable
fun NotificationsScreen(vm: OnboardingViewModel) = StepColumn(dots = 6) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.next() }
    Headline("Can we nudge you?")
    SubText("Only when something's still on your list.")
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
        Image(painterResource(R.drawable.icon_bell), null, Modifier.height(25.dp).align(Alignment.CenterHorizontally))
        Row(
            Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(16.dp)).background(TtoColor.Paper, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(28.dp).border(1.5.dp, TtoColor.Faint, RoundedCornerShape(8.dp)))
            Column(Modifier.weight(1f)) {
                Row {
                    Txt("Don't Forget", TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), Modifier.weight(1f))
                    Txt("now", TtoType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal), color = TtoColor.GraySecondary)
                }
                Txt("4 things still on your list. Order them in one go?", TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp), Modifier.padding(top = 2.dp), color = Color(0xFF3A3833))
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
                    .tap(enabled = !app.comingSoon) { vm.toggleApp(app) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).padding(4.dp), contentAlignment = Alignment.Center) {
                    Image(painterResource(app.logo), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
                Txt(app.label, TtoType.Body.copy(fontWeight = FontWeight.Normal), Modifier.weight(1f))
                if (app.comingSoon) {
                    Box(Modifier.border(1.3.dp, TtoColor.Line, TtoRadius.pill).padding(horizontal = 9.dp, vertical = 3.dp)) {
                        Txt("Coming soon", TtoType.Caption.copy(fontSize = 11.sp, letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified), color = TtoColor.GraySecondary)
                    }
                } else {
                    Image(
                        painterResource(if (app in vm.apps) R.drawable.icon_checkbox_ticked else R.drawable.icon_checkbox_empty),
                        null, Modifier.size(16.dp),
                    )
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
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
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
        Modifier.fillMaxWidth().clip(TtoRadius.md).background(TtoColor.Paper).border(1.5.dp, TtoColor.Ink, TtoRadius.md).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(painterResource(R.drawable.logo_orange), null, Modifier.size(28.dp).padding(3.dp))
        Txt("Don't Forget", TtoType.Body.copy(fontWeight = FontWeight.SemiBold), Modifier.weight(1f))
        Box(Modifier.width(40.dp).height(24.dp).clip(TtoRadius.pill).background(TtoColor.Line)) {
            Box(Modifier.padding(3.dp).size(18.dp).clip(CircleShape).background(Color.White))
        }
    }
}

@Composable
private fun ColumnScope.GrantedBlock(title: String, body: String) {
    Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
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
        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 18.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF7FBFC9))) {
            Image(painterResource(R.drawable.screenshot_blinkit_home), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
            PopupCard(listOf("Oat milk", "Bananas", "Coffee beans"), Modifier.align(Alignment.BottomCenter).padding(10.dp).fillMaxWidth())
        }
        Txt(
            "Never blocks anything underneath. Minimize it any time.",
            TtoType.Body.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal),
            Modifier.fillMaxWidth().padding(top = 10.dp), color = TtoColor.GrayBody, align = TextAlign.Center,
        )
        Txt(
            "On some phones this opens a long list. Find Don't Forget and switch it on.",
            TtoType.Body.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal),
            Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp), color = TtoColor.GraySecondary, align = TextAlign.Center,
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
        "Widget" to if (vm.widgetRequested) "Added" else "Not added",
        "Notifications" to if (notifications) "On" else "Off",
        "Apps" to if (vm.apps.isEmpty()) "None" else vm.apps.joinToString { it.label },
        "Pop-up" to if (vm.apps.isNotEmpty() && usage && overlay) "On" else "Off",
    )
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
        Image(painterResource(R.drawable.mark_confetti), null, Modifier.height(44.dp).align(Alignment.Start))
        Txt("You're all set.", TtoType.Display, Modifier.padding(top = 16.dp))
        Column(Modifier.padding(top = 22.dp)) {
            rows.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Txt(k, body14, color = TtoColor.GrayBody)
                    Txt(v, body14.copy(fontWeight = FontWeight.SemiBold), Modifier.weight(1f), align = TextAlign.End)
                }
                DashedDivider()
            }
        }
    }
    StepFooter("Go to home screen", onPrimary = { vm.finish(onFinished) })
}
