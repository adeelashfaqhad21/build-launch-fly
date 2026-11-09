package com.buildlaunchfly.game.gfx

import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3

/** Environment state shared by every lit program. */
class EnvUniforms {

    val sunDir = Vec3(0.42f, 0.62f, 0.66f)
    val sunColor = Vec3(3.1f, 2.95f, 2.7f)
    val skyColor = Vec3(0.42f, 0.55f, 0.78f)
    val groundColor = Vec3(0.20f, 0.21f, 0.16f)
    val fogColor = Vec3(0.72f, 0.80f, 0.88f)
    val zenithColor = Vec3(0.16f, 0.34f, 0.68f)
    val horizonColor = Vec3(0.72f, 0.80f, 0.88f)

    var fogDensity = 0.00055f
    var shadowStrength = 0.72f
    var time = 0f

    val lightViewProj = Mat4()
    var shadowTexel = 1f / 1536f

    init {
        sunDir.normalize()
    }

    fun apply(program: ShaderProgram, camera: Camera) {
        program.set("uViewProj", camera.viewProj)
        program.set("uCameraPos", camera.position)
        program.set("uSunDir", sunDir)
        program.set("uSunColor", sunColor)
        program.set("uSkyColor", skyColor)
        program.set("uGroundColor", groundColor)
        program.set("uFogColor", fogColor)
        program.set("uFogDensity", fogDensity)
        program.set("uTime", time)
        program.set("uShadowStrength", shadowStrength)
        program.set("uShadowTexel", shadowTexel)
        program.set("uLightViewProj", lightViewProj)
        program.set("uShadowMap", SHADOW_TEXTURE_UNIT)
    }

    /** Copies a world palette into the live environment. */
    fun copyFrom(other: EnvUniforms) {
        sunDir.set(other.sunDir)
        sunColor.set(other.sunColor)
        skyColor.set(other.skyColor)
        groundColor.set(other.groundColor)
        fogColor.set(other.fogColor)
        zenithColor.set(other.zenithColor)
        horizonColor.set(other.horizonColor)
        fogDensity = other.fogDensity
        shadowStrength = other.shadowStrength
    }

    companion object {
        const val SHADOW_TEXTURE_UNIT = 3
    }
}
