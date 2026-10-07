package com.miaomiaopounce.data

import android.content.Context
import com.miaomiaopounce.game.*

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("pounce", Context.MODE_PRIVATE)
    fun load() = GameSettings(
        theme = enumValue(prefs.getString("theme", null), PreyTheme.BUG),
        count = prefs.getInt("count", 3),
        pace = enumValue(prefs.getString("pace", null), Pace.NORMAL),
        size = enumValue(prefs.getString("size", null), PreySize.MEDIUM),
        sound = prefs.getBoolean("sound", true), volume = prefs.getInt("volume", 25),
        minutes = prefs.getInt("minutes", 3), hiding = prefs.getBoolean("hiding", true),
        screenPinning = prefs.getBoolean("pin", false)
    ).sanitized()
    fun save(settings: GameSettings) {
        val s = settings.sanitized()
        prefs.edit().putString("theme", s.theme.name).putInt("count", s.count)
            .putString("pace", s.pace.name).putString("size", s.size.name)
            .putBoolean("sound", s.sound).putInt("volume", s.volume).putInt("minutes", s.minutes)
            .putBoolean("hiding", s.hiding).putBoolean("pin", s.screenPinning).apply()
    }
    fun record(result: SessionResult) {
        prefs.edit().putInt("totalCaptures", totalCaptures + result.captures)
            .putInt("sessions", sessions + 1).putInt("lastCaptures", result.captures)
            .putString("lastTheme", result.theme.label).apply()
    }
    val totalCaptures get() = prefs.getInt("totalCaptures", 0)
    val sessions get() = prefs.getInt("sessions", 0)
    val lastCaptures get() = prefs.getInt("lastCaptures", 0)
    val lastTheme get() = prefs.getString("lastTheme", "") ?: ""
    private inline fun <reified T : Enum<T>> enumValue(value: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback
}
