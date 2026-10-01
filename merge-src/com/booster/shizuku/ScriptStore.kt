package com.booster.shizuku

import android.content.Context
import android.net.Uri

/** Persistent store for .sh scripts: user-imported (SAF uri) + pre-installed (assets). */
data class ScriptEntry(val name: String, val uri: String)

object ScriptStore {
    private const val NAME = "booster_scripts"
    private const val KEY_DATA = "scripts_data"
    private const val KEY_SEEDED_VERSION = "seeded_version"

    /**
     * Bump when the bundled script pack changes — re-seeds on next app start.
     * Bundled scripts live in assets/scripts/ and are copied to internal storage.
     */
    const val SEED_VERSION = 2

    private fun prefs(context: Context) =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getAll(context: Context): MutableList<ScriptEntry> {
        val raw = prefs(context).getString(KEY_DATA, "") ?: ""
        if (raw.isBlank()) return mutableListOf()
        return raw.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val idx = line.indexOf("||")
                if (idx <= 0) null else ScriptEntry(line.substring(0, idx), line.substring(idx + 2))
            }
            .toMutableList()
    }

    fun add(context: Context, name: String, uri: Uri) {
        val cleanName = name.ifBlank { "script.sh" }.replace("\n", "")
        val list = getAll(context)
        // replace existing entry with same uri
        list.removeAll { it.uri == uri.toString() }
        list.add(ScriptEntry(cleanName, uri.toString()))
        save(context, list)
    }

    fun remove(context: Context, entry: ScriptEntry) {
        val list = getAll(context)
        list.removeAll { it.uri == entry.uri }
        save(context, list)
        // best-effort: release persistable permission
        try {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(entry.uri),
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) { }
    }

    private fun save(context: Context, list: List<ScriptEntry>) {
        val raw = list.joinToString("\n") { "${it.name}||${it.uri}" }
        prefs(context).edit().putString(KEY_DATA, raw).apply()
    }

    /**
     * Copies the pre-installed script pack from assets/scripts/ into internal
     * storage and registers each file. Runs once per SEED_VERSION — user
     * deletions are respected until the pack changes.
     */
    fun seedFromAssets(context: Context) {
        val p = prefs(context)
        if (p.getInt(KEY_SEEDED_VERSION, 0) >= SEED_VERSION) return
        val dir = java.io.File(context.filesDir, "bundled_scripts").apply { mkdirs() }
        // v2 migration: poco_f5pro_beast.sh merged into beast_mode.sh
        java.io.File(dir, "poco_f5pro_beast.sh").delete()
        val names = try {
            context.assets.list("scripts")
        } catch (_: Exception) {
            null
        } ?: emptyArray()
        val list = getAll(context).toMutableList()
        for (name in names) {
            if (!name.endsWith(".sh")) continue
            val out = java.io.File(dir, name)
            try {
                context.assets.open("scripts/$name").use { input ->
                    out.outputStream().use { input.copyTo(it) }
                }
            } catch (_: Exception) {
                continue
            }
            val uri = Uri.fromFile(out).toString()
            // drop any stale bundled copy of the same script, then register fresh
            list.removeAll { it.uri == uri || (it.name == name && it.uri.startsWith("file:")) || it.name == "poco_f5pro_beast.sh" }
            list.add(ScriptEntry(name, uri))
        }
        save(context, list)
        p.edit().putInt(KEY_SEEDED_VERSION, SEED_VERSION).apply()
    }

    /**
     * Reads a script's content regardless of source: file:// (bundled) or
     * content:// (user-imported via SAF).
     */
    fun readContent(context: Context, entry: ScriptEntry): String? {
        return try {
            val uri = Uri.parse(entry.uri)
            if (uri.scheme == "file") {
                val f = java.io.File(uri.path ?: return null)
                if (f.exists()) f.readText() else null
            } else {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            }
        } catch (_: Exception) {
            null
        }
    }
}
