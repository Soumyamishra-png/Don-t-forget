package com.thingstoorder.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thingstoorder.app.R
import com.thingstoorder.app.ui.theme.TtoColor
import com.thingstoorder.app.ui.theme.TtoRadius
import com.thingstoorder.app.ui.theme.TtoType

/** The mascot's moods. The face is decorative: the mood is always also in words. */
enum class FaceMood(val drawable: Int) {
    Neutral(R.drawable.face_tile_neutral),
    Listening(R.drawable.face_tile_listening),
    Eager(R.drawable.face_tile_eager),
    Pleading(R.drawable.face_tile_pleading),
    Happy(R.drawable.face_tile_happy),
    Confused(R.drawable.face_tile_confused),
}

@Composable
fun FaceTile(mood: FaceMood, size: Dp = 64.dp, modifier: Modifier = Modifier) {
    Image(painterResource(mood.drawable), null, modifier.size(size))
}

/** Round checkbox: ink ring at rest, green fill with an ink tick when done. */
@Composable
fun RoundCheck(checked: Boolean, size: Dp = 22.dp, ring: Color = TtoColor.Ink, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(CircleShape).then(if (checked) Modifier.background(TtoColor.Green) else Modifier.border(2.dp, ring, CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Image(painterResource(R.drawable.icon_tick_check), null, Modifier.size(size * 0.55f))
    }
}

/** Straight pill for quantities and stale ages. Never rotated. */
@Composable
fun TagPill(text: String, background: Color = TtoColor.TealSoft, textColor: Color = TtoColor.Ink, modifier: Modifier = Modifier) {
    Box(modifier.clip(TtoRadius.pill).background(background).padding(horizontal = 9.dp, vertical = 3.dp)) {
        Txt(text, TtoType.Tag, color = textColor)
    }
}
