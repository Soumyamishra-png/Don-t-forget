package com.thingstoorder.app.ui.shared

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thingstoorder.app.data.AppDatabase
import com.thingstoorder.app.data.SettingsRepository
import com.thingstoorder.app.data.shared.JoinPreview
import com.thingstoorder.app.data.shared.MAX_MEMBERS
import com.thingstoorder.app.data.shared.SharedError
import com.thingstoorder.app.data.shared.SharedLists
import com.thingstoorder.app.ui.components.Txt
import com.thingstoorder.app.ui.components.tap
import com.thingstoorder.app.ui.theme.TtoColor
import com.thingstoorder.app.ui.theme.TtoRadius
import com.thingstoorder.app.ui.theme.TtoType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One colour per seat: orange, blue, green, purple. */
val MemberColors = listOf(TtoColor.PersonYou, TtoColor.Person2, TtoColor.Person3, TtoColor.Person4)

class SharedViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SharedLists.get(app)
    private val settings = SettingsRepository(app)
    private val dao = AppDatabase.get(app).itemDao()

    val current = repo.current
    val myUid get() = repo.myUid
    val sharedId = settings.sharedListId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val code = settings.sharedCode.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val savedName = settings.myName.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
    var preview by mutableStateOf<JoinPreview?>(null)
        private set
    var localNames by mutableStateOf<List<String>>(emptyList())
        private set

    fun clearError() { error = null }
    fun clearPreview() { preview = null }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy = true
            error = null
            try { block() } catch (e: SharedError) { error = e.message } finally { busy = false }
        }
    }

    fun create(name: String) = run { repo.createShare(name.trim()) }

    fun lookup(code: String) = run {
        preview = repo.preview(code)
        localNames = dao.getLocalOnly().map { it.name }
    }

    fun join(code: String, name: String, onDone: () -> Unit) = run {
        repo.join(code, name.trim())
        preview = null
        onDone()
    }

    fun leave(onDone: () -> Unit) = run { repo.leave(); onDone() }

    fun remove(uid: String) = run { repo.removeMember(uid) }
}

private enum class Page { Share, JoinCode }

/** Bottom sheet for sharing: create a code, see who is on the list, or join someone else's list. */
@Composable
fun ShareSheet(onDismiss: () -> Unit, vm: SharedViewModel = viewModel()) {
    val context = LocalContext.current
    val shared by vm.current.collectAsStateWithLifecycle()
    val sharedId by vm.sharedId.collectAsStateWithLifecycle()
    val code by vm.code.collectAsStateWithLifecycle()
    val savedName by vm.savedName.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(Page.Share) }
    var name by rememberSaveable { mutableStateOf("") }
    var joinCode by rememberSaveable { mutableStateOf("") }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(savedName) { if (name.isEmpty() && !savedName.isNullOrEmpty()) name = savedName.orEmpty() }

    Box(Modifier.fillMaxSize().background(TtoColor.Scrim).tap(onClick = onDismiss)) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding()
                .clip(TtoRadius.sheet).background(TtoColor.Paper)
                .tap { }.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp),
        ) {
            Box(Modifier.size(width = 40.dp, height = 4.dp).clip(TtoRadius.pill).background(TtoColor.Line).align(Alignment.CenterHorizontally))

            when {
                sharedId != null -> SharedView(
                    vm = vm, code = code.orEmpty(), confirmLeave = confirmLeave,
                    onLeaveTap = { if (confirmLeave) vm.leave { onDismiss() } else confirmLeave = true },
                    onInvite = { context.startActivity(Intent.createChooser(inviteIntent(code.orEmpty()), "Send invite")) },
                    onCopy = { copy(context, code.orEmpty()) },
                )
                page == Page.JoinCode && vm.preview != null -> ConfirmView(
                    vm = vm, onJoin = { vm.join(joinCode, name) { onDismiss() } },
                    onBack = { vm.clearPreview() },
                )
                page == Page.JoinCode -> JoinCodeView(
                    vm = vm, code = joinCode, onCode = { joinCode = it.uppercase().take(12); vm.clearError() },
                    name = name, onName = { name = it }, onBack = { page = Page.Share; vm.clearError() },
                    onContinue = { vm.lookup(joinCode) },
                )
                else -> CreateView(
                    vm = vm, name = name, onName = { name = it },
                    onCreate = { vm.create(name) },
                    onJoin = { page = Page.JoinCode; vm.clearError() },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.CreateView(vm: SharedViewModel, name: String, onName: (String) -> Unit, onCreate: () -> Unit, onJoin: () -> Unit) {
    Txt("Order together", TtoType.SheetTitle, Modifier.padding(top = 16.dp))
    Txt(
        "Anyone with the code can add to this list. Up to $MAX_MEMBERS people.",
        TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
        Modifier.padding(top = 4.dp), color = TtoColor.GrayBody,
    )
    Txt("YOUR NAME", TtoType.Caption, Modifier.padding(top = 18.dp), color = TtoColor.GraySecondary)
    SheetInput(name, onName, "What should they call you?", Modifier.padding(top = 6.dp), ImeAction.Done)
    ErrorText(vm.error)
    val can = name.isNotBlank() && !vm.busy
    Box(
        Modifier.padding(top = 18.dp).fillMaxWidth().clip(TtoRadius.md).background(if (can) TtoColor.Ink else TtoColor.Line)
            .tap(can, onClick = onCreate).height(52.dp),
        contentAlignment = Alignment.Center,
    ) { Txt(if (vm.busy) "Making your code…" else "Create code", TtoType.Button, color = Color.White) }
    Txt(
        "Have a code? Join a list",
        TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline),
        Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).tap(onClick = onJoin).padding(8.dp),
    )
}

@Composable
private fun ColumnScope.SharedView(
    vm: SharedViewModel, code: String, confirmLeave: Boolean,
    onLeaveTap: () -> Unit, onInvite: () -> Unit, onCopy: () -> Unit,
) {
    val shared by vm.current.collectAsStateWithLifecycle()
    val me = vm.myUid
    val members = shared?.members.orEmpty()
    val iAmOwner = shared?.ownerUid == me

    Txt("Order together", TtoType.SheetTitle, Modifier.padding(top = 16.dp))
    Txt(
        "Anyone with the code can add to this list. Up to $MAX_MEMBERS people.",
        TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
        Modifier.padding(top = 4.dp), color = TtoColor.GrayBody,
    )
    Row(
        Modifier.padding(top = 16.dp).fillMaxWidth().clip(TtoRadius.md).background(TtoColor.OffWhite).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(code.chunked(3).joinToString(" "), TtoType.ScreenTitle.copy(letterSpacing = 3.sp), Modifier.weight(1f))
        Box(Modifier.height(44.dp).tap(onClick = onCopy).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Txt("Copy", TtoType.Button.copy(fontSize = 14.sp), color = TtoColor.GreenDeep)
        }
    }

    Column(Modifier.padding(top = 14.dp)) {
        if (members.isEmpty()) {
            MemberRow("You", 0, "1 of $MAX_MEMBERS", null)
        } else {
            members.forEachIndexed { i, m ->
                val mine = m.uid == me
                MemberRow(
                    name = if (mine) "You" else m.name.ifBlank { "Someone" },
                    color = m.color,
                    trailing = if (i == 0) "${members.size} of $MAX_MEMBERS" else null,
                    onRemove = if (iAmOwner && !mine) ({ vm.remove(m.uid) }) else null,
                )
            }
        }
    }
    ErrorText(vm.error)
    Box(
        Modifier.padding(top = 16.dp).fillMaxWidth().clip(TtoRadius.md).background(TtoColor.Ink).tap(onClick = onInvite).height(52.dp),
        contentAlignment = Alignment.Center,
    ) { Txt("Send invite code", TtoType.Button, color = Color.White) }
    Txt(
        if (confirmLeave) "Tap again to leave. Your items stay on this phone." else "Leave this list",
        TtoType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline),
        Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp).tap(onClick = onLeaveTap).padding(8.dp),
        color = TtoColor.CoralDeep,
    )
}

@Composable
private fun MemberRow(name: String, color: Int, trailing: String?, onRemove: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(MemberColors[color.coerceIn(0, MemberColors.lastIndex)]))
        Txt(name, TtoType.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), Modifier.padding(start = 12.dp).weight(1f))
        if (onRemove != null) {
            Txt(
                "Remove",
                TtoType.Button.copy(fontSize = 14.sp),
                Modifier.tap(onClick = onRemove).padding(horizontal = 6.dp, vertical = 12.dp), color = TtoColor.CoralDeep,
            )
        } else if (trailing != null) {
            Txt(trailing, TtoType.BodySmall, color = TtoColor.GraySecondary)
        }
    }
}

@Composable
private fun ColumnScope.JoinCodeView(
    vm: SharedViewModel, code: String, onCode: (String) -> Unit, name: String, onName: (String) -> Unit,
    onBack: () -> Unit, onContinue: () -> Unit,
) {
    Txt("Join a list", TtoType.SheetTitle, Modifier.padding(top = 16.dp))
    Txt(
        "Type the code you were sent.",
        TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal), Modifier.padding(top = 4.dp), color = TtoColor.GrayBody,
    )
    Txt("CODE", TtoType.Caption, Modifier.padding(top = 18.dp), color = TtoColor.GraySecondary)
    SheetInput(code, onCode, "e.g. K7Q2XM", Modifier.padding(top = 6.dp), ImeAction.Next, caps = KeyboardCapitalization.Characters)
    Txt("YOUR NAME", TtoType.Caption, Modifier.padding(top = 14.dp), color = TtoColor.GraySecondary)
    SheetInput(name, onName, "What should they call you?", Modifier.padding(top = 6.dp), ImeAction.Done)
    ErrorText(vm.error)
    val can = code.length >= 4 && name.isNotBlank() && !vm.busy
    Box(
        Modifier.padding(top = 18.dp).fillMaxWidth().clip(TtoRadius.md).background(if (can) TtoColor.Ink else TtoColor.Line)
            .tap(can, onClick = onContinue).height(52.dp),
        contentAlignment = Alignment.Center,
    ) { Txt("Continue", TtoType.Button, color = Color.White) }
    Txt("Back", TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp).tap(onClick = onBack).padding(8.dp), color = TtoColor.GrayBody)
}

@Composable
private fun ColumnScope.ConfirmView(vm: SharedViewModel, onJoin: () -> Unit, onBack: () -> Unit) {
    val preview = vm.preview ?: return
    Txt(
        "${preview.ownerName.ifBlank { "Someone" }} invited you to their list", TtoType.SectionTitle,
        Modifier.padding(top = 14.dp),
    )
    Txt(
        if (vm.localNames.isEmpty()) "You have no items yet, so you'll just see theirs."
        else "Your ${vm.localNames.size} ${if (vm.localNames.size == 1) "item" else "items"} will move into it. Duplicates become one.",
        TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
        Modifier.padding(top = 4.dp), color = TtoColor.GrayBody,
    )
    if (vm.localNames.isNotEmpty()) {
        FlowRow(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            vm.localNames.take(8).forEach {
                Box(Modifier.clip(TtoRadius.pill).background(TtoColor.OffWhite).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Txt(it, TtoType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal))
                }
            }
        }
    }
    ErrorText(vm.error)
    Box(
        Modifier.padding(top = 20.dp).fillMaxWidth().clip(TtoRadius.md).background(if (vm.busy) TtoColor.Line else TtoColor.Ink)
            .tap(!vm.busy, onClick = onJoin).height(52.dp),
        contentAlignment = Alignment.Center,
    ) { Txt(if (vm.busy) "Joining…" else "Join list", TtoType.Button, color = Color.White) }
    Txt("Not now", TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp).tap(onClick = onBack).padding(8.dp), color = TtoColor.GrayBody)
}

@Composable
private fun ErrorText(message: String?) {
    if (message != null) Txt(message, TtoType.BodySmall.copy(fontSize = 13.sp), Modifier.padding(top = 10.dp), color = TtoColor.OrangeText)
}

@Composable
private fun SheetInput(
    value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier, ime: ImeAction,
    caps: KeyboardCapitalization = KeyboardCapitalization.Words,
) {
    Box(modifier.fillMaxWidth().border(2.dp, TtoColor.LineStrong, TtoRadius.md).padding(horizontal = 16.dp, vertical = 16.dp)) {
        if (value.isEmpty()) Txt(placeholder, TtoType.Body.copy(fontWeight = FontWeight.Normal), color = TtoColor.Faint)
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = TtoType.Body.copy(fontWeight = FontWeight.Normal, color = TtoColor.Ink),
            cursorBrush = SolidColor(TtoColor.Orange),
            keyboardOptions = KeyboardOptions(capitalization = caps, imeAction = ime),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun inviteIntent(code: String) = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_TEXT, "Order with me on Don't Forget. Open the app, tap Share list, then Join a list, and enter this code: $code")
}

private fun copy(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Don't Forget code", code))
}

/** Header pill: "Share list" in ink, or an avatar stack with the member count once the list is shared. */
@Composable
fun SharePill(onClick: () -> Unit, members: List<Int>, modifier: Modifier = Modifier) {
    val shared = members.isNotEmpty()
    Row(
        modifier.height(36.dp).clip(TtoRadius.pill).background(if (shared) TtoColor.TealSoft else TtoColor.Ink).tap(onClick = onClick)
            .semantics { contentDescription = if (shared) "Shared with ${members.size} people. Open sharing." else "Share list" }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!shared) {
            Txt("Share list", TtoType.Chip, color = TtoColor.Paper)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                members.forEach { Box(Modifier.size(14.dp).clip(CircleShape).background(MemberColors[it.coerceIn(0, MemberColors.lastIndex)])) }
            }
            Txt("${members.size}", TtoType.Chip, Modifier.padding(start = 8.dp), color = TtoColor.Ink)
        }
    }
}
