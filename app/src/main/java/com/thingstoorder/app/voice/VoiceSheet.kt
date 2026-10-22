package com.thingstoorder.app.voice

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thingstoorder.app.R
import com.thingstoorder.app.ui.components.FaceMood
import com.thingstoorder.app.ui.components.FaceTile
import com.thingstoorder.app.ui.components.Txt
import com.thingstoorder.app.ui.components.tap
import com.thingstoorder.app.ui.theme.TtoColor
import com.thingstoorder.app.ui.theme.TtoRadius
import com.thingstoorder.app.ui.theme.TtoType

/** The voice-first entry: a round ink button with a paper mic. 120dp in onboarding, 80dp on Home, 72dp on a shared list. */
@Composable
fun MicButton(onClick: () -> Unit, size: Int = 64, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size.dp).clip(CircleShape).background(TtoColor.Ink).tap(onClick = onClick)
            .semantics { contentDescription = "Say what you need" },
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.icon_mic), null, Modifier.size((size * 0.4f).dp), colorFilter = ColorFilter.tint(TtoColor.Paper))
    }
}

/** 52dp action button used across the voice screens: 2dp ink outline or ink fill. Pairs take 1 part and 2 parts. */
@Composable
fun RowScope.VoiceButton(label: String, filled: Boolean, weight: Float, onClick: () -> Unit, icon: Boolean = false) {
    Row(
        Modifier.weight(weight).height(52.dp).clip(TtoRadius.md)
            .then(if (filled) Modifier.background(TtoColor.Ink) else Modifier.border(2.dp, TtoColor.Ink, TtoRadius.md))
            .tap(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon) {
            Image(painterResource(R.drawable.icon_mic), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(if (filled) TtoColor.Paper else TtoColor.Ink))
            Spacer(Modifier.size(8.dp))
        }
        Txt(label, TtoType.Button, color = if (filled) TtoColor.Paper else TtoColor.Ink)
    }
}

/**
 * Bottom sheet that follows the capture state. Renders nothing while idle.
 * [onAdd] receives the items the user left in the review list; [onTypeInstead] is for screens that have a type field.
 */
@Composable
fun VoiceSheet(
    controller: VoiceController,
    onAdd: (List<ParsedItem>) -> Unit,
    onTypeInstead: () -> Unit = {},
    addLabel: (Int) -> String = { n -> if (n == 1) "Add 1 item" else "Add $n items" },
) {
    val capture = controller.capture
    val state = capture.state
    if (state is VoiceState.Idle) return
    BackHandler { capture.reset() }

    val typeInstead = { capture.reset(); onTypeInstead() }

    Box(Modifier.fillMaxSize().background(TtoColor.Scrim).tap { capture.reset() }) {
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(TtoRadius.sheet).tap { }) {
            when (state) {
                is VoiceState.Listening -> MintSheet { Listening(state.partial, onDone = capture::finish) }
                is VoiceState.Heard -> {
                    val parsed = remember(state.text) { VoiceParser.parse(state.text) }
                    if (parsed.isEmpty()) {
                        ProblemSheet("Didn't catch any items", "I heard \"${state.text}\". Try naming things like milk, eggs, bread.", controller.begin, typeInstead)
                    } else {
                        MintSheet { Heard(state.text, parsed, controller, onAdd, addLabel) }
                    }
                }
                VoiceState.NothingHeard -> ProblemSheet("Didn't catch that", "Try again, a little closer.", controller.begin, typeInstead)
                VoiceState.Offline -> ProblemSheet("Can't reach the speech service", "Voice needs a connection on this phone. You can still type.", controller.begin, typeInstead)
                VoiceState.Failed -> ProblemSheet("Something went wrong", "The speech service hiccuped. Try once more, or type instead.", controller.begin, typeInstead)
                VoiceState.Unavailable -> ProblemSheet("No speech service here", "This phone has no speech recogniser installed. You can still type.", null, typeInstead)
                VoiceState.Denied -> DeniedSheet(typeInstead)
                VoiceState.Idle -> Unit
            }
        }
    }
}

/** The listening sheet: mint, with a grip, 24dp insets and 16dp between blocks. */
@Composable
private fun MintSheet(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(TtoColor.Mint).navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(width = 40.dp, height = 4.dp).clip(TtoRadius.pill).background(TtoColor.Paper))
        content()
    }
}

@Composable
private fun Listening(partial: String, onDone: () -> Unit) {
    FaceTile(FaceMood.Listening, 64.dp)
    Txt(
        if (partial.isBlank()) "Listening…" else "\"$partial\"",
        TtoType.Transcript, Modifier.fillMaxWidth().padding(vertical = 8.dp),
        color = if (partial.isBlank()) TtoColor.InkMuted else TtoColor.Ink, align = TextAlign.Center,
    )
    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) { VoiceButton("Done", filled = true, weight = 1f, onClick = onDone) }
}

@Composable
private fun Heard(
    text: String, parsed: List<ParsedItem>, controller: VoiceController,
    onAdd: (List<ParsedItem>) -> Unit, addLabel: (Int) -> String,
) {
    val items = remember(text) { mutableStateListOf<ParsedItem>().apply { addAll(parsed) } }
    FaceTile(FaceMood.Listening, 64.dp)
    Txt("\"$text\"", TtoType.Transcript, Modifier.fillMaxWidth(), align = TextAlign.Center)
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            Row(
                Modifier.clip(TtoRadius.pill).background(TtoColor.Paper).padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt(item.name, TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), Modifier.padding(vertical = 12.dp))
                if (item.quantity != null) {
                    Txt(item.quantity, TtoType.Tag, Modifier.padding(start = 8.dp), color = TtoColor.GreenDeep)
                }
                Box(
                    Modifier.size(32.dp).tap { items.remove(item) }.semantics { contentDescription = "Remove ${item.name}" },
                    contentAlignment = Alignment.Center,
                ) { Image(painterResource(R.drawable.icon_minus), null, Modifier.size(14.dp)) }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VoiceButton("Retry", filled = false, weight = 1f, onClick = controller.begin)
        val canAdd = items.isNotEmpty()
        if (canAdd) {
            VoiceButton(addLabel(items.size), filled = true, weight = 2f, onClick = { val out = items.toList(); controller.capture.reset(); onAdd(out) })
        } else {
            VoiceButton("Nothing to add", filled = true, weight = 2f, onClick = {})
        }
    }
}

/** "Didn't catch that" and friends: white sheet, title and helper on the left, the puzzled face on the right, then Type instead + Retry. */
@Composable
private fun ProblemSheet(title: String, body: String, onRetry: (() -> Unit)?, onTypeInstead: () -> Unit) {
    PaperSheet {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Txt(title, TtoType.SheetTitle)
                Txt(body, TtoType.BodySmall, color = TtoColor.InkMuted)
            }
            FaceTile(FaceMood.Confused, 64.dp)
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onRetry != null) {
                VoiceButton("Type instead", filled = false, weight = 1f, onClick = onTypeInstead)
                VoiceButton("Retry", filled = true, weight = 2f, onClick = onRetry)
            } else {
                VoiceButton("Type instead", filled = true, weight = 1f, onClick = onTypeInstead)
            }
        }
    }
}

@Composable
private fun DeniedSheet(onTypeInstead: () -> Unit) {
    val context = LocalContext.current
    PaperSheet {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Txt("No mic, no problem.", TtoType.SheetTitle)
                Txt("You can type for now. Turn the mic on in Settings whenever you like.", TtoType.BodySmall, color = TtoColor.InkMuted)
            }
            FaceTile(FaceMood.Confused, 64.dp)
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoiceButton("Type instead", filled = false, weight = 1f, onClick = onTypeInstead)
            VoiceButton(
                "Open Settings", filled = true, weight = 2f,
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            )
        }
    }
}

@Composable
private fun PaperSheet(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(TtoColor.Paper).navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(width = 40.dp, height = 4.dp).clip(TtoRadius.pill).background(TtoColor.Line).align(Alignment.CenterHorizontally))
        content()
    }
}
