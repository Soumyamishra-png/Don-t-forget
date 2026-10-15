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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.thingstoorder.app.ui.components.Txt
import com.thingstoorder.app.ui.components.tap
import com.thingstoorder.app.ui.theme.Bricolage
import com.thingstoorder.app.ui.theme.TtoColor
import com.thingstoorder.app.ui.theme.TtoRadius
import com.thingstoorder.app.ui.theme.TtoType

private val ListeningBlue = Color(0xFF4FB3FF)
private val ProblemBlue = Color(0xFF9AD4FF)
private val QuantityBlue = Color(0xFF1F6BFF)
private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

@Composable
fun MicButton(onClick: () -> Unit, size: Int = 64, modifier: Modifier = Modifier, lifted: Boolean = false) {
    Box(
        modifier.then(if (lifted) Modifier.shadow(8.dp, CircleShape) else Modifier)
            .size(size.dp).clip(CircleShape).background(TtoColor.Ink).tap(onClick = onClick)
            .semantics { contentDescription = "Say what you need" },
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.icon_mic), null, Modifier.size((size * 0.4f).dp), colorFilter = ColorFilter.tint(Color.White))
    }
}

/** 52dp action button used across the voice screens: outlined or filled, 14dp corners. */
@Composable
fun RowScope.VoiceButton(label: String, filled: Boolean, weight: Float, onClick: () -> Unit, icon: Boolean = false) {
    Row(
        Modifier.weight(weight).height(52.dp).clip(TtoRadius.md)
            .then(if (filled) Modifier.background(TtoColor.Ink) else Modifier.border(1.5.dp, TtoColor.Ink, TtoRadius.md))
            .tap(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon) {
            Image(painterResource(R.drawable.icon_mic), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(if (filled) Color.White else TtoColor.Ink))
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        }
        Txt(label, TtoType.Button, color = if (filled) Color.White else TtoColor.Ink)
    }
}

private val Transcript = TtoType.Body.copy(fontFamily = Bricolage, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp)

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

    Box(Modifier.fillMaxSize().background(Color(0x6B171717)).tap { capture.reset() }) {
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(SheetShape).tap { }) {
            when (state) {
                is VoiceState.Listening -> BlueSheet { Listening(state.partial, onDone = capture::finish) }
                is VoiceState.Heard -> {
                    val parsed = remember(state.text) { VoiceParser.parse(state.text) }
                    if (parsed.isEmpty()) {
                        ProblemSheet("Didn't catch any items", "I heard \"${state.text}\". Try naming things like milk, eggs, bread.", controller.begin, typeInstead)
                    } else {
                        BlueSheet { Heard(state.text, parsed, controller, onAdd, addLabel) }
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

@Composable
private fun BlueSheet(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(ListeningBlue).navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

@Composable
private fun Face() {
    Image(painterResource(R.drawable.mark_face_typing), null, Modifier.size(width = 64.dp, height = 52.dp))
}

@Composable
private fun Listening(partial: String, onDone: () -> Unit) {
    Face()
    Txt(
        if (partial.isBlank()) "Listening…" else "\"$partial\"",
        Transcript, Modifier.fillMaxWidth().padding(vertical = 8.dp),
        color = if (partial.isBlank()) TtoColor.Ink.copy(alpha = 0.55f) else TtoColor.Ink, align = TextAlign.Center,
    )
    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) { VoiceButton("Done", filled = true, weight = 1f, onClick = onDone) }
}

@Composable
private fun Heard(
    text: String, parsed: List<ParsedItem>, controller: VoiceController,
    onAdd: (List<ParsedItem>) -> Unit, addLabel: (Int) -> String,
) {
    val items = remember(text) { mutableStateListOf<ParsedItem>().apply { addAll(parsed) } }
    Face()
    Txt("\"$text\"", Transcript.copy(fontSize = 22.sp, lineHeight = 28.sp), Modifier.fillMaxWidth(), align = TextAlign.Center)
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            Row(
                Modifier.clip(TtoRadius.pill).background(Color.White).padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt(item.name, TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), Modifier.padding(vertical = 4.dp))
                if (item.quantity != null) {
                    Txt(item.quantity, TtoType.Body.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), Modifier.padding(start = 8.dp), color = QuantityBlue)
                }
                Box(
                    Modifier.padding(start = 2.dp).size(28.dp).tap { items.remove(item) }.semantics { contentDescription = "Remove ${item.name}" },
                    contentAlignment = Alignment.Center,
                ) { Txt("×", TtoType.Body.copy(fontSize = 16.sp, fontWeight = FontWeight.Normal), color = TtoColor.GrayBody) }
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

/** "Didn't catch that" and friends: light-blue band with the puzzled face, then two equal buttons. */
@Composable
private fun ProblemSheet(title: String, body: String, onRetry: (() -> Unit)?, onTypeInstead: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(70.dp).background(ProblemBlue), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.mark_face_confused), null, Modifier.size(width = 64.dp, height = 52.dp))
        }
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Txt(title, TtoType.SectionTitle.copy(fontSize = 20.sp))
            Txt(body, TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp), color = TtoColor.GrayBody)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onRetry != null) {
                    VoiceButton("Type instead", filled = false, weight = 1f, onClick = onTypeInstead)
                    VoiceButton("Retry", filled = true, weight = 1f, onClick = onRetry)
                } else {
                    VoiceButton("Type instead", filled = true, weight = 1f, onClick = onTypeInstead)
                }
            }
        }
    }
}

@Composable
private fun DeniedSheet(onTypeInstead: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(70.dp).background(ProblemBlue), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.mark_face_confused), null, Modifier.size(width = 64.dp, height = 52.dp))
        }
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Txt("Mic is off for Don't Forget", TtoType.SectionTitle.copy(fontSize = 20.sp))
            Txt(
                "Allow the microphone and you can say your list instead of typing it. You can always type.",
                TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp), color = TtoColor.GrayBody,
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoiceButton("Type instead", filled = false, weight = 1f, onClick = onTypeInstead)
                VoiceButton(
                    "Open settings", filled = true, weight = 1f,
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                )
            }
        }
    }
}
