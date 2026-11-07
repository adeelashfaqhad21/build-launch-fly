package com.buildlaunchfly.game.gfx

import android.opengl.GLES30
import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3

/** Normal-mapped water surface with Fresnel sky reflection and sun specular. */
class WaterRenderer {

    private val program = ShaderProgram.create(
        "water", ShaderSources.PBR_VERT, ShaderSources.WATER_FRAG
    )

    fun render(
        mesh: Mesh,
        normals: Texture2D,
        deepColor: Vec3,
        shallowColor: Vec3,
        camera: Camera,
        env: EnvUniforms,
        model: Mat4,
        reflections: Boolean = true
    ) {
        program.use()
        env.apply(program, camera)
        program.set("uModel", model)
        program.set("uNormalMat", model)
        program.set("uUvScale", 1f)
        program.set("uDeepColor", deepColor)
        program.set("uShallowColor", shallowColor)
        program.set("uTime", env.time)
        program.set("uReflections", if (reflections) 1f else 0f)
        normals.bind(0)
        program.set("uNormals", 0)

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(false)
        mesh.draw()
        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)
    }

    fun dispose() {
        program.dispose()
    }
}
