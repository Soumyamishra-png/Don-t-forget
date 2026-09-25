package com.thingstoorder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thingstoorder.app.data.AppDatabase
import com.thingstoorder.app.ui.theme.TtoColor
import com.thingstoorder.app.ui.theme.TtoSpacing
import com.thingstoorder.app.ui.theme.TtoType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val dao = AppDatabase.get(LocalContext.current).itemDao()
            val items by remember { dao.observeAll() }.collectAsStateWithLifecycle(emptyList())
            Column(
                Modifier.fillMaxSize().background(TtoColor.Paper).systemBarsPadding().padding(TtoSpacing.s24),
            ) {
                BasicText("Things to Order", style = TtoType.ScreenTitle.copy(color = TtoColor.Ink))
                BasicText("${items.size} items in the list", style = TtoType.Body.copy(color = TtoColor.GrayBody))
            }
        }
    }
}
