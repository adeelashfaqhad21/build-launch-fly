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
    val hazard: Texture2D
    val bark: Texture2D
    val leaf: Texture2D
    val soil: Texture2D
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
        hazard = register(Texture2D.create("hazard", texSize, texSize, hazardTexture(texSize)))
        bark = register(Texture2D.create("bark", texSize, texSize, barkTexture(texSize)))
        leaf = register(Texture2D.create("leaf", texSize, texSize, leafTexture(texSize)))
        soil = register(Texture2D.create("soil", texSize, texSize, soilTexture(texSize)))
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
        val broad = Noise.fbm2(u * 4f, v * 4f, 3, 2f, 0.5f, seed)
        val blades = Noise.fbm2(u * 26f, v * 60f, 3, 2f, 0.55f, seed + 11)
        val fine = Noise.value2(u * 140f, v * 140f, seed + 23)
        val patch = Noise.fbm2(u * 1.7f, v * 1.7f, 2, 2f, 0.5f, seed + 37)

        val r = 0.20f + broad * 0.05f + patch * 0.045f + blades * 0.03f
        val g = 0.36f + broad * 0.09f + patch * 0.075f + blades * 0.06f + fine * 0.02f
        val b = 0.13f + broad * 0.04f + fine * 0.012f
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
        val broad = Noise.fbm2(u * 5f, v * 5f, 4, 2f, 0.5f, seed + 131)
        val crust = Noise.fbm2(u * 22f, v * 22f, 3, 2f, 0.55f, seed + 149)
        val sparkle = if (Noise.value2(u * 220f, v * 220f, seed + 157) > 0.86f) 0.12f else 0f

        val lum = 0.90f + broad * 0.045f + crust * 0.03f + sparkle
        rgb(lum, lum * 0.99f, M.saturate(lum * 1.02f))
    }

    private fun iceTexture(size: Int): ByteArray = gen(size) { _, _, u, v ->
        val vein = 1f - M.smoothStep(
            0.02f, 0.14f,
            kotlin.math.abs(Noise.fbm2(u * 6f, v * 6f, 4, 2f, 0.5f, seed + 173))
        )
        val grain = Noise.fbm2(u * 30f, v * 30f, 3, 2f, 0.5f, seed + 181)
        val base = 0.74f + grain * 0.05f
        val lum = base * (1f - vein * 0.35f)
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

    /**
     * Two-colour warning stripes for the slingshot elastics: yellow `#FFD52B` alternating
     * with cream `#F5F2E8` across the band. The stripe seam is a shade darker, and a drifting
     * noise term keeps the tube from reading as flat paint.
     */
    private fun hazardTexture(size: Int): ByteArray = gen(size) { _, _, u, v ->
        val drift = Noise.value2(u * 3f, v * 3f, seed + 311) * 0.03f
        val raw = v * 4f + drift
        val phase = raw - kotlin.math.floor(raw)
        val grain = Noise.value2(u * 34f, v * 34f, seed + 317)
        val seam = if (phase < 0.03f || kotlin.math.abs(phase - 0.5f) < 0.03f) 0.74f else 1f
        val shade = (0.94f + grain * 0.06f) * seam

        if (phase < 0.5f) {
            rgb(1.00f * shade, 0.835f * shade, 0.169f * shade)
        } else {
            rgb(0.961f * shade, 0.949f * shade, 0.910f * shade)
        }
    }

    /**
     * Exposed earth for the barren patches between grass: clods, fine grit and a scatter of
     * small stones, in a dry brown so it reads against the grass textures.
     */
    private fun soilTexture(size: Int): ByteArray = gen(size) { _, _, u, v ->
        val clods = Noise.fbm2(u * 6f, v * 6f, 3, 2f, 0.5f, seed + 401)
        val grit = Noise.value2(u * 130f, v * 130f, seed + 409)
        val damp = Noise.fbm2(u * 2.2f, v * 2.2f, 2, 2f, 0.5f, seed + 421)
        val stone = if (Noise.value2(u * 24f, v * 24f, seed + 431) > 0.84f) 0.22f else 0f

        val lum = 0.34f + clods * 0.06f + grit * 0.05f + damp * 0.05f + stone
        rgb(lum, lum * 0.72f, lum * 0.50f)
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

    /**
     * Tangent-space normal map derived from a small FBM height field. Built at half the
     * surface resolution with two octaves: it is sampled at a low water UV scale, so the
     * extra resolution only cost start-up time and bought nothing visible.
     */
    private fun waterNormalTexture(size: Int): ByteArray {
        val sz = (size / 2).coerceIn(32, 128)
        val out = ByteArray(sz * sz * 4)
        var i = 0
        val eps = 1f / sz
        for (y in 0 until sz) {
            val v = y.toFloat() / sz
            for (x in 0 until sz) {
                val u = x.toFloat() / sz
                val h = Noise.fbm2(u * 6f, v * 6f, 2, 2.1f, 0.5f, seed + 293) * 0.5f
                val hx = Noise.fbm2((u + eps) * 6f, v * 6f, 2, 2.1f, 0.5f, seed + 293) * 0.5f
                val hy = Noise.fbm2(u * 6f, (v + eps) * 6f, 2, 2.1f, 0.5f, seed + 293) * 0.5f

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
    var emissive: Float = 0f,
    var alpha: Float = 1f
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
        program.set("uAlpha", alpha)
    }
}
