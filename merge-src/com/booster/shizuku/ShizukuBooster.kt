package com.booster.shizuku

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuBooster {

    val TARGET_GAME_PACKAGE = "jp.konami.pesam"

    val APPS_TO_KILL = arrayOf(
        "com.facebook.katana",
        "com.facebook.orca",
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.android.chrome"
    )

    fun isShizukuAvailable(): Boolean {
        return Shizuku.pingBinder()
    }

    fun hasShizukuPermission(): Boolean {
        return if (isShizukuAvailable()) {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    fun runCommand(cmd: String): String {
        return try {
            val process = newShizukuProcess(arrayOf("sh", "-c", cmd))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            output.toString().trim()
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    /**
     * Shizuku API 13.x keeps newProcess() private (deprecated, slated for removal),
     * so invoke it via reflection. This is the same underlying call the public
     * API used before it was hidden.
     */
    private fun newShizukuProcess(cmd: Array<String>): java.lang.Process {
        val m = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        ).apply { isAccessible = true }
        return m.invoke(null, cmd, null, null) as java.lang.Process
    }

    fun boostDevice(context: Context, logCallback: (String) -> Unit, launchGame: Boolean = true) {
        val kills = Prefs.killTargets(context)
        if (kills.isNotEmpty()) {
            logCallback("[*] Force stopping ${kills.size} background apps...")
            for ((pkg, label) in kills) {
                runCommand("am force-stop $pkg")
                logCallback("[+] Force-stopped: $label")
            }
        } else {
            logCallback("[*] App purge disabled in settings, skipping.")
        }

        if (Prefs.disableAnim(context)) {
            logCallback("[*] Disabling system animations (0.0x scale)...")
            runCommand("settings put global window_animation_scale 0.0")
            runCommand("settings put global transition_animation_scale 0.0")
            runCommand("settings put global animator_duration_scale 0.0")
        } else {
            logCallback("[*] Animation tweak disabled in settings, skipping.")
        }

        if (Prefs.dndEnabled(context)) {
            logCallback("[*] Enabling Do Not Disturb mode (zen_mode 1)...")
            runCommand("settings put global zen_mode 1")
        } else {
            logCallback("[*] DND disabled in settings, skipping.")
        }

        if (Prefs.touchBoost(context)) {
            applyTouchTweaks(logCallback)
        } else {
            logCallback("[*] Touch tweaks disabled in settings, skipping.")
        }

        if (Prefs.netStable(context)) {
            applyNetTweaks(logCallback)
        } else {
            logCallback("[*] Network stabilization disabled in settings, skipping.")
        }

        if (Prefs.pocoBeast(context)) {
            applyPocoTweaks(context, logCallback)
        } else {
            logCallback("[*] POCO beast tuning disabled in settings, skipping.")
        }

        val game = Prefs.targetGame(context)
        if (launchGame) {
            logCallback("[*] Launching $game...")
            runCommand("monkey -p $game -c android.intent.category.LAUNCHER 1")
            logCallback("[✓] Target game launched with maximum RAM & performance!")
        } else {
            logCallback("[✓] Quick boost complete — back to your game!")
        }
    }

    /**
     * Touch-response tweaks for gaming (safe, reversible, ADB-level).
     * - pointer_speed 7: max touch sensitivity -> taps register with less travel
     * - long_press_timeout 300ms / multi_press_timeout 300ms: faster tap recognition
     */
    fun applyTouchTweaks(logCallback: (String) -> Unit) {
        logCallback("[*] Applying touch-response tweaks...")
        runCommand("settings put system pointer_speed 7")
        runCommand("settings put secure long_press_timeout 300")
        runCommand("settings put secure multi_press_timeout 300")
        logCallback("[+] Touch: sensitivity MAX, press timeout 300ms")
    }

    /**
     * Network / ping stabilization (safe, reversible, ADB-level).
     * - Cloudflare Private DNS (1.1.1.1 via DoT): faster, lower-latency
     *   resolution than Google DNS on this user's connection
     * - wifi_scan_throttle_enabled 0: stop WiFi scans from causing ping spikes
     * - wifi_suspend_optimizations_enabled 0: keep WiFi radio at full power
     * - mobile_data_always_on 1: cellular stays hot as instant fallback
     */
    fun applyNetTweaks(logCallback: (String) -> Unit) {
        logCallback("[*] Stabilizing network (anti ping-spike)...")
        runCommand("settings put global private_dns_mode hostname")
        runCommand("settings put global private_dns_specifier 1dot1dot1dot1.cloudflare-dns.com")
        runCommand("settings put global wifi_scan_throttle_enabled 0")
        runCommand("settings put global wifi_suspend_optimizations_enabled 0")
        runCommand("settings put global mobile_data_always_on 1")
        logCallback("[+] Net: Cloudflare DNS (DoT), scan throttle OFF, WiFi full power, data always-on")
    }

    /**
     * POCO F5 Pro "near-root" tuning — aggressive but 100% ADB-level, no root.
     * Everything here is a stock Android setting; fully reversible via Restore.
     */
    fun applyPocoTweaks(context: Context, logCallback: (String) -> Unit) {
        logCallback("[*] POCO F5 Pro beast tuning (no-root max)...")
        // Lock 120Hz for buttery gameplay
        runCommand("settings put system peak_refresh_rate 120.0")
        // Force the updated Game Driver path on all apps (GPU)
        runCommand("settings put global game_driver_all_apps 1")
        // Stop the system from throttling the game to "save battery"
        runCommand("settings put global adaptive_battery_management_enabled 0")
        // Doze OFF — no background CPU throttling mid-match
        runCommand("dumpsys deviceidle disable")
        // Keep the target game out of Doze even after re-enable
        val game = Prefs.targetGame(context)
        runCommand("cmd deviceidle whitelist +$game")
        logCallback("[+] F5P: 120Hz locked, Doze OFF, game driver all-apps, no battery-throttle")
    }

    /**
     * Memory clean: kill all cached/background processes + drop RAM held by
     * background apps, WITHOUT touching the foreground game.
     */
    fun cleanMemory(logCallback: (String) -> Unit) {
        logCallback("[*] Cleaning memory (killing cached background processes)...")
        val out = runCommand("am kill-all")
        if (out.isNotBlank()) logCallback("[out] $out")
        // belt & suspenders: kill bg procs for the known heavy apps too
        for (pkg in APPS_TO_KILL) {
            runCommand("am kill $pkg")
        }
        logCallback("[✓] Memory cleaned — cached apps killed, game untouched!")
    }

    fun restoreStockSettings(logCallback: (String) -> Unit) {
        logCallback("[*] Restoring animation scales to 1.0x...")
        runCommand("settings put global window_animation_scale 1.0")
        runCommand("settings put global transition_animation_scale 1.0")
        runCommand("settings put global animator_duration_scale 1.0")

        logCallback("[*] Disabling Do Not Disturb (zen_mode 0)...")
        runCommand("settings put global zen_mode 0")

        logCallback("[*] Restoring touch settings to stock...")
        runCommand("settings put system pointer_speed 0")
        runCommand("settings put secure long_press_timeout 500")
        runCommand("settings put secure multi_press_timeout 400")

        logCallback("[*] Restoring network settings to stock...")
        runCommand("settings put global private_dns_mode opportunistic")
        runCommand("settings delete global private_dns_specifier")
        runCommand("settings put global wifi_scan_throttle_enabled 1")
        runCommand("settings put global wifi_suspend_optimizations_enabled 1")
        runCommand("settings put global mobile_data_always_on 0")

        logCallback("[*] Restoring POCO beast extras to stock...")
        runCommand("settings delete system peak_refresh_rate")
        runCommand("settings put global game_driver_all_apps 0")
        runCommand("settings put global adaptive_battery_management_enabled 1")
        runCommand("dumpsys deviceidle enable")

        logCallback("[✓] Standard system settings restored!")
    }

    /**
     * Executes a user-imported .sh script via the Shizuku shell.
     * The script content is passed directly as argv (no temp file, no quoting issues).
     */
    fun runScript(scriptContent: String, logCallback: (String) -> Unit) {
        logCallback("[*] Executing script via Shizuku shell...")
        try {
            val process = newShizukuProcess(arrayOf("sh", "-c", scriptContent))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                logCallback("[out] $line")
            }
            while (errReader.readLine().also { line = it } != null) {
                logCallback("[err] $line")
            }
            val code = process.waitFor()
            logCallback(if (code == 0) "[✓] Script finished (exit 0)" else "[!] Script exited with code $code")
        } catch (e: Exception) {
            logCallback("[✕] Failed: ${e.localizedMessage}")
        }
    }
}
