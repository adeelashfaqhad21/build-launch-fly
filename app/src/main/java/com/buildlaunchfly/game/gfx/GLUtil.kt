package com.buildlaunchfly.game.gfx

import android.opengl.GLES30
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Thin helpers around raw GL calls, plus the fixed vertex attribute layout. */
object GLUtil {

    const val TAG = "BLF-GL"

    const val ATTR_POSITION = 0
    const val ATTR_NORMAL = 1
    const val ATTR_UV = 2

    /** Instance matrix occupies locations 4, 5, 6, 7. */
    const val ATTR_INSTANCE = 4

    fun checkError(label: String) {
        var err = GLES30.glGetError()
        var guard = 0
        while (err != GLES30.GL_NO_ERROR && guard < 8) {
            Log.e(TAG, "GL error 0x${Integer.toHexString(err)} at: $label")
            err = GLES30.glGetError()
            guard++
        }
    }

    fun compileShader(type: Int, source: String, label: String): Int {
        val shader = GLES30.glCreateShader(type)
        if (shader == 0) {
            Log.e(TAG, "glCreateShader returned 0 for $label")
            return 0
        }
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e(TAG, "Shader compile failed ($label):\n" + GLES30.glGetShaderInfoLog(shader))
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    fun linkProgram(vertexShader: Int, fragmentShader: Int, label: String): Int {
        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e(TAG, "Cannot link $label: a shader failed to compile")
            return 0
        }
        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)

        val status = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            android.util.Log.e(
                "BLF-GL",
                "Program link failed ($label):\n" + GLES30.glGetProgramInfoLog(program)
            )
            GLES30.glDeleteProgram(program)
            return 0
        }
        return program
    }

    /** Uploads a float array into a new ARRAY_BUFFER and returns its id. */
    fun uploadFloatBuffer(data: FloatArray): Int {
        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        val buffer = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(data)
        buffer.position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, ids[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, data.size * 4, buffer, GLES30.GL_STATIC_DRAW)
        return ids[0]
    }

    /** Uploads an int array into a new ELEMENT_ARRAY_BUFFER and returns its id. */
    fun uploadIndexBuffer(data: IntArray): Int {
        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        val buffer = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
        buffer.put(data)
        buffer.position(0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ids[0])

        GLES30.glBufferData(
            GLES30.GL_ELEMENT_ARRAY_BUFFER,
            data.size * 4,
            buffer,
            GLES30.GL_STATIC_DRAW
        )
        return ids[0]
    }

    /** Configures the standard position/normal/uv attribute pointers for the bound VAO. */
    fun bindStandardAttributes() {
        GLES30.glEnableVertexAttribArray(ATTR_POSITION)
        GLES30.glVertexAttribPointer(
            ATTR_POSITION, 3, GLES30.GL_FLOAT, false, MeshData.STRIDE_BYTES, 0
        )
        GLES30.glEnableVertexAttribArray(ATTR_NORMAL)
        GLES30.glVertexAttribPointer(
            ATTR_NORMAL, 3, GLES30.GL_FLOAT, false, MeshData.STRIDE_BYTES, 12
        )
        GLES30.glEnableVertexAttribArray(ATTR_UV)
        GLES30.glVertexAttribPointer(
            ATTR_UV, 2, GLES30.GL_FLOAT, false, MeshData.STRIDE_BYTES, 24
        )
    }
}
