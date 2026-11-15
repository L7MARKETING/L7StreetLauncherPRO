package com.l7.streetlauncher

import android.content.Context

/** Preferências do launcher, salvas localmente no aparelho. */
class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("l7_launcher_prefs", Context.MODE_PRIVATE)

    var favorites: List<String>
        get() = sp.getString(KEY_FAVORITES, "").orEmpty()
            .split('\n')
            .filter { it.isNotBlank() }
        set(value) = sp.edit().putString(KEY_FAVORITES, value.joinToString("\n")).apply()

    var wallpaperMode: Int
        get() = sp.getInt(KEY_WALL_MODE, WALL_NEON)
        set(value) = sp.edit().putInt(KEY_WALL_MODE, value).apply()

    var neonColorIndex: Int
        get() = sp.getInt(KEY_COLOR, 0)
        set(value) = sp.edit().putInt(KEY_COLOR, value).apply()

    var use24h: Boolean
        get() = sp.getBoolean(KEY_24H, true)
        set(value) = sp.edit().putBoolean(KEY_24H, value).apply()

    var showSeconds: Boolean
        get() = sp.getBoolean(KEY_SECONDS, false)
        set(value) = sp.edit().putBoolean(KEY_SECONDS, value).apply()

    var neonIconFrame: Boolean
        get() = sp.getBoolean(KEY_NEON_FRAME, true)
        set(value) = sp.edit().putBoolean(KEY_NEON_FRAME, value).apply()

    var showLabels: Boolean
        get() = sp.getBoolean(KEY_LABELS, true)
        set(value) = sp.edit().putBoolean(KEY_LABELS, value).apply()

    /** 0 = automático (conforme a largura da tela). */
    var gridColumns: Int
        get() = sp.getInt(KEY_COLUMNS, 0)
        set(value) = sp.edit().putInt(KEY_COLUMNS, value).apply()

    companion object {
        const val MAX_FAVORITES = 5

        const val WALL_NEON = 0
        const val WALL_CUSTOM = 1
        const val WALL_SYSTEM = 2

        private const val KEY_FAVORITES = "favorites"
        private const val KEY_WALL_MODE = "wallpaper_mode"
        private const val KEY_COLOR = "neon_color"
        private const val KEY_24H = "clock_24h"
        private const val KEY_SECONDS = "clock_seconds"
        private const val KEY_NEON_FRAME = "neon_icon_frame"
        private const val KEY_LABELS = "show_labels"
        private const val KEY_COLUMNS = "grid_columns"
    }
}
