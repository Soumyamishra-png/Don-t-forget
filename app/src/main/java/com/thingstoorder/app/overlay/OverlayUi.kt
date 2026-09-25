@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.thingstoorder.app.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thingstoorder.app.R
import com.thingstoorder.app.data.Item
import com.thingstoorder.app.ui.components.Txt
import com.thingstoorder.app.ui.components.tap
import com.thingstoorder.app.ui.theme.TtoColor
import com.thingstoorder.app.ui.theme.TtoRadius
import com.thingstoorder.app.ui.theme.TtoType

private val CardShape = RoundedCornerShape(24.dp)

@Composable
fun ExpandedOverlay(items: List<Item>, toast: String?, onChip: (Item) -> Unit, onMinimize: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (toast != null) {
            Box(Modifier.clip(TtoRadius.pill).background(TtoColor.Ink).padding(horizontal = 20.dp, vertical = 12.dp)) {
                Txt(toast, TtoType.Body.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = Color.White)
            }
        }
        Column(Modifier.fillMaxWidth().shadow(10.dp, CardShape).background(TtoColor.Paper, CardShape).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt("tap to add", TtoType.Headline.copy(fontSize = 24.sp, lineHeight = 28.sp), Modifier.weight(1f))
                MinimizeButton(onMinimize)
            }
            Column(Modifier.padding(top = 16.dp).heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEach { item ->
                        Box(
                            Modifier.clip(TtoRadius.pill).background(TtoColor.OffWhite)
                                .border(1.4.dp, TtoColor.Ink, TtoRadius.pill)
                                .tap { onChip(item) }.padding(horizontal = 20.dp, vertical = 11.dp),
                        ) { Txt(item.name, TtoType.Body.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimizeButton(onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(TtoColor.OffWhite).border(1.dp, TtoColor.Line, CircleShape).tap(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(16.dp)) {
            val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(TtoColor.Ink, Offset(size.width * 0.12f, size.height * 0.3f), Offset(size.width / 2, size.height * 0.7f), stroke.width, StrokeCap.Round)
            drawLine(TtoColor.Ink, Offset(size.width / 2, size.height * 0.7f), Offset(size.width * 0.88f, size.height * 0.3f), stroke.width, StrokeCap.Round)
        }
    }
}

@Composable
fun DoneOverlay() {
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 28.dp)) {
        Column(
            Modifier.fillMaxWidth().shadow(10.dp, CardShape).background(TtoColor.Paper, CardShape).padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(painterResource(R.drawable.mark_confetti), null, Modifier.height(44.dp))
            Txt("Nothing left to add", TtoType.Headline.copy(fontSize = 24.sp, lineHeight = 28.sp), align = TextAlign.Center)
            Txt("Minimizing…", TtoType.Body.copy(fontSize = 17.sp, fontWeight = FontWeight.Normal), color = TtoColor.GrayBody)
        }
    }
}

@Composable
fun PillOverlay(count: Int) {
    Box(Modifier.padding(14.dp)) {
        Box(Modifier.size(56.dp).shadow(6.dp, CircleShape).background(TtoColor.Ink, CircleShape), contentAlignment = Alignment.Center) {
            Txt("$count", TtoType.Headline.copy(fontSize = 24.sp, lineHeight = 28.sp), color = TtoColor.Orange)
        }
    }
}
