package com.miaomiaopounce.ui

import android.graphics.Color
import com.miaomiaopounce.game.PreyTheme

object Palette {
    val cream = Color.rgb(246, 243, 235)
    val ink = Color.rgb(38, 65, 56)
    val muted = Color.rgb(116, 127, 115)
    val line = Color.rgb(225, 225, 210)
    val orange = Color.rgb(214, 107, 65)
    val white = Color.rgb(255, 253, 248)
    fun accent(theme: PreyTheme) = when (theme) {
        PreyTheme.BUG -> Color.rgb(139, 106, 39)
        PreyTheme.FISH -> Color.rgb(48, 114, 121)
        PreyTheme.MOUSE -> Color.rgb(175, 98, 79)
        PreyTheme.DOT -> Color.rgb(114, 91, 146)
    }
    fun habitat(theme: PreyTheme) = when (theme) {
        PreyTheme.BUG -> Color.rgb(235, 236, 215)
        PreyTheme.FISH -> Color.rgb(215, 234, 232)
        PreyTheme.MOUSE -> Color.rgb(237, 229, 214)
        PreyTheme.DOT -> Color.rgb(38, 60, 57)
    }
}
