package com.buildlaunchfly.game.gfx

import android.content.res.AssetManager
import android.util.Log
import java.util.zip.ZipInputStream

/**
 * Reads small OBJ/PNG props straight out of a zip shipped in `assets` — the itch.io aircraft kit
 * arrives only as an archive, and there is no build-time unzip step, so the handful of models we
 * want are pulled out in memory at load time.
 */
object ZipKit {

    private const val TAG = "BLF-KIT"

    class Entry(val name: String, val bytes: ByteArray)

    fun read(assets: AssetManager, path: String): List<Entry> = try {
        val entries = ArrayList<Entry>()
        assets.open(path).use { stream ->
            ZipInputStream(stream.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val lower = entry.name.lowercase()
                        if (lower.endsWith(".obj") || lower.endsWith(".png") || lower.endsWith(".txt")) {
                            entries.add(Entry(entry.name, zip.readBytes()))
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        entries
    } catch (error: Throwable) {
        Log.e(TAG, "Failed to read archive $path", error)
        emptyList()
    }

    /** First entry with the given suffix whose path contains [contains]. */
    fun find(entries: List<Entry>, suffix: String, contains: String): Entry? =
        entries.firstOrNull {
            val lower = it.name.lowercase()
            lower.endsWith(suffix) && lower.contains(contains)
        }

    /** PNG whose name shares the OBJ's base name, else the first PNG in the archive. */
    fun textureFor(entries: List<Entry>, objName: String): Entry? {
        val base = objName.substringBeforeLast('.').substringAfterLast('/').lowercase()
        return entries.firstOrNull {
            val lower = it.name.lowercase()
            lower.endsWith(".png") &&
                lower.substringBeforeLast('.').substringAfterLast('/').contains(base)
        } ?: entries.firstOrNull { it.name.lowercase().endsWith(".png") }
    }
}
