package com.buildlaunchfly.game.gfx

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A 2D RGBA texture uploaded to GL. */
class Texture2D(val label: String, val id: Int, val width: Int, val height: Int) {

    fun bind(unit: Int) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id)
    }

    fun dispose() {
        if (id != 0) GLES30.glDeleteTextures(1, intArrayOf(id), 0)
    }

    companion object {
        fun create(
            label: String,
            width: Int,
            height: Int,
            rgba: ByteArray,
            mipmaps: Boolean = true,
            repeat: Boolean = true
        ): Texture2D {
            val ids = IntArray(1)
            GLES30.glGenTextures(1, ids, 0)
            val id = ids[0]
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id)

            val buffer = ByteBuffer.allocateDirect(rgba.size).order(ByteOrder.nativeOrder())
            buffer.put(rgba)
            buffer.position(0)
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
                width, height, 0,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer
            )

            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER,
                if (mipmaps) GLES30.GL_LINEAR_MIPMAP_LINEAR else GLES30.GL_LINEAR
            )
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR
            )
            val wrap = if (repeat) GLES30.GL_REPEAT else GLES30.GL_CLAMP_TO_EDGE
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, wrap)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, wrap)

            if (mipmaps) GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
            GLUtil.checkError("Texture2D.create($label)")
            return Texture2D(label, id, width, height)
        }
    }
}
