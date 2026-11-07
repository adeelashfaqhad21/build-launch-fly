package com.buildlaunchfly.game.gfx

import android.opengl.GLES30
import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3

/**
 * Owns every shading program and orchestrates the frame: shadow pass, opaque pass,
 * transparent pass.
 */
class SceneRenderer(var width: Int, var height: Int, private val white: Texture2D) {

    val env = EnvUniforms()
    val shadowMap = ShadowMap(1536)

    private val pbrProgram = ShaderProgram.create(
        "pbr", ShaderSources.PBR_VERT, ShaderSources.PBR_FRAG
    )
    private val instancedProgram = ShaderProgram.create(
        "pbrInstanced", ShaderSources.PBR_INSTANCED_VERT, ShaderSources.PBR_FRAG
    )
    private val terrainProgram = ShaderProgram.create(
        "terrain", ShaderSources.PBR_VERT, ShaderSources.TERRAIN_FRAG
    )
    private val depthProgram = ShaderProgram.create(
        "depth", ShaderSources.DEPTH_VERT, ShaderSources.DEPTH_FRAG
    )
    private val depthInstancedProgram = ShaderProgram.create(
        "depthInstanced", ShaderSources.DEPTH_INSTANCED_VERT, ShaderSources.DEPTH_FRAG
    )
    private val propellerBlurProgram = ShaderProgram.create(
        "propellerBlur", ShaderSources.PROPELLER_BLUR_VERT, ShaderSources.PROPELLER_BLUR_FRAG
    )

    val sky = SkyRenderer()
    val water = WaterRenderer()
    val particles = ParticleRenderer(3072)

    private val normalMatrix = Mat4()
    private val identity = Mat4()
    private var camera: Camera? = null

    init {
        env.shadowTexel = 1f / shadowMap.size
    }

    fun resize(w: Int, h: Int) {
        width = w
        height = h
    }

    fun beginFrame(camera: Camera) {
        this.camera = camera
        GLES30.glViewport(0, 0, width, height)
        GLES30.glClearColor(
            env.fogColor.x * 0.35f, env.fogColor.y * 0.35f, env.fogColor.z * 0.35f, 1f
        )
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        GLES30.glDisable(GLES30.GL_BLEND)

        // Bind the shadow depth texture once; every lit program samples unit 3.
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + EnvUniforms.SHADOW_TEXTURE_UNIT)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, shadowMap.depthTexture)
    }

    fun beginShadowPass(focus: Vec3, radius: Float) {
        shadowMap.updateLight(focus, radius, env.sunDir)
        env.lightViewProj.set(shadowMap.lightViewProj)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, shadowMap.fbo)
        GLES30.glViewport(0, 0, shadowMap.size, shadowMap.size)
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)

        depthProgram.use()
        depthProgram.set("uLightViewProj", shadowMap.lightViewProj)
        depthInstancedProgram.use()
        depthInstancedProgram.set("uLightViewProj", shadowMap.lightViewProj)
    }

    fun shadowMesh(mesh: Mesh, model: Mat4) {
        depthProgram.use()
        depthProgram.set("uLightViewProj", shadowMap.lightViewProj)
        depthProgram.set("uModel", model)
        mesh.draw()
    }

    fun shadowInstanced(mesh: InstancedMesh) {
        depthInstancedProgram.use()
        depthInstancedProgram.set("uLightViewProj", shadowMap.lightViewProj)
        mesh.draw()
    }

    fun endShadowPass() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, width, height)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    fun drawSky() {
        val cam = camera ?: return
        sky.render(cam, env)
    }

    fun drawMesh(mesh: Mesh, material: Material, model: Mat4) {
        val cam = camera ?: return
        pbrProgram.use()
        env.apply(pbrProgram, cam)
        pbrProgram.set("uModel", model)
        model.normalMatrix(normalMatrix)
        pbrProgram.set("uNormalMat", normalMatrix)
        material.bind(pbrProgram, 0, white)
        mesh.draw()
    }

    /** Draws a camera-facing translucent disc in the propeller plane; no shadow is cast. */
    fun drawPropellerBlur(mesh: Mesh, model: Mat4, alpha: Float) {
        val cam = camera ?: return
        val opacity = alpha.coerceIn(0f, 0.42f)
        if (opacity <= 0.001f) return
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(false)
        propellerBlurProgram.use()
        propellerBlurProgram.set("uModel", model)
        propellerBlurProgram.set("uViewProj", cam.viewProj)
        propellerBlurProgram.set("uBlurAlpha", opacity)
        mesh.draw()
        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)
    }

    fun drawInstanced(mesh: InstancedMesh, material: Material) {
        val cam = camera ?: return
        instancedProgram.use()
        env.apply(instancedProgram, cam)
        material.bind(instancedProgram, 0, white)
        mesh.draw()
    }

    fun drawTerrain(
        mesh: Mesh,
        textureA: Texture2D,
        textureB: Texture2D,
        textureC: Texture2D,
        tintA: Vec3,
        tintB: Vec3,
        tintC: Vec3,
        rockThreshold: Float,
        snowLine: Float,
        soilStrength: Float = 0f
    ) {
        val cam = camera ?: return
        terrainProgram.use()
        env.apply(terrainProgram, cam)
        terrainProgram.set("uModel", identity)
        terrainProgram.set("uNormalMat", identity)
        terrainProgram.set("uUvScale", 1f)

        textureA.bind(0)
        terrainProgram.set("uTexA", 0)
        textureB.bind(1)
        terrainProgram.set("uTexB", 1)
        textureC.bind(2)
        terrainProgram.set("uTexC", 2)

        terrainProgram.set("uTintA", tintA)
        terrainProgram.set("uTintB", tintB)
        terrainProgram.set("uTintC", tintC)
        terrainProgram.set("uRockThreshold", rockThreshold)
        terrainProgram.set("uSnowLine", snowLine)
        terrainProgram.set("uSoilStrength", soilStrength.coerceIn(0f, 0.65f))

        mesh.draw()

        // Restore the shadow texture on its own unit for the next lit draw.
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + EnvUniforms.SHADOW_TEXTURE_UNIT)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, shadowMap.depthTexture)
    }

    fun drawWater(
        mesh: Mesh,
        normals: Texture2D,
        deepColor: Vec3,
        shallowColor: Vec3,
        reflections: Boolean = true
    ) {
        val cam = camera ?: return
        water.render(mesh, normals, deepColor, shallowColor, cam, env, identity, reflections)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + EnvUniforms.SHADOW_TEXTURE_UNIT)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, shadowMap.depthTexture)
    }

    fun flushParticles(color: Vec3) {
        val cam = camera ?: return
        particles.flush(cam, env, color, height)
    }

    fun dispose() {
        pbrProgram.dispose()
        instancedProgram.dispose()
        terrainProgram.dispose()
        depthProgram.dispose()
        depthInstancedProgram.dispose()
        propellerBlurProgram.dispose()
        sky.dispose()
        water.dispose()
        particles.dispose()
        shadowMap.dispose()
    }
}
