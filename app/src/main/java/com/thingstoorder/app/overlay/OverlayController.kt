package com.thingstoorder.app.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.thingstoorder.app.data.AppDatabase
import com.thingstoorder.app.data.Item
import com.thingstoorder.app.data.QcApp
import com.thingstoorder.app.data.SettingsRepository
import com.thingstoorder.app.watcher.WatcherState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.abs

object OverlayController {
    private const val TAG = "DFOverlay"
    private const val DONE_MS = 1400L
    private const val TOAST_MS = 1400L
    private const val TEST_MS = 5000L
    private const val BOTTOM_GAP_DP = 60

    private enum class Mode { Hidden, Expanded, Pill, Done }

    private lateinit var app: Context
    private lateinit var windowManager: WindowManager
    private lateinit var dao: com.thingstoorder.app.data.ItemDao
    private lateinit var settings: SettingsRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var view: View? = null
    private var owner: OverlayLifecycleOwner? = null
    private var mode = Mode.Hidden
    private var testing = false
    private var inSession = false
    private var userMinimized = false
    private var leftAt = 0L
    private var pillPosition: Pair<Int, Int>? = null
    private var toastJob: Job? = null
    private var doneJob: Job? = null

    private val items = mutableStateOf<List<Item>>(emptyList())
    private val toast = mutableStateOf<String?>(null)

    fun start(context: Context) {
        app = context.applicationContext
        windowManager = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        dao = AppDatabase.get(app).itemDao()
        settings = SettingsRepository(app)

        scope.launch { settings.pillPosition.collect { pillPosition = it } }
        scope.launch {
            combine(WatcherState.foregroundApp, dao.observeRemaining()) { fg, remaining -> fg to remaining }
                .collect { (fg, remaining) -> onState(fg, remaining) }
        }
    }

    fun showTest(): Boolean {
        if (!Settings.canDrawOverlays(app)) return false
        testing = true
        doneJob?.cancel()
        items.value = TEST_ITEMS
        show(Mode.Expanded)
        scope.launch {
            delay(TEST_MS)
            testing = false
            hide()
            onState(WatcherState.foregroundApp.value, latestRemaining)
        }
        return true
    }

    private var latestRemaining: List<Item> = emptyList()

    private fun onState(foreground: QcApp?, remaining: List<Item>) {
        latestRemaining = remaining
        Log.d(TAG, "state fg=${foreground?.label} remaining=${remaining.size} mode=$mode inSession=$inSession userMinimized=$userMinimized testing=$testing")
        if (testing) return

        if (foreground == null) {
            if (inSession) leftAt = SystemClock.elapsedRealtime()
            inSession = false
            hide()
            return
        }

        val wasInSession = inSession
        inSession = true
        items.value = remaining

        if (remaining.isEmpty()) {
            when (mode) {
                Mode.Expanded -> showDone()
                Mode.Pill -> { hide(); userMinimized = true }
                else -> Unit
            }
            return
        }
        if (mode == Mode.Done) return

        if (!wasInSession) {
            val stayMinimized = ReopenRule.stayMinimized(userMinimized, leftAt, SystemClock.elapsedRealtime())
            if (!stayMinimized) userMinimized = false
            show(if (stayMinimized) Mode.Pill else Mode.Expanded)
        } else if (mode == Mode.Hidden) {
            userMinimized = false
            show(Mode.Expanded)
        }
    }

    private fun showDone() {
        userMinimized = true
        show(Mode.Done)
        doneJob?.cancel()
        doneJob = scope.launch {
            delay(DONE_MS)
            hide()
        }
    }

    private fun onChipTap(item: Item) {
        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("item", item.name))
        showToast("Copied \"${item.name}\"")
        if (testing) {
            items.value = items.value.filter { it.id != item.id }
        } else {
            scope.launch(Dispatchers.IO) { dao.setChecked(item.id, true) }
        }
    }

    private fun showToast(text: String) {
        toast.value = text
        toastJob?.cancel()
        toastJob = scope.launch {
            delay(TOAST_MS)
            toast.value = null
        }
    }

    private fun minimize() {
        userMinimized = true
        show(Mode.Pill)
    }

    private fun expand() {
        userMinimized = false
        show(Mode.Expanded)
    }

    private fun show(target: Mode) {
        if (!Settings.canDrawOverlays(app)) {
            Log.w(TAG, "overlay permission missing")
            return
        }
        if (mode == target && view != null) return
        removeView()
        mode = target
        when (target) {
            Mode.Expanded -> addComposeView(bottomParams()) {
                ExpandedOverlay(items.value, toast.value, onChip = ::onChipTap, onMinimize = ::minimize)
            }
            Mode.Done -> addComposeView(bottomParams()) { DoneOverlay() }
            Mode.Pill -> addPill()
            Mode.Hidden -> Unit
        }
    }

    private fun hide() {
        doneJob?.cancel()
        toast.value = null
        removeView()
        mode = Mode.Hidden
    }

    private fun removeView() {
        view?.let { runCatching { windowManager.removeView(it) } }
        owner?.destroy()
        view = null
        owner = null
    }

    private fun addComposeView(params: WindowManager.LayoutParams, touch: View.OnTouchListener? = null, content: @androidx.compose.runtime.Composable () -> Unit) {
        val lifecycleOwner = OverlayLifecycleOwner().also { it.start() }
        val composeView = ComposeView(app).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent(content)
            if (touch != null) setOnTouchListener(touch)
        }
        try {
            windowManager.addView(composeView, params)
            view = composeView
            owner = lifecycleOwner
        } catch (e: Exception) {
            Log.w(TAG, "could not add overlay", e)
            lifecycleOwner.destroy()
            mode = Mode.Hidden
        }
    }

    private fun baseParams(width: Int) = WindowManager.LayoutParams(
        width,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT,
    )

    private fun dp(value: Int) = (value * app.resources.displayMetrics.density).toInt()

    private fun bottomParams() = baseParams(WindowManager.LayoutParams.MATCH_PARENT).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        y = dp(BOTTOM_GAP_DP)
    }

    private fun addPill() {
        val metrics = app.resources.displayMetrics
        val size = dp(84)
        val saved = pillPosition
        val params = baseParams(WindowManager.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (saved?.first ?: (metrics.widthPixels - size - dp(10))).coerceIn(0, metrics.widthPixels - size)
            y = (saved?.second ?: (metrics.heightPixels - size - dp(150))).coerceIn(0, metrics.heightPixels - size)
        }
        addComposeView(params, pillTouchListener(params)) { PillOverlay(items.value.size) }
    }

    private fun pillTouchListener(params: WindowManager.LayoutParams) = object : View.OnTouchListener {
        private val slop = ViewConfiguration.get(app).scaledTouchSlop
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        private var dragging = false

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val metrics = app.resources.displayMetrics
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = params.x; startY = params.y
                    dragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!dragging && (abs(dx) > slop || abs(dy) > slop)) dragging = true
                    if (dragging) {
                        params.x = (startX + dx.toInt()).coerceIn(0, metrics.widthPixels - v.width)
                        params.y = (startY + dy.toInt()).coerceIn(0, metrics.heightPixels - v.height)
                        windowManager.updateViewLayout(v, params)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        val (x, y) = params.x to params.y
                        pillPosition = x to y
                        scope.launch(Dispatchers.IO) { settings.setPillPosition(x, y) }
                    } else {
                        expand()
                    }
                }
            }
            return true
        }
    }

    private val TEST_ITEMS = listOf("Oat milk", "Bananas", "Coffee beans", "Dish soap").mapIndexed { i, name -> Item(id = -(i + 1L), name = name) }
}
