package com.booster.shizuku

import android.content.Context

/** SharedPreferences-backed boost configuration. */
object Prefs {
    private const val NAME = "booster_prefs"

    const val KEY_TARGET_GAME = "target_game"
    const val KEY_KILL_FB = "kill_fb"
    const val KEY_KILL_MSG = "kill_msg"
    const val KEY_KILL_IG = "kill_ig"
    const val KEY_KILL_TT = "kill_tt"
    const val KEY_KILL_CHROME = "kill_chrome"
    const val KEY_DISABLE_ANIM = "disable_anim"
    const val KEY_DND = "dnd"
    const val KEY_BUBBLE = "bubble_enabled"
    const val KEY_BUBBLE_X = "bubble_x"
    const val KEY_BUBBLE_Y = "bubble_y"
    const val KEY_TOUCH_BOOST = "touch_boost"
    const val KEY_NET_STABLE = "net_stable"
    const val KEY_POCO_BEAST = "poco_beast"

    const val DEFAULT_GAME = "jp.konami.pesam"

    private fun prefs(context: Context) =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun targetGame(context: Context): String =
        prefs(context).getString(KEY_TARGET_GAME, DEFAULT_GAME)?.ifBlank { DEFAULT_GAME } ?: DEFAULT_GAME

    fun setTargetGame(context: Context, pkg: String) {
        prefs(context).edit().putString(KEY_TARGET_GAME, pkg.trim()).apply()
    }

    /** Returns enabled (package, label) pairs to force-stop on boost. */
    fun killTargets(context: Context): List<Pair<String, String>> {
        val p = prefs(context)
        val list = mutableListOf<Pair<String, String>>()
        if (p.getBoolean(KEY_KILL_FB, true)) list += "com.facebook.katana" to "Facebook"
        if (p.getBoolean(KEY_KILL_MSG, true)) list += "com.facebook.orca" to "Messenger"
        if (p.getBoolean(KEY_KILL_IG, true)) list += "com.instagram.android" to "Instagram"
        if (p.getBoolean(KEY_KILL_TT, true)) list += "com.zhiliaoapp.musically" to "TikTok"
        if (p.getBoolean(KEY_KILL_CHROME, true)) list += "com.android.chrome" to "Chrome"
        return list
    }

    fun disableAnim(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DISABLE_ANIM, true)

    fun dndEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DND, true)

    /** Touch-response tweaks: pointer speed max + faster long-press timeouts. */
    fun touchBoost(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TOUCH_BOOST, true)

    /** Ping stabilization: Cloudflare DNS, WiFi scan throttle off, data always on. */
    fun netStable(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NET_STABLE, true)

    /** POCO F5 Pro near-root tuning: 120Hz lock, Doze off, game driver, no throttle. */
    fun pocoBeast(context: Context): Boolean =
        prefs(context).getBoolean(KEY_POCO_BEAST, true)

    fun getBoolean(context: Context, key: String, def: Boolean): Boolean =
        prefs(context).getBoolean(key, def)

    fun setBoolean(context: Context, key: String, value: Boolean) {
        prefs(context).edit().putBoolean(key, value).apply()
    }

    fun getInt(context: Context, key: String, def: Int): Int =
        prefs(context).getInt(key, def)

    fun setInt(context: Context, key: String, value: Int) {
        prefs(context).edit().putInt(key, value).apply()
    }

    /** One-line summary shown on the main screen. */
    fun summaryLine(context: Context): String {
        val parts = mutableListOf<String>()
        val kills = killTargets(context).size
        parts += if (kills > 0) "Purge x$kills" else "No purge"
        parts += if (disableAnim(context)) "Anim 0.0x" else "Anim stock"
        parts += if (dndEnabled(context)) "DND" else "No DND"
        parts += if (touchBoost(context)) "Touch+" else "Touch off"
        parts += if (netStable(context)) "Ping+ • CF DNS" else "Ping off"
        parts += if (pocoBeast(context)) "F5P+" else "F5P off"
        return parts.joinToString(" • ")
    }
}
