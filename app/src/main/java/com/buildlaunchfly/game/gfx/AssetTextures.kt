package com.buildlaunchfly.game.gfx

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log

/**
 * Decodes a PNG from the APK assets into a GL texture. Images are downscaled while decoding —
 * the Quaternius source maps go up to 4K, which would be tens of megabytes of texture memory
 * on a phone — and uploaded mip-mapped, which is what the foliage alpha-ish edges need to
 * stay calm at distance.
 */
object AssetTextures {

    private const val TAG = "BLF-ASSETS"

    fun load(assets: AssetManager, path: String, maxSize: Int = 512): Texture2D? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSize) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }
            if (bitmap == null) {
                null
            } else {
                try {
                    upload(bitmap, path)
                } finally {
                    bitmap.recycle()
                }
            }
        }
    } catch (error: Throwable) {
        Log.e(TAG, "Failed to load texture $path", error)
        null
    }

    private fun upload(bitmap: Bitmap, label: String): Texture2D {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val rgba = ByteArray(width * height * 4)
        var i = 0
        for (pixel in pixels) {
            rgba[i++] = ((pixel shr 16) and 0xFF).toByte()
            rgba[i++] = ((pixel shr 8) and 0xFF).toByte()
            rgba[i++] = (pixel and 0xFF).toByte()
            rgba[i++] = ((pixel ushr 24) and 0xFF).toByte()
        }
        return Texture2D.create(label, width, height, rgba, mipmaps = true, repeat = true)
    }
}
