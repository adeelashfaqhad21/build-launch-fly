package com.buildlaunchfly.game.world

import com.buildlaunchfly.game.gfx.Noise
import com.buildlaunchfly.game.math.M
import com.buildlaunchfly.game.math.Vec3
import kotlin.math.abs
import kotlin.math.sin

/**
 * Height field for one world. The launch pad is a flattened plateau at the origin and the
 * flight corridor (toward -Z) is gently carved so the first seconds of a flight are clear.
 */
class Terrain(val definition: WorldDefinition) {

    val padHeight: Float = definition.heightOffset + definition.hillHeight * 0.15f + 4f

    val waterLevel: Float get() = definition.waterLevel

    fun baseHeight(x: Float, z: Float): Float {
        val s = definition.seed
        val broad = Noise.fbm2(x * 0.0018f, z * 0.0018f, 4, 2f, 0.5f, s) * definition.hillHeight
        var h = broad

        if (definition.mountainHeight > 0f) {
            val ridge = Noise.ridged2(x * 0.0009f, z * 0.0009f, 3, s + 17)
            val mask = 0.35f + 0.65f * Noise.fbm01(x * 0.0006f, z * 0.0006f, 2, s + 29)
            h += ridge * definition.mountainHeight * mask
        }

        h += Noise.fbm2(x * 0.012f, z * 0.012f, 3, 2f, 0.5f, s + 41) * definition.roughness * 2.2f

        // Add restrained, domain-warped erosion relief to temperate and coastal terrain.
        // The warped field breaks up the broad hills without introducing sharp gameplay
        // hazards or a regular ripple pattern across the aircraft's flight corridor.
        if (definition.id == WorldId.FOREST ||
            definition.id == WorldId.MOUNTAIN ||
            definition.id == WorldId.OCEAN
        ) {
            val erosionWarp = Noise.fbm2(
                x * 0.0024f, z * 0.0024f, 2, 2f, 0.5f, s + 151
            ) * 13f
            val erosionRelief = Noise.fbm2(
                (x + erosionWarp) * 0.026f,
                (z - erosionWarp * 0.62f) * 0.026f,
                2, 2f, 0.5f, s + 167
            )
            h += erosionRelief * definition.roughness * 0.72f
        }

        if (definition.duneStrength > 0f) {
            val broadDune = Noise.fbm2(x * 0.0032f, z * 0.0062f, 3, 2f, 0.55f, s + 53)
            val slipFace = sin((z * 0.010f + x * 0.0022f + broadDune * 1.7f).toDouble()).toFloat()
            val ripple = sin((z * 0.085f + x * 0.012f + broadDune * 5f).toDouble()).toFloat()
            h += broadDune * definition.duneStrength + slipFace * definition.duneStrength * 0.28f
            h += ripple * 0.34f
        }

        if (definition.id == WorldId.FOREST || definition.id == WorldId.MOUNTAIN) {
            val riverX = 150f * sin(z * 0.00125f) + 54f * sin(z * 0.0028f + 0.7f)
            val riverDistance = abs(x - riverX)
            val riverWidth = if (definition.id == WorldId.FOREST) 9f else 6.5f
            val riverBed = definition.waterLevel - definition.heightOffset - 0.8f +
                (riverDistance / riverWidth) * (riverDistance / riverWidth) * 4.5f
            val bank = 1f - M.smoothStep(riverWidth, riverWidth * 3.2f, riverDistance)
            h = M.lerp(h, riverBed, bank * 0.94f)
        }

        if (definition.id == WorldId.DESERT) {
            // Broad, flat-topped sandstone mesas with steep eroded escarpments.
            val mesaCenters = arrayOf(
                floatArrayOf(-360f, -520f, 190f, 31f),
                floatArrayOf(310f, -1120f, 245f, 42f),
                floatArrayOf(-120f, -2020f, 175f, 27f),
                floatArrayOf(690f, -2580f, 220f, 36f)
            )
            for (mesa in mesaCenters) {
                val dx = x - mesa[0]
                val dz = z - mesa[1]
                val distance = kotlin.math.sqrt(dx * dx + dz * dz)
                val edgeNoise = Noise.fbm2(x * 0.009f, z * 0.009f, 2, 2f, 0.5f, s + 83) * 16f
                val radius = mesa[2] + edgeNoise
                val plateau = 1f - M.smoothStep(radius * 0.88f, radius, distance)
                val talus = 1f - M.smoothStep(radius, radius * 1.22f, distance)
                val mesaTop = h + mesa[3]
                h = M.lerp(h, mesaTop, plateau)
                h += talus * (1f - plateau) * 3.5f
            }

            // Dry wadi cuts through the dune field, exposing a lower clay wash.
            val wadiX = 95f * sin(z * 0.0018f) + 38f * sin(z * 0.0041f + 1.2f)
            val wadiDistance = abs(x - wadiX)
            val wadiCut = 1f - M.smoothStep(5f, 27f, wadiDistance)
            h -= wadiCut * 5.5f
        }

        if (definition.id == WorldId.SNOW) {
            // Main crevasse field bends with glacier flow; secondary fractures branch across it.
            val flow = z * 0.0035f + sin(x * 0.0022f) * 1.15f
            val mainWave = abs(sin((x * 0.045f + sin(flow) * 1.6f).toDouble()).toFloat())
            val branchWave = abs(sin((x * 0.019f - z * 0.012f +
                sin(z * 0.004f + x * 0.006f) * 1.1f).toDouble()).toFloat())
            val mainCrack = 1f - M.smoothStep(0.012f, 0.092f, mainWave)
            val branchCrack = 1f - M.smoothStep(0.018f, 0.12f, branchWave)
            val fractureNoise = Noise.fbm01(x * 0.008f, z * 0.008f, 2, s + 131)
            val elevationMask = M.smoothStep(definition.heightOffset + 12f, definition.heightOffset + 42f, h)
            val exposedIce = M.smoothStep(0.22f, 0.68f, fractureNoise)
            h -= elevationMask * (mainCrack * 1.45f + branchCrack * exposedIce * 0.72f)
            // Rounded pressure ridges alongside fractures catch highlights without forming sharp spikes.
            val ridge = (1f - M.smoothStep(0.10f, 0.32f, mainWave)) *
                M.smoothStep(0.015f, 0.10f, mainWave)
            h += ridge * elevationMask * 0.24f
        }

        if (definition.id == WorldId.OCEAN) {
            // A continuous, gently curving shoreline: shallow beach shelf, then weathered sea cliffs.
            val shoreline = -285f + 78f * sin(z * 0.00145f) + 24f * sin(z * 0.0031f + 0.8f)
            val inland = x - shoreline
            if (inland in 0f..190f) {
                val beachHeight = definition.waterLevel - definition.heightOffset + 1.2f + (inland / 190f) * 5.8f
                val beachBlend = 1f - M.smoothStep(0f, 190f, inland)
                h = M.lerp(h, beachHeight, beachBlend * 0.92f)
            }
            val cliffBand = M.smoothStep(185f, 260f, inland) *
                (1f - M.smoothStep(520f, 720f, inland))
            val cliffNoise = Noise.fbm2(x * 0.004f, z * 0.006f, 3, 2f, 0.55f, s + 117)
            val strata = sin((inland * 0.075f + cliffNoise * 2.8f).toDouble()).toFloat()
            h += cliffBand * (13f + cliffNoise * 8f + strata * 1.15f)
            // Several branching drainage lines cut the cliff face, with wider talus fans at its foot.
            for (gullyIndex in 0..2) {
                val offset = gullyIndex * 37f
                val gullyLine = shoreline + 230f + offset +
                    22f * sin(z * (0.0054f + gullyIndex * 0.0007f) + cliffNoise * 2f + gullyIndex)
                val gully = abs(x - gullyLine)
                val channel = 1f - M.smoothStep(2.5f, 12f + gullyIndex * 2f, gully)
                val fan = 1f - M.smoothStep(12f, 48f, gully)
                h -= channel * cliffBand * (3.2f + gullyIndex * 0.45f)
                h -= fan * cliffBand * 0.85f
            }
        }

        // Distant landform shoulders establish a readable skyline behind the playable
        // terrain. Their broad, low-amplitude profiles sit beyond the near flight corridor;
        // the existing world-specific height fields determine each ecosystem's character.
        val distance = (-z).coerceAtLeast(0f)
        val farLayer = M.smoothStep(780f, 1380f, distance) *
            (1f - M.smoothStep(2860f, 3480f, distance))
        val skylineWarp = Noise.fbm2(
            x * 0.0017f + s * 0.00013f,
            z * 0.0013f, 2, 2f, 0.5f, s + 211
        )
        val shoulderNoise = Noise.fbm2(
            x * 0.0032f + skylineWarp * 0.42f,
            z * 0.0021f - skylineWarp * 0.24f,
            3, 2f, 0.5f, s + 227
        )
        val shoulderProfile = (0.58f + shoulderNoise * 0.42f).coerceIn(0.12f, 1.0f)
        val lateralEnvelope = 1f - M.smoothStep(520f, 1380f, abs(x))
        val skylineAmplitude = when (definition.id) {
            WorldId.FOREST -> 9.5f
            WorldId.DESERT -> 14f
            WorldId.SNOW -> 23f
            WorldId.OCEAN -> 12f
            WorldId.MOUNTAIN -> 28f
        }

        // Give each far skyline a recognisable geological profile instead of using
        // one generic rolling shoulder for every unlocked world.
        val ecosystemSilhouette = when (definition.id) {
            WorldId.FOREST -> {
                // Soft, overlapping wooded ridges with no hard geometric peaks.
                val woodedRidge = Noise.ridged2(
                    x * 0.00115f, z * 0.00105f, 3, s + 251
                )
                (0.62f + woodedRidge * 0.38f).coerceIn(0.18f, 1.0f)
            }
            WorldId.DESERT -> {
                // Flat mesa crowns and broken escarpment shoulders on the far horizon.
                val mesaA = kotlin.math.exp(
                    (-((x - 470f) * (x - 470f)) / (330f * 330f) -
                        ((z + 2180f) * (z + 2180f)) / (520f * 520f)).toDouble()
                ).toFloat()
                val mesaB = kotlin.math.exp(
                    (-((x + 510f) * (x + 510f)) / (390f * 390f) -
                        ((z + 2760f) * (z + 2760f)) / (430f * 430f)).toDouble()
                ).toFloat()
                val brokenCrown = 0.72f + shoulderNoise * 0.28f
                (0.28f + maxOf(mesaA, mesaB) * brokenCrown * 1.45f).coerceIn(0.16f, 1.0f)
            }
            WorldId.SNOW -> {
                // Sharp, glacially cut high ridges with a second lower shoulder.
                val alpineRidge = Noise.ridged2(
                    x * 0.00165f, z * 0.00125f, 4, s + 263
                )
                (0.24f + alpineRidge * 0.76f).coerceIn(0.12f, 1.0f)
            }
            WorldId.OCEAN -> {
                // Isolated offshore sea stacks break the waterline silhouette.
                val stackA = kotlin.math.exp(
                    (-((x + 540f) * (x + 540f)) / (92f * 92f) -
                        ((z + 2310f) * (z + 2310f)) / (150f * 150f)).toDouble()
                ).toFloat()
                val stackB = kotlin.math.exp(
                    (-((x + 760f) * (x + 760f)) / (118f * 118f) -
                        ((z + 2920f) * (z + 2920f)) / (180f * 180f)).toDouble()
                ).toFloat()
                (0.20f + maxOf(stackA, stackB) * 0.80f).coerceIn(0.12f, 1.0f)
            }
            WorldId.MOUNTAIN -> {
                // Stronger multi-scale ridges create layered alpine peaks.
                val highRidge = Noise.ridged2(
                    x * 0.00135f, z * 0.00110f, 4, s + 277
                )
                (0.18f + highRidge * 0.82f).coerceIn(0.12f, 1.0f)
            }
        }
        h += farLayer * lateralEnvelope * shoulderProfile *
            ecosystemSilhouette * skylineAmplitude

        // Branching natural ravines create real lowland routes and raised shoulders.
        // They are deliberately outside the first launch stretch and fade irregularly
        // into the existing landform so they do not look like a straight trench.
        if (definition.id == WorldId.FOREST || definition.id == WorldId.MOUNTAIN) {
            val ravineCenter = 138f * sin(z * 0.00165f + 0.4f) +
                46f * sin(z * 0.0043f + 1.1f)
            val ravineDistance = abs(x - ravineCenter)
            val ravineWidth = 16f + 11f * Noise.fbm01(x * 0.003f, z * 0.002f, 2, s + 319)
            val ravineMask = (1f - M.smoothStep(5f, ravineWidth, ravineDistance)) *
                M.smoothStep(260f, 520f, -z) *
                (1f - M.smoothStep(3000f, 3520f, -z))
            val depth = if (definition.id == WorldId.MOUNTAIN) 13.5f else 8.5f
            h -= ravineMask * depth
            val shoulder = kotlin.math.exp(
                (-((ravineDistance - ravineWidth * 1.25f) *
                    (ravineDistance - ravineWidth * 1.25f)) / 420.0).toDouble()
            ).toFloat()
            h += shoulder * ravineMask * 2.2f
        }

        // Shallow valley along the launch corridor. The distant shoulder mask fades in
        // outside this corridor so the aircraft's first seconds remain unobstructed.
        val corridor = kotlin.math.exp((-(x * x) / (260f * 260f)).toDouble()).toFloat()
        h -= corridor * 6f

        return h + definition.heightOffset
    }

    /**
     * True inside a deep, exposed main glacier fracture. Kept consistent with the
     * crevasse field used by [baseHeight], so gameplay milestones use the rendered terrain.
     */
    fun isCrevasseZone(x: Float, z: Float): Boolean {
        if (definition.id != WorldId.SNOW) return false
        val h = baseHeight(x, z)
        val elevationMask = M.smoothStep(
            definition.heightOffset + 12f,
            definition.heightOffset + 42f,
            h
        )
        if (elevationMask < 0.55f) return false
        val flow = z * 0.0035f + sin(x * 0.0022f) * 1.15f
        val mainWave = abs(sin((x * 0.045f + sin(flow) * 1.6f).toDouble()).toFloat())
        val mainCrack = 1f - M.smoothStep(0.012f, 0.092f, mainWave)
        return mainCrack >= 0.5f
    }

    fun height(x: Float, z: Float): Float {
        var h = baseHeight(x, z)
        val distSq = x * x + z * z
        if (distSq < 90f * 90f) {
            val distance = kotlin.math.sqrt(distSq)
            val influence = 1f - M.smoothStep(26f, 88f, distance)
            h = M.lerp(h, padHeight, influence)
        }
        return h
    }

    fun normal(x: Float, z: Float, out: Vec3): Vec3 {
        val e = 1.5f
        val hl = height(x - e, z)
        val hr = height(x + e, z)
        val hd = height(x, z - e)
        val hu = height(x, z + e)
        out.set(hl - hr, 2f * e, hd - hu)
        out.normalize()
        return out
    }

    /** Range of heights to place the world's water plane at a sensible level. */
    fun groundAt(x: Float, z: Float): Float = height(x, z)
}
