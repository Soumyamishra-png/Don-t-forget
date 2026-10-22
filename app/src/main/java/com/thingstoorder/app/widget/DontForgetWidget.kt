package com.thingstoorder.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.thingstoorder.app.R
import com.thingstoorder.app.data.AppDatabase
import com.thingstoorder.app.data.Item
import com.thingstoorder.app.data.ItemStore
import com.thingstoorder.app.data.SettingsRepository
import com.thingstoorder.app.data.StaleSort
import com.thingstoorder.app.data.StaleTier
import com.thingstoorder.app.data.shared.SharedLists
import kotlinx.coroutines.flow.first

// Mint Line colours, as used on the widget surfaces.
private val Ink = Color(0xFF14293A)
private val InkMuted = Color(0xFF4A5F6B)
private val Paper = Color(0xFFFFFFFF)
private val Mint = Color(0xFFBDF2D5)

private val ItemIdKey = ActionParameters.Key<Long>("item_id")

class DontForgetWidget : GlanceAppWidget() {
    // Rows adapt to the card's height: 3 on the default card, up to 5 when it is made taller.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = AppDatabase.get(context).itemDao()
        val settings = SettingsRepository(context)
        val initialItems = dao.getAll()
        val initialMinimized = settings.widgetMinimized.first()
        val initialTakeover = settings.widgetTakeoverUntil.first()
        val initialListening = settings.widgetListeningUntil.first()
        provideContent {
            val items by dao.observeAll().collectAsState(initialItems)
            val minimized by settings.widgetMinimized.collectAsState(initialMinimized)
            val takeoverUntil by settings.widgetTakeoverUntil.collectAsState(initialTakeover)
            val listeningUntil by settings.widgetListeningUntil.collectAsState(initialListening)
            val members by SharedLists.get(context).current.collectAsState()
            val dots = members?.members?.associate { it.uid to it.color } ?: emptyMap()
            WidgetContent(context, items, minimized, takeoverUntil, listeningUntil, dots)
        }
    }
}

class DontForgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DontForgetWidget()
}

class ToggleItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        parameters[ItemIdKey]?.let { ItemStore(context).toggle(it) }
        DontForgetWidget().updateAll(context)
    }
}

class RemoveItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        parameters[ItemIdKey]?.let { ItemStore(context).remove(it) }
        DontForgetWidget().updateAll(context)
    }
}

class ToggleMinimizeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val settings = SettingsRepository(context)
        settings.setWidgetMinimized(!settings.widgetMinimized.first())
        DontForgetWidget().updateAll(context)
    }
}

// Dismisses the full-colour takeover and opens the voice sheet in one tap.
class DismissTakeoverAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        SettingsRepository(context).setWidgetTakeoverUntil(null)
        DontForgetWidget().updateAll(context)
        openVoice(context)
    }
}

private fun openVoice(context: Context) {
    context.startActivity(
        Intent().setClassName("com.thingstoorder.app", VoiceAddActivity::class.java.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

class VoiceFromWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) = openVoice(context)
}

@Composable
private fun WidgetContent(
    context: Context, items: List<Item>, minimized: Boolean, takeoverUntil: Long?, listeningUntil: Long?, dots: Map<String, Int>,
) {
    val now = System.currentTimeMillis()
    val listening = listeningUntil != null && now < listeningUntil
    val state = WidgetMascotState.of(items, now, listening)
    val copy = WidgetCopyBuilder.forState(state, items, now)
    val remaining = items.count { !it.checked }
    when {
        takeoverUntil != null && now < takeoverUntil -> Takeover(context, state, copy)
        minimized -> MinimizedPill(context, state, copy)
        else -> Expanded(context, items, remaining, state, copy, dots, now)
    }
}

private fun textOn(state: WidgetMascotState) = if (state.onInk) Paper else Ink
private fun moodOn(state: WidgetMascotState) = when (state) {
    WidgetMascotState.AllDone -> Mint
    WidgetMascotState.Eager, WidgetMascotState.Stale -> Ink
    else -> InkMuted
}

@Composable
private fun Icon(res: Int, sizeDp: Int, tint: Color? = null, description: String? = null) =
    Image(
        provider = ImageProvider(res),
        contentDescription = description,
        modifier = GlanceModifier.size(sizeDp.dp),
        contentScale = ContentScale.Fit,
        colorFilter = tint?.let { ColorFilter.tint(ColorProvider(it)) },
    )

@Composable
private fun BitmapText(bitmap: SizedBitmap, description: String) =
    Image(
        provider = ImageProvider(bitmap.bitmap),
        contentDescription = description,
        modifier = GlanceModifier.width(bitmap.widthDp.dp).height(bitmap.heightDp.dp),
        contentScale = ContentScale.Fit,
    )

private val DOT_DRAWABLES = listOf(R.drawable.dot_member_0, R.drawable.dot_member_1, R.drawable.dot_member_2, R.drawable.dot_member_3)

private const val HEAD_DP = 56
private const val ROW_DP = 40
private const val FOOTER_DP = 48

@Composable
private fun Expanded(
    context: Context, items: List<Item>, remaining: Int, state: WidgetMascotState, copy: WidgetCopy,
    dots: Map<String, Int>, now: Long,
) {
    val height = LocalSize.current.height.value.toInt()
    // 16dp padding top and bottom, header, two 8dp gaps and the footer come off the card's height.
    val rowsFit = ((height + 40 - 32 - HEAD_DP - 16 - FOOTER_DP) / ROW_DP).coerceIn(1, 5)
    val arranged = StaleSort.arrange(items, now)
    val shown = arranged.take(rowsFit)
    val tint = textOn(state)

    Column(GlanceModifier.fillMaxSize().background(ImageProvider(state.card)).padding(16.dp)) {
        Row(GlanceModifier.fillMaxWidth().height(HEAD_DP.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
                BitmapText(
                    WidgetBitmaps.text(context, "Things to Order", 20f, if (state.onInk) 0xFFFFFFFF.toInt() else 0xFF14293A.toInt()),
                    "Things to Order",
                )
                Spacer(GlanceModifier.height(4.dp))
                if (state == WidgetMascotState.Eager && remaining > 0) CountPill(remaining) else MoodLine(copy.caption, state)
            }
            Box(GlanceModifier.size(40.dp).clickable(actionRunCallback<ToggleMinimizeAction>()), contentAlignment = Alignment.Center) {
                Icon(R.drawable.icon_chevron, 20, tint = tint, description = "Minimise")
            }
            Image(
                provider = ImageProvider(state.tile),
                contentDescription = null,
                modifier = GlanceModifier.size(HEAD_DP.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        Column(GlanceModifier.fillMaxWidth().defaultWeight()) {
            if (shown.isEmpty()) {
                Spacer(GlanceModifier.defaultWeight())
            } else {
                shown.forEach { ItemRow(it, state, dots[it.addedBy], showAge = state == WidgetMascotState.Stale, now = now) }
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        when {
            state == WidgetMascotState.Listening -> ListeningFooter()
            items.isEmpty() -> AddFooter()
            else -> MicFooter()
        }
    }
}

@Composable
private fun MoodLine(text: String, state: WidgetMascotState) {
    Text(text, style = TextStyle(color = ColorProvider(moodOn(state)), fontSize = 14.sp), maxLines = 1)
}

/** "3 to order": a paper pill with an ink badge, shown on the eager (green) state. */
@Composable
private fun CountPill(count: Int) {
    Row(
        GlanceModifier.background(ImageProvider(R.drawable.widget_count_pill)).padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(GlanceModifier.size(20.dp).background(ImageProvider(R.drawable.widget_badge_ink)), contentAlignment = Alignment.Center) {
            Text("$count", style = TextStyle(color = ColorProvider(Paper), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
        }
        Spacer(GlanceModifier.width(8.dp))
        Text("to order", style = TextStyle(color = ColorProvider(Ink), fontSize = 13.sp, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun MicFooter() {
    Row(
        GlanceModifier.fillMaxWidth().height(FOOTER_DP.dp).background(ImageProvider(R.drawable.widget_btn_onpaper))
            .clickable(actionRunCallback<VoiceFromWidgetAction>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(R.drawable.icon_mic, 20, description = null)
        Spacer(GlanceModifier.width(8.dp))
        Text("Tap to say it", style = TextStyle(color = ColorProvider(Ink), fontSize = 15.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

@Composable
private fun ListeningFooter() {
    Row(
        GlanceModifier.fillMaxWidth().height(FOOTER_DP.dp).background(ImageProvider(R.drawable.widget_btn_onpaper)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(R.drawable.icon_mic, 20, description = null)
        Spacer(GlanceModifier.width(8.dp))
        Text("Listening…", style = TextStyle(color = ColorProvider(InkMuted), fontSize = 15.sp, fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

@Composable
private fun AddFooter() {
    Row(
        GlanceModifier.fillMaxWidth().height(FOOTER_DP.dp).background(ImageProvider(R.drawable.widget_add_dashed)).padding(start = 16.dp)
            .clickable(actionRunCallback<VoiceFromWidgetAction>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(R.drawable.icon_plus, 20, description = null)
        Spacer(GlanceModifier.width(8.dp))
        Text("Add or say an item", style = TextStyle(color = ColorProvider(Ink), fontSize = 15.sp, fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

@Composable
private fun ItemRow(item: Item, state: WidgetMascotState, dot: Int?, showAge: Boolean, now: Long) {
    val nameColor = when {
        item.checked && state.onInk -> Mint
        item.checked -> InkMuted
        state.onInk -> Paper
        else -> Ink
    }
    val tier = if (showAge && !item.checked) StaleSort.tierOf(item, now) else null
    Row(GlanceModifier.fillMaxWidth().height(ROW_DP.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            GlanceModifier.size(width = 40.dp, height = ROW_DP.dp)
                .clickable(actionRunCallback<ToggleItemAction>(actionParametersOf(ItemIdKey to item.id))),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                if (item.checked) R.drawable.widget_check_on else R.drawable.widget_check_off, 20,
                tint = if (!item.checked && state.onInk) Paper else null,
                description = if (item.checked) "Mark not done: ${item.name}" else "Mark done: ${item.name}",
            )
        }
        if (dot != null) {
            Image(provider = ImageProvider(DOT_DRAWABLES[dot.coerceIn(0, 3)]), contentDescription = null, modifier = GlanceModifier.size(8.dp))
            Spacer(GlanceModifier.width(8.dp))
        }
        Text(
            item.name,
            style = TextStyle(
                color = ColorProvider(nameColor),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
            ),
            maxLines = 1,
        )
        if (item.quantity != null) {
            Spacer(GlanceModifier.width(8.dp))
            TagOnColor(item.quantity)
        }
        if (tier != null) {
            Spacer(GlanceModifier.width(8.dp))
            TagOnColor(if (tier == StaleTier.Day) "since yesterday" else tier.label)
        }
        Spacer(GlanceModifier.defaultWeight())
        if (!item.checked) {
            Box(
                GlanceModifier.size(width = 40.dp, height = ROW_DP.dp)
                    .clickable(actionRunCallback<RemoveItemAction>(actionParametersOf(ItemIdKey to item.id))),
                contentAlignment = Alignment.CenterEnd,
            ) { Icon(R.drawable.icon_minus, 18, tint = if (state.onInk) Paper else null, description = "Remove ${item.name}") }
        }
    }
}

/** Quantity and age tags on a coloured surface: paper fill, ink text. */
@Composable
private fun TagOnColor(text: String) {
    Box(GlanceModifier.background(ImageProvider(R.drawable.widget_tag_paper)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(text, style = TextStyle(color = ColorProvider(Ink), fontSize = 11.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

@Composable
private fun MinimizedPill(context: Context, state: WidgetMascotState, copy: WidgetCopy) {
    val textColor = if (state.onInk) 0xFFFFFFFF.toInt() else 0xFF14293A.toInt()
    Row(
        GlanceModifier.fillMaxWidth().height(60.dp).background(ImageProvider(state.pill))
            .padding(start = 8.dp, end = 12.dp).clickable(actionRunCallback<ToggleMinimizeAction>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(provider = ImageProvider(state.tile), contentDescription = null, modifier = GlanceModifier.size(44.dp), contentScale = ContentScale.Fit)
        Spacer(GlanceModifier.width(12.dp))
        BitmapText(WidgetBitmaps.text(context, copy.pillLabel, 16f, textColor), copy.pillLabel)
        Spacer(GlanceModifier.defaultWeight())
        Icon(R.drawable.icon_chevron_up, 20, tint = textOn(state), description = "Expand")
    }
}

@Composable
private fun Takeover(context: Context, state: WidgetMascotState, copy: WidgetCopy) {
    val textColor = if (state.onInk) 0xFFFFFFFF.toInt() else 0xFF14293A.toInt()
    Column(
        GlanceModifier.fillMaxSize().background(ImageProvider(state.card)).clickable(actionRunCallback<DismissTakeoverAction>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(provider = ImageProvider(state.tile), contentDescription = null, modifier = GlanceModifier.size(96.dp), contentScale = ContentScale.Fit)
        Spacer(GlanceModifier.height(12.dp))
        BitmapText(WidgetBitmaps.text(context, copy.takeoverLine, 22f, textColor), copy.takeoverLine)
    }
}
