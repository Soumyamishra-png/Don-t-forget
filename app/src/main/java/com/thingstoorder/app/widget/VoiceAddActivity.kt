package com.thingstoorder.app.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.thingstoorder.app.data.AppDatabase
import com.thingstoorder.app.voice.VoiceSheet
import com.thingstoorder.app.voice.VoiceState
import com.thingstoorder.app.voice.addParsed
import com.thingstoorder.app.voice.rememberVoiceController
import kotlinx.coroutines.launch

/** Transparent screen opened from the widget: listens, shows the review sheet, adds the items, closes. */
class VoiceAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val dao = AppDatabase.get(this).itemDao()

        setContent {
            val voice = rememberVoiceController()
            var started by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (!started) { started = true; voice.begin() }
            }
            // Closing the sheet (Close, tapping outside) ends the screen too.
            LaunchedEffect(voice.capture.state, started) {
                if (started && voice.capture.state is VoiceState.Idle) finish()
            }

            VoiceSheet(voice, onAdd = { parsed ->
                lifecycleScope.launch {
                    dao.addParsed(parsed)
                    DontForgetWidget().updateAll(applicationContext)
                    finish()
                }
            })
        }
    }
}
