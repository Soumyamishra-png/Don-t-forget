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
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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
import com.thingstoorder.app.data.SettingsRepository
import kotlinx.coroutines.flow.first

private val Ink = Color(0xFF171717)
private val Line = Color(0xFFEDEAE1)
private val Faint = Color(0xFFB7B2A6)
private val GrayBody = Color(0xFF6E6B64)
private val Orange = Color(0xFFFF5E1A)

private val ItemIdKey = ActionParameters.Key<Long>("item_id")

class DontForgetWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = AppDatabase.get(context).itemDao()
        val settings = SettingsRepository(context)
        val initialItems = dao.getAll()
        val initialMinimized = settings.widgetMinimized.first()
        val initialTakeover = settings.widgetTakeoverUntil.first()
        provideContent {
            val items by dao.observeAll().collectAsState(initialItems)
            val minimized by settings.widgetMinimized.collectAsState(initialMinimized)
            val takeoverUntil by settings.widgetTakeoverUntil.collectAsState(initialTakeover)
            WidgetContent(context, items, minimized, takeoverUntil)
        }
    }
}

class DontForgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DontForgetWidget()
}

class ToggleItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        parameters[ItemIdKey]?.let { AppDatabase.get(context).itemDao().toggleChecked(it) }
        DontForgetWidget().updateAll(context)
    }
}

class RemoveItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        parameters[ItemIdKey]?.let { AppDatabase.get(context).itemDao().deleteById(it) }
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

// Dismisses the full-color takeover and opens quick-add in one tap, same destination as the
// regular "+" (there's no dedicated "Typing" screen yet -- that's the Voice phase).
class DismissTakeoverAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        SettingsRepository(context).setWidgetTakeoverUntil(null)
        DontForgetWidget().updateAll(context)
        context.startActivity(
            Intent().setClassName("com.thingstoorder.app", QuickAddActivity::class.java.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

@Composable
private fun WidgetContent(context: Context, items: List<Item>, minimized: Boolean, takeoverUntil: Long?) {
    val now = System.currentTimeMillis()
    val state = WidgetMascotState.of(items, now)
    val copy = WidgetCopyBuilder.forState(state, items, now)
    val remaining = items.count { !it.checked }
    when {
        takeoverUntil != null && now < takeoverUntil -> Takeover(context, state, copy)
        minimized -> MinimizedPill(context, state, copy)
        else -> Expanded(context, items, remaining, state, copy)
    }
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

private fun stripDrawable(state: WidgetMascotState) = when (state) {
    WidgetMascotState.Empty -> R.drawable.widget_strip_empty
    WidgetMascotState.Eager -> R.drawable.widget_strip_eager
    WidgetMascotState.Stale -> R.drawable.widget_strip_stale
    WidgetMascotState.AllDone -> R.drawable.widget_strip_alldone
}

private fun takeoverDrawable(state: WidgetMascotState) = when (state) {
    WidgetMascotState.Empty -> R.drawable.widget_takeover_empty
    WidgetMascotState.Eager -> R.drawable.widget_takeover_eager
    WidgetMascotState.Stale -> R.drawable.widget_takeover_stale
    WidgetMascotState.AllDone -> R.drawable.widget_takeover_alldone
}

private fun pillDrawable(state: WidgetMascotState) = when (state) {
    WidgetMascotState.Empty -> R.drawable.widget_pill_empty
    WidgetMascotState.Eager -> R.drawable.widget_pill_eager
    WidgetMascotState.Stale -> R.drawable.widget_pill_stale
    WidgetMascotState.AllDone -> R.drawable.widget_pill_alldone
}

@Composable
private fun MascotStrip(state: WidgetMascotState) {
    val iconTint = if (state.faceIsLight) Color.White else Ink
    Row(
        GlanceModifier.fillMaxWidth().height(52.dp).background(ImageProvider(stripDrawable(state))).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(GlanceModifier.size(36.dp)) {}
        Box(GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
            Icon(state.face, 32, description = null)
        }
        Box(GlanceModifier.size(36.dp).clickable(actionRunCallback<AddFromStripAction>()), contentAlignment = Alignment.Center) {
            Icon(R.drawable.icon_plus, 18, tint = iconTint, description = "Add item")
        }
        Box(GlanceModifier.size(36.dp).clickable(actionRunCallback<ToggleMinimizeAction>()), contentAlignment = Alignment.Center) {
            Icon(R.drawable.icon_chevron, 18, tint = iconTint, description = "Minimize")
        }
    }
}

class AddFromStripAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.startActivity(
            Intent().setClassName("com.thingstoorder.app", QuickAddActivity::class.java.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

@Composable
private fun Expanded(context: Context, items: List<Item>, remaining: Int, state: WidgetMascotState, copy: WidgetCopy) {
    Column(GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_card_bg))) {
        MascotStrip(state)
        Column(GlanceModifier.fillMaxWidth().defaultWeight().padding(start = 18.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BitmapText(WidgetBitmaps.text(context, "Things to Order", 16f, 0xFF171717.toInt()), "Things to Order")
                if (remaining > 0) {
                    Spacer(GlanceModifier.width(8.dp))
                    BitmapText(WidgetBitmaps.badge(context, remaining), "$remaining to order")
                }
            }
            Text(
                copy.caption,
                modifier = GlanceModifier.padding(top = 2.dp),
                style = TextStyle(color = ColorProvider(GrayBody), fontSize = 12.sp),
                maxLines = 1,
            )
            if (items.isEmpty()) {
                EmptyBody()
            } else {
                Spacer(GlanceModifier.height(6.dp))
                LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
                    items(items, itemId = { it.id }) { ItemRow(it) }
                }
            }
        }
    }
}

@Composable
private fun ItemRow(item: Item) {
    Column(GlanceModifier.fillMaxWidth()) {
        Row(GlanceModifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                GlanceModifier.size(width = 34.dp, height = 38.dp)
                    .clickable(actionRunCallback<ToggleItemAction>(actionParametersOf(ItemIdKey to item.id))),
                contentAlignment = Alignment.CenterStart,
            ) { Icon(if (item.checked) R.drawable.icon_checkbox_ticked else R.drawable.icon_checkbox_empty, 18, description = if (item.checked) "Mark not done: ${item.name}" else "Mark done: ${item.name}") }
            Text(
                item.name,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = ColorProvider(if (item.checked) Faint else Ink),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                ),
                maxLines = 1,
            )
            Box(
                GlanceModifier.size(width = 40.dp, height = 38.dp)
                    .clickable(actionRunCallback<RemoveItemAction>(actionParametersOf(ItemIdKey to item.id))),
                contentAlignment = Alignment.CenterEnd,
            ) { Icon(R.drawable.icon_subtract, 14, description = "Remove ${item.name}") }
        }
        Box(GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(Line))) {}
    }
}

@Composable
private fun EmptyBody() {
    Column(GlanceModifier.fillMaxSize().clickable(actionRunCallback<AddFromStripAction>())) {
        Spacer(GlanceModifier.defaultWeight())
        Box(GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(Line))) {}
        Text(
            "+ Add something",
            modifier = GlanceModifier.padding(top = 10.dp),
            style = TextStyle(color = ColorProvider(Orange), fontSize = 15.sp, fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun MinimizedPill(context: Context, state: WidgetMascotState, copy: WidgetCopy) {
    val textColor = if (state.faceIsLight) 0xFFFFFFFF.toInt() else 0xFF171717.toInt()
    val iconTint = if (state.faceIsLight) Color.White else Ink
    Row(
        GlanceModifier.fillMaxWidth().height(52.dp).background(ImageProvider(pillDrawable(state)))
            .padding(start = 18.dp, end = 12.dp).clickable(actionRunCallback<ToggleMinimizeAction>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(state.face, 26, description = null)
        Spacer(GlanceModifier.width(8.dp))
        BitmapText(WidgetBitmaps.text(context, copy.pillLabel, 15f, textColor), copy.pillLabel)
        Spacer(GlanceModifier.defaultWeight())
        Icon(R.drawable.icon_chevron_up, 18, tint = iconTint, description = "Expand")
    }
}

@Composable
private fun Takeover(context: Context, state: WidgetMascotState, copy: WidgetCopy) {
    val textColor = if (state.faceIsLight) 0xFFFFFFFF.toInt() else 0xFF171717.toInt()
    Column(
        GlanceModifier.fillMaxSize().background(ImageProvider(takeoverDrawable(state))).clickable(actionRunCallback<DismissTakeoverAction>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(state.face, 40, description = null)
        Spacer(GlanceModifier.height(6.dp))
        BitmapText(WidgetBitmaps.text(context, copy.takeoverLine, 18f, textColor), copy.takeoverLine)
    }
}
