package com.miaomiaopounce.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import com.miaomiaopounce.R
import com.miaomiaopounce.game.PreyTheme

class GameAudio(context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    private val ids = mutableMapOf<PreyTheme, Int>()
    private val loaded = mutableSetOf<Int>()
    private var lastPlayed = -1000L
    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded += id }
        ids[PreyTheme.BUG] = pool.load(context, R.raw.bug, 1)
        ids[PreyTheme.FISH] = pool.load(context, R.raw.fish, 1)
        ids[PreyTheme.MOUSE] = pool.load(context, R.raw.mouse, 1)
        ids[PreyTheme.DOT] = pool.load(context, R.raw.dot, 1)
    }
    fun play(theme: PreyTheme, volume: Int) {
        val now = SystemClock.uptimeMillis()
        val id = ids[theme] ?: return
        if (now - lastPlayed < 90 || id !in loaded || volume <= 0) return
        val gain = volume.coerceIn(0, 60) / 100f
        pool.play(id, gain, gain, 1, 0, 1f); lastPlayed = now
    }
    fun pause() = pool.autoPause()
    fun release() = pool.release()
}
