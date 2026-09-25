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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    Box(modifier.fillMaxWidth().height(1.dp).background(TtoColor.Row))

@Composable
fun MinusButton(onClick: () -> Unit, description: String = "Remove") {
    Box(
        Modifier.size(36.dp).tap(onClick = onClick).semantics { contentDescription = description; role = Role.Button },
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(Modifier.width(10.dp).height(1.6.dp).background(TtoColor.Ink))
    }
}

@Composable
fun ToggleSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Box(
        Modifier.width(40.dp).height(24.dp).clip(TtoRadius.pill)
            .background(if (checked) TtoColor.Ink else TtoColor.Line)
            .tap { onCheckedChange(!checked) }.padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) { Box(Modifier.size(18.dp).clip(CircleShape).background(Color.White)) }
}

@Composable
fun AddField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().border(1.5.dp, TtoColor.Ink, TtoRadius.md).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Txt(placeholder, TtoType.Body.copy(fontWeight = FontWeight.Normal), color = TtoColor.Faint)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TtoType.Body.copy(fontWeight = FontWeight.Normal, color = TtoColor.Ink),
                cursorBrush = SolidColor(TtoColor.Orange),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Image(painterResource(R.drawable.icon_plus), null, Modifier.height(14.dp).tap(onClick = onSubmit))
    }
}

@Composable
fun PrivacyCard() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(TtoColor.OffWhite).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PrivacyRow(R.drawable.icon_checkbox_ticked, "What we check", "Whether Blinkit, Zepto or another app you picked is on screen.")
        Box(Modifier.fillMaxWidth().height(1.dp).background(TtoColor.Line))
        PrivacyRow(R.drawable.icon_cross, "What we never do", "Read what you browse, buy or type. Nothing leaves your phone.")
    }
}

@Composable
private fun PrivacyRow(icon: Int, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(painterResource(icon), null, Modifier.padding(top = 3.dp).size(15.dp))
        Column {
            Txt(title.uppercase(), TtoType.Caption, color = TtoColor.GraySecondary)
            Txt(body, TtoType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp), Modifier.padding(top = 3.dp))
        }
    }
}
