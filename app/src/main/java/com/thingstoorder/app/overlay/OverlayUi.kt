@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.thingstoorder.app.overlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thingstoorder.app.R
import com.thingstoorder.app.data.Item
import com.thingstoorder.app.ui.components.FaceMood
import com.thingstoorder.app.ui.components.FaceTile
import com.thingstoorder.app.ui.components.Txt
import com.thingstoorder.app.ui.components.tap
import com.thingstoorder.app.ui.theme.TtoColor
import com.thingstoorder.app.ui.theme.TtoRadius
import com.thingstoorder.app.ui.theme.TtoType

private val FloatShadow = TtoColor.Ink.copy(alpha = 0.28f)

/** The quick-commerce pop-up (tag stack): a paper card floating 12dp in from the edges. */
@Composable
fun ExpandedOverlay(items: List<Item>, toast: String?, onChip: (Item) -> Unit, onMinimize: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (toast != null) {
            Box(
                Modifier.shadow(10.dp, TtoRadius.pill, ambientColor = TtoColor.Ink.copy(alpha = 0.16f), spotColor = TtoColor.Ink.copy(alpha = 0.16f))
                    .clip(TtoRadius.pill).background(TtoColor.Paper).padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Txt(toast, TtoType.Chip.copy(fontSize = 14.sp), color = TtoColor.Ink)
            }
        }
        Column(
            Modifier.fillMaxWidth().shadow(12.dp, TtoRadius.xl, ambientColor = FloatShadow, spotColor = FloatShadow)
                .background(TtoColor.Paper, TtoRadius.xl).padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FaceTile(FaceMood.Eager, 44.dp)
                Txt("tap to add", TtoType.ScreenTitle.copy(fontSize = 18.sp, lineHeight = 22.sp), Modifier.padding(start = 12.dp).weight(1f))
                MinimizeButton(onMinimize)
            }
            Column(Modifier.padding(top = 12.dp).heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        Box(
                            Modifier.height(40.dp).clip(TtoRadius.pill).background(TtoColor.Paper)
                                .border(2.dp, TtoColor.Ink, TtoRadius.pill)
                                .tap { onChip(item) }.padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) { Txt(item.name, TtoType.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimizeButton(onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).tap(onClick = onClick).semantics { contentDescription = "Minimize"; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.icon_chevron), null, Modifier.size(24.dp))
    }
}

@Composable
fun DoneOverlay() {
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 28.dp)) {
        Column(
            Modifier.fillMaxWidth().shadow(12.dp, TtoRadius.xl, ambientColor = FloatShadow, spotColor = FloatShadow)
                .background(TtoColor.Paper, TtoRadius.xl).padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FaceTile(FaceMood.Happy, 44.dp)
            Txt("Nothing left to add", TtoType.ScreenTitle.copy(fontSize = 20.sp, lineHeight = 24.sp), align = TextAlign.Center)
            Txt("Minimizing…", TtoType.BodySmall, color = TtoColor.InkMuted)
        }
    }
}

/** Minimised: an ink rounded pill with a small green face and the count left. */
@Composable
fun PillOverlay(count: Int) {
    Box(Modifier.padding(16.dp).semantics { contentDescription = "$count left to order. Tap to open." }) {
        Row(
            Modifier.shadow(12.dp, TtoRadius.lg, ambientColor = FloatShadow, spotColor = FloatShadow)
                .background(TtoColor.Ink, TtoRadius.lg).padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FaceTile(FaceMood.Happy, 40.dp)
            Txt("$count", TtoType.ScreenTitle.copy(fontSize = 20.sp, lineHeight = 24.sp), Modifier.padding(start = 12.dp), color = TtoColor.Paper)
        }
    }
}
