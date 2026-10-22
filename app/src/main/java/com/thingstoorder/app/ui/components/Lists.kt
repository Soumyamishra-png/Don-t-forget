package com.thingstoorder.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thingstoorder.app.R
import com.thingstoorder.app.ui.theme.TtoColor
import com.thingstoorder.app.ui.theme.TtoRadius
import com.thingstoorder.app.ui.theme.TtoType

@Composable
fun HairlineRule(modifier: Modifier = Modifier) =
    Box(modifier.fillMaxWidth().height(1.dp).background(TtoColor.Line))

/** Remove button: a plain minus with a 44dp touch target. */
@Composable
fun MinusButton(onClick: () -> Unit, description: String = "Remove") {
    Box(
        Modifier.size(48.dp).tap(onClick = onClick).semantics { contentDescription = description; role = Role.Button },
        contentAlignment = Alignment.CenterEnd,
    ) {
        Image(painterResource(R.drawable.icon_minus), null, Modifier.size(20.dp))
    }
}

@Composable
fun ToggleSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Box(
        Modifier.width(48.dp).height(28.dp).clip(TtoRadius.pill)
            .background(if (checked) TtoColor.Ink else TtoColor.Line)
            .tap { onCheckedChange(!checked) }.padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) { Box(Modifier.size(22.dp).clip(CircleShape).background(TtoColor.Paper)) }
}

/** The text input: 56dp, 2dp border that is line-strong at rest and ink when focused, trailing mic and plus. */
@Composable
fun AddField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onMic: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth().height(56.dp).clip(TtoRadius.md).background(TtoColor.Paper)
            .border(2.dp, if (focused) TtoColor.Ink else TtoColor.LineStrong, TtoRadius.md)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Txt(placeholder, TtoType.Body, color = TtoColor.InkSubtle)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TtoType.Body.copy(color = TtoColor.Ink),
                cursorBrush = SolidColor(TtoColor.GreenDeep),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            )
        }
        if (onMic != null) {
            Box(Modifier.size(48.dp).tap(onClick = onMic).semantics { contentDescription = "Say what you need" }, contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.icon_mic), null, Modifier.size(20.dp))
            }
        }
        Box(Modifier.size(48.dp).tap(onClick = onSubmit).semantics { contentDescription = "Add" }, contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.icon_plus), null, Modifier.size(20.dp))
        }
    }
}

@Composable
fun PrivacyCard() {
    Column(
        Modifier.fillMaxWidth().clip(TtoRadius.lg).background(TtoColor.TealSoft).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PrivacyRow(R.drawable.icon_tick, "What we check", "Whether Blinkit, Zepto or another app you picked is on screen.")
        Box(Modifier.fillMaxWidth().height(1.dp).background(TtoColor.Line))
        PrivacyRow(R.drawable.icon_cross, "What we never do", "Read what you browse, buy or type. Your list stays on your phone unless you share it.")
        Box(Modifier.fillMaxWidth().height(1.dp).background(TtoColor.Line))
        PrivacyRow(R.drawable.icon_mic, "What leaves your phone", "Only if you share a list: it's stored online for the people who have your code. If you use the mic, your phone's speech service may process what you say.")
    }
}

@Composable
private fun PrivacyRow(icon: Int, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(painterResource(icon), null, Modifier.size(20.dp))
        Column {
            Txt(title.uppercase(), TtoType.Caption, color = TtoColor.InkMuted)
            Txt(body, TtoType.BodySmall.copy(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp), Modifier.padding(top = 4.dp))
        }
    }
}
