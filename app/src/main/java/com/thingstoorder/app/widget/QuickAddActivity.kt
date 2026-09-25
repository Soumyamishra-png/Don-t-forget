package com.thingstoorder.app.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.thingstoorder.app.data.AppDatabase
import com.thingstoorder.app.data.Item
import com.thingstoorder.app.ui.components.AddField
import com.thingstoorder.app.ui.components.Txt
import com.thingstoorder.app.ui.components.tap
import com.thingstoorder.app.ui.theme.TtoColor
import com.thingstoorder.app.ui.theme.TtoType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QuickAddActivity : ComponentActivity() {
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val dao = AppDatabase.get(this).itemDao()

        setContent {
            var text by remember { mutableStateOf("") }
            var lastAdded by remember { mutableStateOf<String?>(null) }
            val focus = remember { FocusRequester() }

            androidx.compose.runtime.LaunchedEffect(Unit) { focus.requestFocus() }

            Box(Modifier.fillMaxSize().tap { finish() }) {
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding().navigationBarsPadding()
                        .padding(12.dp).clip(RoundedCornerShape(24.dp)).background(TtoColor.Paper)
                        .tap { }.padding(20.dp),
                ) {
                    Txt("Add something", TtoType.Headline.copy(fontSize = 22.sp, lineHeight = 26.sp))
                    AddField(
                        value = text,
                        onValueChange = { text = it },
                        onSubmit = {
                            val name = text.trim()
                            if (name.isNotEmpty()) {
                                writeScope.launch { dao.insert(Item(name = name)) }
                                lastAdded = name
                                text = ""
                            }
                        },
                        placeholder = "Type an item…",
                        modifier = Modifier.padding(top = 14.dp).focusRequester(focus),
                    )
                    Txt(
                        lastAdded?.let { "Added \"$it\". Add another, or tap outside to close." } ?: "Press enter to add. Tap outside to close.",
                        TtoType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal),
                        Modifier.padding(top = 10.dp),
                        color = TtoColor.GrayBody,
                    )
                }
            }
        }
    }
}
