package com.buildlaunchfly.game.gfx

import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3

/** Interleaved vertex data: position(3) + normal(3) + uv(2). */
class MeshData(val vertices: FloatArray, val indices: IntArray) {
    val vertexCount: Int get() = vertices.size / VERTEX_FLOATS

    companion object {
        const val VERTEX_FLOATS = 8
        const val STRIDE_BYTES = VERTEX_FLOATS * 4
    }
}

/** A compiled GLSL program with a small uniform-location cache. */
class ShaderProgram private constructor(val label: String, val id: Int) {

    private val uniformCache = HashMap<String, Int>()

    fun use() {
        if (id != 0) android.opengl.GLES30.glUseProgram(id)
    }

    fun location(name: String): Int {
        val cached = uniformCache[name]
        if (cached != null) return cached
        val loc = if (id == 0) -1 else android.opengl.GLES30.glGetUniformLocation(id, name)
        uniformCache[name] = loc
        return loc
    }

    fun set(name: String, value: Int) {
        val loc = location(name)
        if (loc >= 0) android.opengl.GLES30.glUniform1i(loc, value)
    }

    fun set(name: String, value: Float) {
        val loc = location(name)
        if (loc >= 0) android.opengl.GLES30.glUniform1f(loc, value)
    }

    fun set(name: String, x: Float, y: Float) {
        val loc = location(name)
        if (loc >= 0) android.opengl.GLES30.glUniform2f(loc, x, y)
    }

    fun set(name: String, value: Vec3) {
        val loc = location(name)
        if (loc >= 0) android.opengl.GLES30.glUniform3f(loc, value.x, value.y, value.z)
    }

    fun set(name: String, value: Mat4) {
        val loc = location(name)
        if (loc >= 0) {
            android.opengl.GLES30.glUniformMatrix4fv(loc, 1, false, value.m, 0)
        }
    }

    fun dispose() {
        if (id != 0) android.opengl.GLES30.glDeleteProgram(id)
        uniformCache.clear()
    }

    companion object {
        fun create(label: String, vertexSource: String, fragmentSource: String): ShaderProgram {
            val vs = GLUtil.compileShader(
                android.opengl.GLES30.GL_VERTEX_SHADER, vertexSource, "$label.vert"
            )
            val fs = GLUtil.compileShader(
                android.opengl.GLES30.GL_FRAGMENT_SHADER, fragmentSource, "$label.frag"
            )
            val program = GLUtil.linkProgram(vs, fs, label)
            if (vs != 0) android.opengl.GLES30.glDeleteShader(vs)
            if (fs != 0) android.opengl.GLES30.glDeleteShader(fs)
            return ShaderProgram(label, program)
        }
    }
}
