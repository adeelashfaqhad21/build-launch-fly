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

    val sky = SkyRenderer()
    val water = WaterRenderer()
    val particles = ParticleRenderer(3072)

    private val normalMatrix = Mat4()
    private val identity = Mat4()
    private var camera: Camera? = null

    init {
        env.shadowTexel = 1f / shadowMap.size
    }

    /**
     * Programs that failed to compile or link, by label. A non-empty list means either a
     * driver problem or a GLSL error: the owner reports it instead of drawing with whatever
     * program happened to be bound.
     */
    val failedPrograms: List<String>
        get() = arrayOf(
            "pbr" to pbrProgram,
            "pbrInstanced" to instancedProgram,
            "terrain" to terrainProgram,
            "depth" to depthProgram,
            "depthInstanced" to depthInstancedProgram
        ).filter { it.second.id == 0 }.map { it.first }

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

        val blended = material.alpha < 0.999f
        if (blended) {
            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            GLES30.glDepthMask(false)
        }
        mesh.draw()
        if (blended) {
            GLES30.glDepthMask(true)
            GLES30.glDisable(GLES30.GL_BLEND)
        }
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
        soil: Texture2D,
        tintA: Vec3,
        tintB: Vec3,
        tintC: Vec3,
        rockThreshold: Float,
        snowLine: Float,
        soilStart: Float = 0.52f,
        soilEnd: Float = 0.68f
    ) {
        val cam = camera ?: return
        if (terrainProgram.id == 0) {
            // The layered terrain program did not build. Draw the ground with the plain PBR
            // program and the base layer instead: a ground without its soil blend is a far
            // better failure than a landscape left as sky.
            drawTerrainFallback(mesh, textureA, tintA, cam)
            return
        }
        terrainProgram.use()
        env.apply(terrainProgram, cam)
        // The world sky fill is deliberately blue for atmospheric lighting, but applying it
        // unmodified to an upward-facing terrain normal makes every non-desert ground map look
        // cyan. Keep the terrain's ambient fill close to neutral without changing prop lighting.
        terrainProgram.set("uSkyColor", terrainSkyFill())
        terrainProgram.set("uModel", identity)
        terrainProgram.set("uNormalMat", identity)
        // TerrainMesh writes one UV unit per 8 m of ground; this maps that to a ~10 m
        // texture tile, which is the scale the imported photo maps were shot at. The old
        // value of 1 tiled them every 8 m and made the ground shimmer like gravel.
        terrainProgram.set("uUvScale", TERRAIN_UV_SCALE)

        textureA.bind(0)
        terrainProgram.set("uTexA", 0)
        textureB.bind(1)
        terrainProgram.set("uTexB", 1)
        textureC.bind(2)
        terrainProgram.set("uTexC", 2)
        soil.bind(4)
        terrainProgram.set("uTexD", 4)

        // The imported maps are photographs and already carry the ground's real colour, so
        // the realm tints are allowed a hint of the design palette instead of re-grading
        // them: a strong tint over a photo reads as dirt, not as the realm's soil.
        terrainProgram.set("uTintA", muted(tintA))
        terrainProgram.set("uTintB", muted(tintB))
        terrainProgram.set("uTintC", muted(tintC))
        terrainProgram.set("uRockThreshold", rockThreshold)
        terrainProgram.set("uSnowLine", snowLine)
        terrainProgram.set("uSoilStart", soilStart)
        terrainProgram.set("uSoilEnd", soilEnd)

        mesh.draw()

        // Restore the shadow texture on its own unit for the next lit draw.
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + EnvUniforms.SHADOW_TEXTURE_UNIT)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, shadowMap.depthTexture)
    }

    /** Prevent blue sky fill from washing upward-facing terrain into a cyan cast. */
    private fun terrainSkyFill(): Vec3 = Vec3(
        env.skyColor.x,
        env.skyColor.y,
        env.skyColor.z.coerceAtMost(env.skyColor.y * 1.08f)
    )

    /** Pulls a design tint [TINT_NEUTRALISE] of the way back to white for photographic maps. */
    private fun muted(tint: Vec3): Vec3 = Vec3(
        tint.x + (1f - tint.x) * TINT_NEUTRALISE,
        tint.y + (1f - tint.y) * TINT_NEUTRALISE,
        tint.z + (1f - tint.z) * TINT_NEUTRALISE
    )

    /** Plain PBR draw of the terrain's base layer, used when the layered program is missing. */
    private fun drawTerrainFallback(mesh: Mesh, texture: Texture2D, tint: Vec3, cam: Camera) {
        pbrProgram.use()
        env.apply(pbrProgram, cam)
        pbrProgram.set("uSkyColor", terrainSkyFill())
        pbrProgram.set("uModel", identity)
        pbrProgram.set("uNormalMat", identity)
        pbrProgram.set("uUvScale", TERRAIN_UV_SCALE)
        texture.bind(0)
        pbrProgram.set("uAlbedo", 0)
        pbrProgram.set("uTint", muted(tint))
        pbrProgram.set("uRoughness", 0.9f)
        pbrProgram.set("uMetallic", 0f)
        pbrProgram.set("uEmissive", 0f)
        pbrProgram.set("uAlpha", 1f)
        mesh.draw()

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
        sky.dispose()
        water.dispose()
        particles.dispose()
        shadowMap.dispose()
    }

    companion object {
        /**
         * Texture tiling in UV units. TerrainMesh writes one UV unit per 8 m of ground, so
         * 0.75 is roughly one 10.7 m photo tile — the scale the imported maps were shot at.
         */
        const val TERRAIN_UV_SCALE = 0.75f

        /** How far a realm tint is pulled back toward white for the photographic maps. */
        const val TINT_NEUTRALISE = 0.8f
    }
}
