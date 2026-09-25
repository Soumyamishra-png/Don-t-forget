package com.thingstoorder.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thingstoorder.app.ui.theme.TtoColor
import com.thingstoorder.app.ui.theme.TtoRadius
import com.thingstoorder.app.ui.theme.TtoType

@Composable
fun Txt(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = TtoColor.Ink,
    align: TextAlign = TextAlign.Unspecified,
) = BasicText(text, modifier, style.copy(color = color, textAlign = align))

@Composable
fun Modifier.tap(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick,
    )

@Composable
fun ProgressDots(active: Int, total: Int = 10, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..total) {
            val isActive = i == active
            Box(
                Modifier
                    .width(if (isActive) 18.dp else 6.dp)
                    .height(6.dp)
                    .clip(TtoRadius.pill)
                    .background(if (i <= active) TtoColor.Ink else TtoColor.Line),
            )
        }
    }
}

@Composable
fun PrimaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(TtoRadius.md)
            .background(if (enabled) TtoColor.Ink else TtoColor.Line)
            .tap(enabled, onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Txt(label, TtoType.Button, color = Color.White)
    }
}

@Composable
fun NotNowButton(label: String = "Not now", onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().tap(onClick = onClick).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Txt(label, TtoType.Button.copy(fontSize = 14.sp))
    }
}

@Composable
fun StepFooter(
    primaryLabel: String,
    primaryEnabled: Boolean = true,
    onPrimary: () -> Unit,
    onNotNow: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryButton(primaryLabel, primaryEnabled, onPrimary)
        if (onNotNow != null) NotNowButton(onClick = onNotNow)
    }
}

@Composable
fun DashedDivider(color: Color = TtoColor.Line, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
        )
    }
}

@Composable
fun Chip(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(TtoRadius.pill)
            .background(TtoColor.OffWhite)
            .border(1.3.dp, TtoColor.Ink, TtoRadius.pill)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Txt(label, TtoType.Caption.copy(fontSize = 14.sp, letterSpacing = TextUnit.Unspecified))
    }
}
