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

/** Equal-width bars: finished steps ink, the current step green, the rest line. */
@Composable
fun ProgressDots(active: Int, total: Int = 10, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..total) {
            Box(
                Modifier.weight(1f).height(4.dp).clip(TtoRadius.pill)
                    .background(
                        when {
                            i < active -> TtoColor.Ink
                            i == active -> TtoColor.Green
                            else -> TtoColor.Line
                        },
                    ),
            )
        }
    }
}

/** One per screen, full width, 52dp. Disabled until the screen has an answer. */
@Composable
fun PrimaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(TtoRadius.md)
            .background(if (enabled) TtoColor.Ink else TtoColor.Line)
            .tap(enabled, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Txt(label, TtoType.Button, color = if (enabled) TtoColor.Paper else TtoColor.InkSubtle)
    }
}

@Composable
fun NotNowButton(label: String = "Not now", onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(44.dp).tap(onClick = onClick), contentAlignment = Alignment.Center) {
        Txt(label, TtoType.Button.copy(fontSize = 14.sp), color = TtoColor.InkMuted)
    }
}

@Composable
fun StepFooter(
    primaryLabel: String,
    primaryEnabled: Boolean = true,
    onPrimary: () -> Unit,
    onNotNow: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

/** Straight outlined pill, 2dp ink, as used for tag chips. */
@Composable
fun Chip(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(TtoRadius.pill)
            .background(TtoColor.Paper)
            .border(2.dp, TtoColor.Ink, TtoRadius.pill)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Txt(label, TtoType.Chip.copy(fontSize = 14.sp))
    }
}
