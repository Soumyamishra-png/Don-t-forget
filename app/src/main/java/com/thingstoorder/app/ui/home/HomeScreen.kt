package com.thingstoorder.app.ui.home

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thingstoorder.app.R
import com.thingstoorder.app.data.Item
import com.thingstoorder.app.data.StaleSort
import com.thingstoorder.app.data.StaleTier
import com.thingstoorder.app.permissions.Permissions
import com.thingstoorder.app.permissions.rememberPermissionState
import com.thingstoorder.app.ui.components.AddField
import com.thingstoorder.app.ui.components.DashedDivider
import com.thingstoorder.app.ui.components.MinusButton
import com.thingstoorder.app.ui.components.RoundCheck
import com.thingstoorder.app.ui.components.TagPill
import com.thingstoorder.app.ui.components.Txt
import com.thingstoorder.app.ui.components.tap
import com.thingstoorder.app.ui.shared.MemberColors
import com.thingstoorder.app.ui.shared.SharePill
import com.thingstoorder.app.ui.shared.ShareSheet
import com.thingstoorder.app.ui.theme.TtoColor
import com.thingstoorder.app.ui.theme.TtoRadius
import com.thingstoorder.app.ui.theme.TtoType
import com.thingstoorder.app.voice.MicButton
import com.thingstoorder.app.voice.VoiceSheet
import com.thingstoorder.app.voice.rememberVoiceController
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(onOpenSettings: () -> Unit, vm: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val items by vm.items.collectAsStateWithLifecycle()
    val apps by vm.watchedApps.collectAsStateWithLifecycle()
    val shared by vm.shared.collectAsStateWithLifecycle()
    val bannerSeen by vm.bannerSeen.collectAsStateWithLifecycle()
    var shareOpen by rememberSaveable { mutableStateOf(false) }
    val colorOf = remember(shared) { shared?.members?.associate { it.uid to it.color } ?: emptyMap() }
    val nameOf = remember(shared) { shared?.members?.associate { it.uid to it.name } ?: emptyMap() }
    val order = shared?.lastOrder?.takeIf { it.at > bannerSeen && it.byUid != vm.myUid }
    val usage by rememberPermissionState(Permissions::usageAccessGranted)
    val overlay by rememberPermissionState(Permissions::overlayGranted)

    var text by rememberSaveable { mutableStateOf("") }
    var typing by rememberSaveable { mutableStateOf(false) }
    var voiceNote by remember { mutableStateOf<String?>(null) }
    val voice = rememberVoiceController()
    LaunchedEffect(voiceNote) { if (voiceNote != null) { delay(3500); voiceNote = null } }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var keepCardId by rememberSaveable { mutableStateOf<Long?>(null) }
    var keepAnchor by remember { mutableStateOf<Rect?>(null) }
    val editing = items.firstOrNull { it.id == editingId }
    val keepCardItem = items.firstOrNull { it.id == keepCardId }

    BackHandler(enabled = editing != null || keepCardItem != null || shareOpen) {
        editingId = null
        keepCardId = null
        shareOpen = false
    }

    val now = remember(items) { System.currentTimeMillis() }
    val arranged = remember(items, now) { StaleSort.arrange(items, now) }
    val remaining = arranged.filter { !it.checked }
    val done = arranged.filter { it.checked }
    val setupBroken = apps.isNotEmpty() && (!usage || !overlay)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp)) {
            Row(Modifier.height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                SharePill(onClick = { shareOpen = true }, members = shared?.members?.map { it.color }.orEmpty())
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(44.dp).tap(onClick = onOpenSettings), contentAlignment = Alignment.CenterEnd) {
                    Image(painterResource(R.drawable.icon_settings), "Settings", Modifier.size(24.dp))
                }
            }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Bottom) {
                Txt("Don't Forget", TtoType.Headline, Modifier.weight(1f))
                if (remaining.isNotEmpty()) Txt("${remaining.size}", TtoType.BodySmall.copy(fontWeight = FontWeight.Medium), Modifier.padding(bottom = 4.dp), color = TtoColor.InkMuted)
            }

            if (setupBroken) {
                Row(
                    Modifier.padding(top = 16.dp).fillMaxWidth().clip(TtoRadius.md).background(TtoColor.CoralSoft).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Txt("Pop-up is off.", TtoType.Body.copy(fontWeight = FontWeight.Bold), color = TtoColor.CoralDeep)
                        Txt(
                            if (!usage) "Usage Access is missing, so your list can't show up in your shopping apps."
                            else "Draw over other apps is off, so your list can't sit on top of your shopping apps.",
                            TtoType.BodySmall,
                            Modifier.padding(top = 2.dp),
                        )
                    }
                    Box(
                        Modifier.clip(TtoRadius.pill).background(TtoColor.Ink)
                            .tap { if (!usage) Permissions.openUsageAccessSettings(context) else Permissions.openOverlaySettings(context) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) { Txt("Fix it", TtoType.Chip, color = TtoColor.Paper) }
                }
            }

            if (order != null) {
                Row(
                    Modifier.padding(top = 16.dp).fillMaxWidth().clip(TtoRadius.md).background(TtoColor.MintSoft)
                        .tap { vm.dismissBanner(order.at) }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(TtoColor.Person3))
                    Txt(
                        "${order.byName.ifBlank { "Someone" }} ordered ${order.count} ${if (order.count == 1) "item" else "items"} · cleared",
                        TtoType.BodySmall.copy(fontWeight = FontWeight.Medium), Modifier.padding(start = 12.dp),
                    )
                }
            }

            Column(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    items.isEmpty() -> EmptyState()
                    remaining.isEmpty() -> AllSortedState(onClearDone = vm::clearDone)
                    else -> LazyColumn(Modifier.fillMaxSize().padding(top = 16.dp)) {
                        items(remaining, key = { it.id }) {
                            ItemRow(
                                it, vm, dot = colorOf[it.addedBy], tier = StaleSort.tierOf(it, now), onEdit = { editingId = it.id },
                                onTagTap = { bounds -> keepAnchor = bounds; keepCardId = it.id },
                            )
                        }
                        if (done.isNotEmpty()) {
                            item {
                                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Txt("DONE · ${done.size}", TtoType.Caption, Modifier.weight(1f), color = TtoColor.InkMuted)
                                    Txt(
                                        "Clear done",
                                        TtoType.Button.copy(fontSize = 14.sp),
                                        Modifier.tap(onClick = vm::clearDone).padding(vertical = 12.dp), color = TtoColor.GreenDeep,
                                    )
                                }
                            }
                            items(done, key = { it.id }) { ItemRow(it, vm, dot = colorOf[it.addedBy], tier = null, onEdit = { editingId = it.id }, onTagTap = {}) }
                        }
                    }
                }
            }

            if (voiceNote != null) {
                Txt(voiceNote!!, TtoType.BodySmall, Modifier.fillMaxWidth().padding(top = 8.dp), color = TtoColor.InkMuted, align = TextAlign.Center)
            }
            if (typing || text.isNotEmpty()) {
                AddField(
                    value = text,
                    onValueChange = { text = it },
                    onSubmit = { if (vm.add(text)) text = "" },
                    placeholder = "Type an item…",
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    onMic = voice.begin,
                )
            } else {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    MicButton(onClick = voice.begin, size = 80)
                    Box(Modifier.height(44.dp).tap { typing = true }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        Txt("or type instead", TtoType.Button.copy(fontSize = 14.sp), color = TtoColor.InkMuted)
                    }
                }
            }

            DashedDivider(color = TtoColor.Line)
            Txt(
                when {
                    items.none { !it.checked } -> "Pop-up stays hidden while the list is empty"
                    apps.isEmpty() -> "No shopping apps connected. Pick some in Settings."
                    setupBroken -> "Pop-up is off until setup is fixed"
                    else -> "Pop-up on for ${apps.joinToString { it.label }}"
                },
                TtoType.BodySmall.copy(fontSize = 12.sp),
                Modifier.padding(top = 12.dp),
                color = TtoColor.InkMuted,
            )
        }

        if (shareOpen) ShareSheet(onDismiss = { shareOpen = false })

        VoiceSheet(voice, onTypeInstead = { typing = true }, onAdd = { parsed ->
            vm.addVoice(parsed) { skipped ->
                voiceNote = when (skipped.size) {
                    0 -> null
                    1 -> "${skipped[0]} is already there"
                    else -> "${skipped.joinToString()} are already there"
                }
            }
        })

        if (editing != null) {
            EditSheet(
                item = editing,
                onDismiss = { editingId = null },
                onSave = { name, note -> vm.save(editing, name, note); editingId = null },
                onDelete = { vm.remove(editing); editingId = null },
            )
        }

        if (keepCardItem != null) {
            KeepCard(
                item = keepCardItem,
                tier = StaleSort.tierOf(keepCardItem, now) ?: StaleTier.Day,
                addedBy = nameOf[keepCardItem.addedBy],
                anchor = keepAnchor,
                onKeep = { vm.keepFresh(keepCardItem); keepCardId = null },
                onDismiss = { keepCardId = null },
            )
        }
    }
}

@Composable
private fun ItemRow(item: Item, vm: HomeViewModel, dot: Int?, tier: StaleTier?, onEdit: () -> Unit, onTagTap: (Rect) -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).tap { vm.toggle(item) }.semantics {
                contentDescription = if (item.checked) "Mark not done: ${item.name}" else "Mark done: ${item.name}"
                role = Role.Checkbox
            },
            contentAlignment = Alignment.CenterStart,
        ) { RoundCheck(item.checked, 22.dp) }
        Row(Modifier.weight(1f).height(44.dp).tap(onClick = onEdit), verticalAlignment = Alignment.CenterVertically) {
            if (dot != null) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(MemberColors[dot.coerceIn(0, 3)]))
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f, fill = false)) {
                Txt(
                    item.name,
                    TtoType.BodyLarge.copy(textDecoration = if (item.checked) TextDecoration.LineThrough else null),
                    color = if (item.checked) TtoColor.InkMuted else TtoColor.Ink,
                )
                if (!item.note.isNullOrBlank()) {
                    Txt(item.note, TtoType.BodySmall.copy(fontSize = 13.sp), color = TtoColor.InkMuted)
                }
            }
            if (item.quantity != null) {
                Spacer(Modifier.width(8.dp))
                TagPill(item.quantity, TtoColor.TealSoft, if (item.checked) TtoColor.InkMuted else TtoColor.Ink)
            }
            if (tier != null) {
                Spacer(Modifier.width(8.dp))
                StaleTag(tier, onClick = onTagTap)
            }
        }
        if (!item.checked) MinusButton({ vm.remove(item) }, "Remove ${item.name}")
    }
}

@Composable
private fun StaleTag(tier: StaleTier, onClick: (Rect) -> Unit) {
    val (bg, text) = when (tier) {
        StaleTier.Day -> TtoColor.StaleDaySoft to TtoColor.StaleDayText
        StaleTier.ThreeDays -> TtoColor.StaleThreeSoft to TtoColor.StaleThreeText
        StaleTier.Week -> TtoColor.StaleWeekSoft to TtoColor.StaleWeekText
    }
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(
        Modifier.onGloballyPositioned { coords = it }
            .tap { coords?.let { onClick(it.boundsInRoot()) } }
            .semantics { contentDescription = "Been on the list for ${tier.label}. Tap to keep it." },
    ) { TagPill(tier.label, bg, text) }
}

/** Small popover under a stale tag: names the item and its age, who added it, and one Keep. */
@Composable
private fun KeepCard(item: Item, tier: StaleTier, addedBy: String?, anchor: Rect?, onKeep: () -> Unit, onDismiss: () -> Unit) {
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize().tap(onClick = onDismiss)) {
        val width = 240.dp
        val widthPx = with(density) { width.roundToPx() }
        val offset = if (anchor != null) {
            val x = (anchor.left.toInt()).coerceAtMost(with(density) { (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp - 24.dp).roundToPx() } - widthPx).coerceAtLeast(with(density) { 24.dp.roundToPx() })
            IntOffset(x, anchor.bottom.toInt() + with(density) { 8.dp.roundToPx() })
        } else IntOffset(with(density) { 24.dp.roundToPx() }, with(density) { 200.dp.roundToPx() })
        Column(
            Modifier.offset { offset }.width(width)
                .shadow(10.dp, TtoRadius.lg, ambientColor = TtoColor.Ink.copy(alpha = 0.16f), spotColor = TtoColor.Ink.copy(alpha = 0.16f))
                .clip(TtoRadius.lg).background(TtoColor.Paper).tap { }.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Txt("${item.name} · ${tier.label}", TtoType.SectionTitle)
            Txt(
                if (addedBy.isNullOrBlank()) "Still need it?" else "Added by $addedBy. Still need it?",
                TtoType.BodySmall, color = TtoColor.InkMuted,
            )
            Box(
                Modifier.padding(top = 12.dp).fillMaxWidth().height(44.dp).clip(TtoRadius.md).background(TtoColor.Ink).tap(onClick = onKeep),
                contentAlignment = Alignment.Center,
            ) { Txt("Keep", TtoType.Button, color = TtoColor.Paper) }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(R.drawable.icon_arrow_doodle), null, Modifier.height(44.dp).rotate(150f))
        Spacer(Modifier.height(12.dp))
        Txt(
            "Nothing yet. Out of milk? Write it before you forget.",
            TtoType.BodySmall,
            Modifier.padding(horizontal = 32.dp), color = TtoColor.InkMuted, align = TextAlign.Center,
        )
    }
}

@Composable
private fun AllSortedState(onClearDone: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(R.drawable.mark_confetti), null, Modifier.height(44.dp))
        Spacer(Modifier.height(8.dp))
        Txt("All sorted!", TtoType.ScreenTitle)
        Txt("Everything's in the cart.", TtoType.BodySmall, Modifier.padding(top = 4.dp), color = TtoColor.InkMuted)
        Box(Modifier.padding(top = 12.dp).height(44.dp).tap(onClick = onClearDone).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Txt("Clear done", TtoType.Button.copy(fontSize = 14.sp), color = TtoColor.GreenDeep)
        }
    }
}

@Composable
private fun EditSheet(item: Item, onDismiss: () -> Unit, onSave: (String, String) -> Unit, onDelete: () -> Unit) {
    var name by remember(item.id) { mutableStateOf(TextFieldValue(item.name, TextRange(item.name.length))) }
    var note by remember(item.id) { mutableStateOf(TextFieldValue(item.note.orEmpty(), TextRange(item.note.orEmpty().length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(item.id) { focus.requestFocus() }

    Box(Modifier.fillMaxSize().background(TtoColor.Scrim).tap(onClick = onDismiss)) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding()
                .clip(TtoRadius.sheet).background(TtoColor.Paper)
                .tap { }.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp),
        ) {
            Box(Modifier.size(width = 40.dp, height = 4.dp).clip(TtoRadius.pill).background(TtoColor.Line).align(Alignment.CenterHorizontally))
            Txt("Edit item", TtoType.SheetTitle, Modifier.padding(top = 16.dp))
            Txt("NAME", TtoType.Caption, Modifier.padding(top = 16.dp), color = TtoColor.InkMuted)
            SheetField(name, { name = it }, Modifier.padding(top = 8.dp).focusRequester(focus), ImeAction.Next)
            Txt("NOTE · OPTIONAL", TtoType.Caption, Modifier.padding(top = 16.dp), color = TtoColor.InkMuted)
            SheetField(note, { note = it }, Modifier.padding(top = 8.dp), ImeAction.Done)
            val canSave = name.text.isNotBlank()
            Box(
                Modifier.padding(top = 24.dp).fillMaxWidth().height(52.dp).clip(TtoRadius.md).background(if (canSave) TtoColor.Ink else TtoColor.Line)
                    .tap(canSave) { onSave(name.text, note.text) },
                contentAlignment = Alignment.Center,
            ) { Txt("Save", TtoType.Button, color = if (canSave) TtoColor.Paper else TtoColor.InkSubtle) }
            Box(Modifier.fillMaxWidth().height(44.dp).tap(onClick = onDelete), contentAlignment = Alignment.Center) {
                Txt("Delete", TtoType.Button.copy(fontSize = 14.sp), color = TtoColor.CoralDeep)
            }
        }
    }
}

@Composable
private fun SheetField(value: TextFieldValue, onChange: (TextFieldValue) -> Unit, modifier: Modifier, ime: ImeAction) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier.fillMaxWidth().height(56.dp).clip(TtoRadius.md).border(2.dp, if (focused) TtoColor.Ink else TtoColor.LineStrong, TtoRadius.md)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TtoType.Body.copy(color = TtoColor.Ink),
            cursorBrush = SolidColor(TtoColor.GreenDeep),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ime),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        )
    }
}
