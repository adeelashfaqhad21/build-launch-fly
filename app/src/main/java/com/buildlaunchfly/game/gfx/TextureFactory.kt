package com.buildlaunchfly.game.gfx

import com.buildlaunchfly.game.math.M
import com.buildlaunchfly.game.math.Vec3

/** The surface classes a world can be built from. */
enum class SurfaceKind { GRASS, ROCK, SAND, SNOW, ICE }

/**
 * Generates every texture the game needs at load time. Nothing is shipped as a binary
 * asset: the whole look is defined by code and a fixed seed, so worlds are reproducible
 * and the repository stays small.
 *
 * [size] is driven by the "High-Resolution Textures" setting.
 */
class TextureFactory(private val seed: Int = 1337, private val size: Int = SIZE) {

    val white: Texture2D
    val metal: Texture2D
    val rubber: Texture2D
    val bark: Texture2D
    val leaf: Texture2D
    val waterNormals: Texture2D

    private val surfaces = HashMap<SurfaceKind, Texture2D>()
    private val all = ArrayList<Texture2D>()

    init {
        val texSize = size.coerceIn(32, 1024)
        white = register(
            Texture2D.create("white", 1, 1, ByteArray(4) { 255.toByte() }, mipmaps = false, repeat = false)
        )

        surfaces[SurfaceKind.GRASS] = register(Texture2D.create("grass", texSize, texSize, grassTexture(texSize)))
        surfaces[SurfaceKind.ROCK] = register(Texture2D.create("rock", texSize, texSize, rockTexture(texSize)))
        surfaces[SurfaceKind.SAND] = register(Texture2D.create("sand", texSize, texSize, sandTexture(texSize)))
        surfaces[SurfaceKind.SNOW] = register(Texture2D.create("snow", texSize, texSize, snowTexture(texSize)))
        surfaces[SurfaceKind.ICE] = register(Texture2D.create("ice", texSize, texSize, iceTexture(texSize)))

        metal = register(Texture2D.create("metal", texSize, texSize, metalTexture(texSize)))
        rubber = register(Texture2D.create("rubber", texSize, texSize, rubberTexture(texSize)))
        bark = register(Texture2D.create("bark", texSize, texSize, barkTexture(texSize)))
        leaf = register(Texture2D.create("leaf", texSize, texSize, leafTexture(texSize)))
        waterNormals = register(Texture2D.create("waterNormal", texSize, texSize, waterNormalTexture(texSize)))
    }

    private fun register(texture: Texture2D): Texture2D {
        all.add(texture)
        return texture
    }

    fun surface(kind: SurfaceKind): Texture2D = surfaces[kind] ?: white

    fun disposeAll() {
        for (texture in all) texture.dispose()
        all.clear()
        surfaces.clear()
    }

    // ---- generation -------------------------------------------------------

    private fun gen(size: Int, pixel: (Int, Int, Float, Float) -> Int): ByteArray {
        val out = ByteArray(size * size * 4)
        var i = 0
        for (y in 0 until size) {
            val v = y.toFloat() / size
            for (x in 0 until size) {
                val u = x.toFloat() / size
                val packed = pixel(x, y, u, v)
                out[i++] = ((packed shr 16) and 0xFF).toByte()
                out[i++] = ((packed shr 8) and 0xFF).toByte()
                out[i++] = (packed and 0xFF).toByte()
                out[i++] = ((packed ushr 24) and 0xFF).toByte()
            }
        }
        return out
    }

    private fun rgb(r: Float, g: Float, b: Float, a: Int = 255): Int {
        val cr = (M.saturate(r) * 255f).toInt()
        val cg = (M.saturate(g) * 255f).toInt()
        val cb = (M.saturate(b) * 255f).toInt()
        return (a shl 24) or (cr shl 16) or (cg shl 8) or cb
    }

    private fun grassTexture(size: Int): ByteArray = gen(size) { _, _, u, v ->
        // Broad, irregular colour patches dominate; short blade-scale noise is
        // deliberately restrained so the terrain does not read as a tiled green carpet.
        val broad = Noise.fbm2(u * 3.2f, v * 3.2f, 4, 2f, 0.52f, seed)
        val blades = Noise.fbm2(u * 17f, v * 29f, 3, 2f, 0.58f, seed + 11)
        val fine = Noise.value2(u * 118f, v * 118f, seed + 23)
        val patch = Noise.fbm2(u * 1.25f, v * 1.25f, 3, 2f, 0.5f, seed + 37)
        val dryPatch = M.smoothStep(0.08f, 0.52f, patch + broad * 0.42f)

        val r = 0.22f + broad * 0.065f + dryPatch * 0.075f + blades * 0.018f
        val g = 0.32f + broad * 0.075f + patch * 0.045f - dryPatch * 0.055f + blades * 0.028f + fine * 0.012f
        val b = 0.14f + broad * 0.035f + dryPatch * 0.018f + fine * 0.010f
        rgb(r, g, b)
    }

    private fun rockTexture(size: Int): ByteArray = gen(size) { _, _, u, v ->
        val strata = kotlin.math.sin((v * 9f + Noise.fbm2(u * 3f, v * 3f, 3, 2f, 0.5f, seed + 5) * 2.4f) * 3.14159f)
        val cracks = 1f - M.smoothStep(
            0.03f, 0.16f,
            kotlin.math.abs(Noise.fbm2(u * 8f, v * 8f, 3, 2f, 0.5f, seed + 61))
        )
        val grain = Noise.fbm2(u * 40f, v * 40f, 3, 2f, 0.5f, seed + 71)

        val base = 0.40f + strata * 0.045f + grain * 0.07f
        val tinted = base * (1f - cracks * 0.45f)
        rgb(tinted * 1.05f, tinted * 0.98f, tinted * 0.90f)
    }

    private fun sandTexture(size: Int): ByteArray = gen(size) { _, _, u, v ->
        val ripple = kotlin.math.sin(
            (v * 26f + Noise.fbm2(u * 4f, v * 4f, 3, 2f, 0.5f, seed + 90) * 3.2f) * 3.14159f
        )
        val grain = Noise.value2(u * 180f, v * 180f, seed + 101)
        val broad = Noise.fbm2(u * 3f, v * 3f, 3, 2f, 0.5f, seed + 113)

        val lum = 0.80f + ripple * 0.055f + grain * 0.035f + broad * 0.05f
        rgb(lum, lum * 0.90f, lum * 0.71f)
    }

    private fun snowTexture(size: Int): ByteArray = gen(size) { _, _, u, v ->
        // Powder, wind-packed crust and scattered ice crystals. Avoid regular
        // stripe/ripple motifs: snow should read as a soft, irregular frozen surface.
        val broad = Noise.fbm2(u * 4.2f, v * 4.2f, 4, 2f, 0.52f, seed + 131)
        val crust = Noise.fbm2(u * 17f, v * 17f, 4, 2f, 0.56f, seed + 149)
        val micro = Noise.value2(u * 96f, v * 96f, seed + 153)
        val crystal = if (Noise.value2(u * 190f, v * 190f, seed + 157) > 0.91f) 0.055f else 0f
        val windPack = M.smoothStep(-0.12f, 0.48f, broad + crust * 0.24f)

        val lum = 0.875f + broad * 0.052f + crust * 0.026f + micro * 0.012f + crystal + windPack * 0.012f
        rgb(lum, lum * 0.99f, M.saturate(lum * 1.02f))
    }

    private fun iceTexture(size: Int): ByteArray = gen(size) { _, _, u, v ->
        // Branching, warped fracture veins rather than parallel painted lines.
        val warpX = Noise.fbm2(u * 4f, v * 4f, 3, 2f, 0.5f, seed + 171) * 0.075f
        val warpY = Noise.fbm2(u * 4f + 7f, v * 4f - 3f, 3, 2f, 0.5f, seed + 172) * 0.075f
        val fracture = Noise.fbm2((u + warpX) * 10f, (v + warpY) * 10f, 4, 2f, 0.52f, seed + 173)
        val vein = 1f - M.smoothStep(0.035f, 0.15f, kotlin.math.abs(fracture))
        val grain = Noise.fbm2(u * 24f, v * 24f, 3, 2f, 0.5f, seed + 181)
        val facets = Noise.value2(u * 72f, v * 72f, seed + 183)
        val base = 0.75f + grain * 0.045f + facets * 0.012f
        val lum = base * (1f - vein * 0.22f)
        rgb(lum * 0.88f, lum * 0.96f, lum * 1.05f)
    }

    private fun metalTexture(size: Int): ByteArray = gen(size) { _, _, u, v ->
        val panelU = kotlin.math.abs(kotlin.math.sin(u * 3.14159f * 4f))
        val panelV = kotlin.math.abs(kotlin.math.sin(v * 3.14159f * 6f))
        val seam = if (panelU < 0.05f || panelV < 0.05f) 0.72f else 1f
        val grain = Noise.fbm2(u * 24f, v * 24f, 3, 2f, 0.5f, seed + 197)
        val wear = Noise.fbm2(u * 6f, v * 6f, 2, 2f, 0.5f, seed + 211)

        val lum = (0.80f + grain * 0.05f + wear * 0.04f) * seam
        rgb(lum, lum * 0.995f, lum * 0.98f)
    }

    private fun rubberTexture(size: Int): ByteArray = gen(size) { _, _, u, v ->
        val grain = Noise.fbm2(u * 44f, v * 44f, 3, 2f, 0.5f, seed + 227)
        val lum = 0.16f + grain * 0.05f
        rgb(lum, lum * 0.98f, lum * 1.02f)
    }

    private fun barkTexture(size: Int): ByteArray = gen(size) { _, _, u, v ->
        val stripes = Noise.fbm2(u * 22f, v * 2.5f, 4, 2f, 0.55f, seed + 239)
        val cracks = 1f - M.smoothStep(
            0.05f, 0.22f,
            kotlin.math.abs(Noise.fbm2(u * 34f, v * 6f, 3, 2f, 0.5f, seed + 251))
        )
        val lum = (0.30f + stripes * 0.10f) * (1f - cracks * 0.4f)
        rgb(lum * 1.05f, lum * 0.80f, lum * 0.58f)
    }

    private fun leafTexture(size: Int): ByteArray = gen(size) { _, _, u, v ->
        val clump = Noise.fbm2(u * 12f, v * 12f, 4, 2f, 0.55f, seed + 263)
        val veins = 1f - M.smoothStep(
            0.02f, 0.10f,
            kotlin.math.abs(Noise.fbm2(u * 18f, v * 18f, 3, 2f, 0.5f, seed + 271))
        )
        val lum = 0.34f + clump * 0.16f
        rgb(lum * 0.62f, lum * 1.05f, lum * 0.48f)
    }

    /** Tangent-space normal map derived from a small FBM height field. */
    private fun waterNormalTexture(size: Int): ByteArray {
        val sz = size
        val out = ByteArray(sz * sz * 4)
        var i = 0
        val eps = 1f / sz
        for (y in 0 until sz) {
            val v = y.toFloat() / sz
            for (x in 0 until sz) {
                val u = x.toFloat() / sz
                val h = Noise.fbm2(u * 6f, v * 6f, 4, 2.1f, 0.5f, seed + 293) * 0.5f +
                    Noise.fbm2(u * 17f, v * 17f, 3, 2.1f, 0.5f, seed + 307) * 0.25f
                val hx = Noise.fbm2((u + eps) * 6f, v * 6f, 4, 2.1f, 0.5f, seed + 293) * 0.5f +
                    Noise.fbm2((u + eps) * 17f, v * 17f, 3, 2.1f, 0.5f, seed + 307) * 0.25f
                val hy = Noise.fbm2(u * 6f, (v + eps) * 6f, 4, 2.1f, 0.5f, seed + 293) * 0.5f +
                    Noise.fbm2(u * 17f, (v + eps) * 17f, 3, 2.1f, 0.5f, seed + 307) * 0.25f

                var nx = (h - hx) * 24f
                var ny = (h - hy) * 24f
                var nz = 1f
                val len = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz)
                nx /= len
                ny /= len
                nz /= len

                out[i++] = ((nx * 0.5f + 0.5f) * 255f).toInt().toByte()
                out[i++] = ((ny * 0.5f + 0.5f) * 255f).toInt().toByte()
                out[i++] = ((nz * 0.5f + 0.5f) * 255f).toInt().toByte()
                out[i++] = 255.toByte()
            }
        }
        return out
    }

    companion object {
        const val SIZE = 256
    }
}

/** Surface description handed to the PBR shader. */
class Material(
    var albedo: Texture2D? = null,
    var tint: Vec3 = Vec3(1f, 1f, 1f),
    var roughness: Float = 0.85f,
    var metallic: Float = 0f,
    var uvScale: Float = 1f,
    var emissive: Float = 0f
) {
    fun bind(program: ShaderProgram, textureUnit: Int = 0, fallback: Texture2D? = null) {
        val texture = albedo ?: fallback
        if (texture != null) {
            texture.bind(textureUnit)
            program.set("uAlbedo", textureUnit)
        }
        program.set("uTint", tint)
        program.set("uRoughness", roughness)
        program.set("uMetallic", metallic)
        program.set("uUvScale", uvScale)
        program.set("uEmissive", emissive)
    }
}
