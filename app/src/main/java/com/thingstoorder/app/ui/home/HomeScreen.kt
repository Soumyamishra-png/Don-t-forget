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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thingstoorder.app.R
import com.thingstoorder.app.data.Item
import com.thingstoorder.app.permissions.Permissions
import com.thingstoorder.app.permissions.rememberPermissionState
import com.thingstoorder.app.ui.components.AddField
import com.thingstoorder.app.ui.components.DashedDivider
import com.thingstoorder.app.ui.components.HairlineRule
import com.thingstoorder.app.ui.components.MinusButton
import com.thingstoorder.app.ui.components.Txt
import com.thingstoorder.app.ui.components.tap
import com.thingstoorder.app.ui.theme.TtoColor
import com.thingstoorder.app.ui.theme.TtoRadius
import com.thingstoorder.app.ui.theme.TtoType

@Composable
fun HomeScreen(onOpenSettings: () -> Unit, vm: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val items by vm.items.collectAsStateWithLifecycle()
    val apps by vm.watchedApps.collectAsStateWithLifecycle()
    val usage by rememberPermissionState(Permissions::usageAccessGranted)
    val overlay by rememberPermissionState(Permissions::overlayGranted)

    var text by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editing = items.firstOrNull { it.id == editingId }

    BackHandler(enabled = editing != null) { editingId = null }

    val remaining = items.filter { !it.checked }
    val done = items.filter { it.checked }
    val setupBroken = apps.isNotEmpty() && (!usage || !overlay)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt("Don't Forget", TtoType.ScreenTitle, Modifier.weight(1f))
                Box(Modifier.size(40.dp).tap(onClick = onOpenSettings), contentAlignment = Alignment.CenterEnd) {
                    Image(painterResource(R.drawable.icon_settings), "Settings", Modifier.size(22.dp))
                }
            }

            if (setupBroken) {
                Row(
                    Modifier.padding(top = 12.dp).fillMaxWidth().clip(TtoRadius.md).background(TtoColor.OrangeSoft).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Txt("Pop-up is off.", TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
                        Txt(
                            if (!usage) "Usage Access is missing, so your list can't show up in your shopping apps."
                            else "Draw over other apps is off, so your list can't sit on top of your shopping apps.",
                            TtoType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp),
                            Modifier.padding(top = 2.dp),
                        )
                    }
                    Box(
                        Modifier.clip(TtoRadius.pill).background(TtoColor.Ink)
                            .tap { if (!usage) Permissions.openUsageAccessSettings(context) else Permissions.openOverlaySettings(context) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) { Txt("Fix it", TtoType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = Color.White) }
                }
            }

            AddField(
                value = text,
                onValueChange = { text = it },
                onSubmit = { if (vm.add(text)) text = "" },
                placeholder = "Add something…",
                modifier = Modifier.padding(top = if (setupBroken) 14.dp else 18.dp),
            )

            Column(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    items.isEmpty() -> EmptyState()
                    remaining.isEmpty() -> AllSortedState(onClearDone = vm::clearDone)
                    else -> LazyColumn(Modifier.fillMaxSize().padding(top = 20.dp)) {
                        item {
                            Row {
                                Txt(
                                    "${remaining.size}",
                                    TtoType.Caption,
                                    color = if (setupBroken) TtoColor.GraySecondary else TtoColor.Orange,
                                )
                                Txt(" TO ORDER", TtoType.Caption, color = TtoColor.GraySecondary)
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                        items(remaining, key = { it.id }) { ItemRow(it, vm, onEdit = { editingId = it.id }) }
                        if (done.isNotEmpty()) {
                            item {
                                Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Txt("DONE · ${done.size}", TtoType.Caption, Modifier.weight(1f), color = TtoColor.GraySecondary)
                                    Txt(
                                        "Clear done",
                                        TtoType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline),
                                        Modifier.tap(onClick = vm::clearDone).padding(vertical = 6.dp),
                                    )
                                }
                            }
                            items(done, key = { it.id }) { ItemRow(it, vm, onEdit = { editingId = it.id }) }
                        }
                    }
                }
            }

            DashedDivider()
            Txt(
                when {
                    items.none { !it.checked } -> "Pop-up stays hidden while the list is empty"
                    apps.isEmpty() -> "No shopping apps connected. Pick some in Settings."
                    setupBroken -> "Pop-up is off until setup is fixed"
                    else -> "Pop-up on for ${apps.joinToString { it.label }}"
                },
                TtoType.Body.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal),
                Modifier.padding(top = 12.dp),
                color = TtoColor.GrayBody,
            )
        }

        if (editing != null) {
            EditSheet(
                item = editing,
                onDismiss = { editingId = null },
                onSave = { name, note -> vm.save(editing, name, note); editingId = null },
                onDelete = { vm.remove(editing); editingId = null },
            )
        }
    }
}

@Composable
private fun ItemRow(item: Item, vm: HomeViewModel, onEdit: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).tap { vm.toggle(item) }.semantics {
                    contentDescription = if (item.checked) "Mark not done: ${item.name}" else "Mark done: ${item.name}"
                    role = Role.Checkbox
                },
                contentAlignment = Alignment.CenterStart,
            ) {
                Image(
                    painterResource(if (item.checked) R.drawable.icon_checkbox_ticked else R.drawable.icon_checkbox_empty),
                    null, Modifier.size(16.dp),
                )
            }
            Column(Modifier.weight(1f).tap(onClick = onEdit).padding(vertical = 4.dp)) {
                Txt(
                    item.name,
                    TtoType.Body.copy(fontWeight = FontWeight.Normal, textDecoration = if (item.checked) TextDecoration.LineThrough else null),
                    color = if (item.checked) TtoColor.Faint else TtoColor.Ink,
                )
                if (!item.note.isNullOrBlank()) {
                    Txt(item.note, TtoType.BodySmall, color = if (item.checked) TtoColor.Faint else TtoColor.GraySecondary)
                }
            }
            MinusButton({ vm.remove(item) }, "Remove ${item.name}")
        }
        HairlineRule()
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(R.drawable.icon_arrow_doodle), null, Modifier.height(44.dp).rotate(150f))
        Spacer(Modifier.height(12.dp))
        Txt(
            "Nothing yet. Out of milk? Write it before you forget.",
            TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
            Modifier.padding(horizontal = 32.dp), color = TtoColor.GrayBody, align = TextAlign.Center,
        )
    }
}

@Composable
private fun AllSortedState(onClearDone: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(R.drawable.mark_confetti), null, Modifier.height(44.dp))
        Spacer(Modifier.height(8.dp))
        Txt("All sorted!", TtoType.ScreenTitle)
        Txt("Everything's in the cart.", TtoType.BodySmall.copy(fontSize = 13.sp), Modifier.padding(top = 4.dp), color = TtoColor.GrayBody)
        Txt(
            "Clear done",
            TtoType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline),
            Modifier.padding(top = 16.dp).tap(onClick = onClearDone).padding(8.dp),
        )
    }
}

@Composable
private fun EditSheet(item: Item, onDismiss: () -> Unit, onSave: (String, String) -> Unit, onDelete: () -> Unit) {
    var name by remember(item.id) { mutableStateOf(TextFieldValue(item.name, TextRange(item.name.length))) }
    var note by remember(item.id) { mutableStateOf(TextFieldValue(item.note.orEmpty(), TextRange(item.note.orEmpty().length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(item.id) { focus.requestFocus() }

    Box(Modifier.fillMaxSize().background(Color(0x6B171717)).tap(onClick = onDismiss)) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)).background(TtoColor.Paper)
                .tap { }.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
        ) {
            Box(Modifier.size(width = 36.dp, height = 4.dp).clip(TtoRadius.pill).background(TtoColor.Faint).align(Alignment.CenterHorizontally))
            Txt("Edit item", TtoType.SectionTitle, Modifier.padding(top = 14.dp))
            Txt("NAME", TtoType.Caption, Modifier.padding(top = 16.dp), color = TtoColor.GraySecondary)
            SheetField(name, { name = it }, Modifier.padding(top = 6.dp).focusRequester(focus), TtoColor.Ink, ImeAction.Next)
            Txt("NOTE · OPTIONAL", TtoType.Caption, Modifier.padding(top = 14.dp), color = TtoColor.GraySecondary)
            SheetField(note, { note = it }, Modifier.padding(top = 6.dp), TtoColor.Line, ImeAction.Done)
            Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).clip(TtoRadius.md).border(1.5.dp, TtoColor.Ink, TtoRadius.md).tap(onClick = onDelete).padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Txt("Delete", TtoType.Button) }
                val canSave = name.text.isNotBlank()
                Box(
                    Modifier.weight(1f).clip(TtoRadius.md).background(if (canSave) TtoColor.Ink else TtoColor.Line)
                        .tap(canSave) { onSave(name.text, note.text) }.padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) { Txt("Save", TtoType.Button, color = Color.White) }
            }
        }
    }
}

@Composable
private fun SheetField(value: TextFieldValue, onChange: (TextFieldValue) -> Unit, modifier: Modifier, borderColor: Color, ime: ImeAction) {
    Box(modifier.fillMaxWidth().border(1.5.dp, borderColor, TtoRadius.md).padding(horizontal = 16.dp, vertical = 14.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TtoType.Body.copy(fontWeight = FontWeight.Normal, color = TtoColor.Ink),
            cursorBrush = SolidColor(TtoColor.Orange),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ime),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
