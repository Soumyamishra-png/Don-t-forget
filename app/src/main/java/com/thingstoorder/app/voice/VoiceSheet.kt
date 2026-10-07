package com.thingstoorder.app.voice

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import com.thingstoorder.app.ui.theme.TtoColor
import com.thingstoorder.app.ui.theme.TtoRadius
import com.thingstoorder.app.ui.theme.TtoType

@Composable
fun MicButton(onClick: () -> Unit, size: Int = 64, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size.dp).clip(CircleShape).background(TtoColor.Ink).tap(onClick = onClick)
            .semantics { contentDescription = "Say what you need" },
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.icon_mic), null, Modifier.size((size * 0.42f).dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White))
    }
}

/**
 * Bottom sheet that follows the capture state. Renders nothing while idle.
 * [onAdd] receives the items the user left in the review list.
 */
@Composable
fun VoiceSheet(
    controller: VoiceController,
    onAdd: (List<ParsedItem>) -> Unit,
    addLabel: (Int) -> String = { n -> if (n == 1) "Add 1 item" else "Add $n items" },
) {
    val capture = controller.capture
    val state = capture.state
    if (state is VoiceState.Idle) return

    Box(Modifier.fillMaxSize().background(Color(0x6B171717)).tap { capture.reset() }) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)).background(TtoColor.Paper)
                .tap { }.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(width = 36.dp, height = 4.dp).clip(TtoRadius.pill).background(TtoColor.Faint))
            when (state) {
                is VoiceState.Listening -> Listening(state.partial, onDone = capture::finish)
                is VoiceState.Heard -> Heard(state.text, controller, onAdd, addLabel)
                VoiceState.NothingHeard -> Problem("Didn't catch that", "Try again, a little closer to the phone.", controller)
                VoiceState.Offline -> Problem("Can't reach the speech service", "Voice needs a connection on this phone. You can still type.", controller)
                VoiceState.Failed -> Problem("Something went wrong", "The speech service hiccuped. Try once more, or type instead.", controller)
                VoiceState.Unavailable -> Problem("No speech service here", "This phone has no speech recogniser installed. You can still type.", null)
                VoiceState.Denied -> Denied()
                VoiceState.Idle -> Unit
            }
            Txt(
                "Close", TtoType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                Modifier.padding(top = 14.dp).tap { capture.reset() }.padding(8.dp), color = TtoColor.GrayBody,
            )
        }
    }
}

@Composable
private fun Listening(partial: String, onDone: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale = pulse.animateFloat(1f, 1.35f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "scale")
    val glow = pulse.animateFloat(0.35f, 0.08f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "glow")
    Txt("Listening…", TtoType.SectionTitle, Modifier.padding(top = 18.dp))
    Box(Modifier.padding(vertical = 26.dp).size(96.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(64.dp).scale(scale.value).alpha(glow.value).clip(CircleShape).background(TtoColor.Orange))
        MicButton(onClick = onDone)
    }
    Txt(
        partial.ifBlank { "Say what you need. Hindi words are fine." },
        TtoType.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 21.sp),
        Modifier.padding(horizontal = 8.dp),
        color = if (partial.isBlank()) TtoColor.GraySecondary else TtoColor.Ink, align = TextAlign.Center,
    )
    Txt("Tap the mic when you're done", TtoType.BodySmall, Modifier.padding(top = 10.dp), color = TtoColor.Faint)
}

@Composable
private fun Heard(text: String, controller: VoiceController, onAdd: (List<ParsedItem>) -> Unit, addLabel: (Int) -> String) {
    val items = remember(text) { mutableStateListOf<ParsedItem>().apply { addAll(VoiceParser.parse(text)) } }
    if (items.isEmpty() && VoiceParser.parse(text).isEmpty()) {
        Problem("Didn't catch any items", "I heard \"$text\". Try naming things like milk, eggs, bread.", controller)
        return
    }
    Txt("Here's what I got", TtoType.SectionTitle, Modifier.padding(top = 18.dp))
    Txt("\"$text\"", TtoType.BodySmall, Modifier.padding(top = 4.dp), color = TtoColor.GraySecondary, align = TextAlign.Center)
    FlowRow(
        Modifier.padding(top = 16.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            Row(
                Modifier.clip(TtoRadius.pill).background(TtoColor.OffWhite).border(1.dp, TtoColor.Line, TtoRadius.pill)
                    .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt(item.name, TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
                if (item.quantity != null) Txt("  ${item.quantity}", TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal), color = TtoColor.OrangeText)
                Box(
                    Modifier.padding(start = 4.dp).size(26.dp).tap { items.remove(item) }.semantics { contentDescription = "Remove ${item.name}" },
                    contentAlignment = Alignment.Center,
                ) { Txt("×", TtoType.Body.copy(fontSize = 18.sp, fontWeight = FontWeight.Normal), color = TtoColor.GrayBody) }
            }
        }
    }
    Row(Modifier.padding(top = 22.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.weight(1f).clip(TtoRadius.md).border(1.5.dp, TtoColor.Ink, TtoRadius.md).tap(onClick = controller.begin).padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) { Txt("Try again", TtoType.Button) }
        val canAdd = items.isNotEmpty()
        Box(
            Modifier.weight(1.4f).clip(TtoRadius.md).background(if (canAdd) TtoColor.Ink else TtoColor.Line)
                .tap(canAdd) { val out = items.toList(); controller.capture.reset(); onAdd(out) }.padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) { Txt(if (canAdd) addLabel(items.size) else "Nothing to add", TtoType.Button, color = Color.White) }
    }
}

@Composable
private fun Problem(title: String, body: String, controller: VoiceController?) {
    Image(painterResource(R.drawable.mark_face_confused), null, Modifier.padding(top = 18.dp).size(56.dp))
    Txt(title, TtoType.SectionTitle, Modifier.padding(top = 12.dp), align = TextAlign.Center)
    Txt(
        body, TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
        Modifier.padding(top = 6.dp, start = 8.dp, end = 8.dp), color = TtoColor.GrayBody, align = TextAlign.Center,
    )
    if (controller != null) {
        Box(
            Modifier.padding(top = 20.dp).fillMaxWidth().clip(TtoRadius.md).background(TtoColor.Ink).tap(onClick = controller.begin).padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) { Txt("Try again", TtoType.Button, color = Color.White) }
    }
}

@Composable
private fun Denied() {
    val context = LocalContext.current
    Image(painterResource(R.drawable.mark_face_confused), null, Modifier.padding(top = 18.dp).size(56.dp))
    Txt("Mic is off for Don't Forget", TtoType.SectionTitle, Modifier.padding(top = 12.dp), align = TextAlign.Center)
    Txt(
        "Allow the microphone and you can say your list instead of typing it. You can always type.",
        TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
        Modifier.padding(top = 6.dp, start = 8.dp, end = 8.dp), color = TtoColor.GrayBody, align = TextAlign.Center,
    )
    Box(
        Modifier.padding(top = 20.dp).fillMaxWidth().clip(TtoRadius.md).background(TtoColor.Ink).tap {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) { Txt("Open settings", TtoType.Button, color = Color.White) }
}
