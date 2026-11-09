package com.buildlaunchfly.game.slingshot

import com.buildlaunchfly.game.gfx.Material
import com.buildlaunchfly.game.gfx.Mesh
import com.buildlaunchfly.game.gfx.MeshBuilder
import com.buildlaunchfly.game.gfx.MeshData
import com.buildlaunchfly.game.gfx.TextureFactory
import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The reference slingshot, and nothing else: two wide squared timber supports standing either
 * side of the aircraft, and one elastic band strung between them.
 *
 * There is no ramp, no crossbar, no braces, no mast and no lattice work. The aircraft hangs on
 * the middle of the single band, which is why the band is drawn as one continuous curve from
 * tip to tip through the tow point rather than as two separate arms.
 *
 * All values are relative to the pad origin.
 */
object SlingshotLayout {

    /**
     * Height of the aircraft's fuselage centre above the hangar floor. The landing wheels sit
     * on the boards; the aircraft is not hung in the band.
     */
    const val SEAT_HEIGHT = 1.02f

    /** Where the parked aircraft stands inside the shed, ahead of the back wall and clear of
     * the ramp, which starts at the open front. */
    const val HOLD_Z = 3.0f

    /** Two wide posts, a little wider apart than the wingspan so the band clears the tips. */
    const val POLE_X = 4.00f

    /** Taller than the aircraft, as in the reference plates. */
    const val POLE_HEIGHT = 2.65f

    /** Half-width of a post: about a third of a metre across, a chunky timber support. */
    const val POLE_HALF = 0.17f

    /** Slight outward splay of the posts toward their tips. */
    const val POLE_SPLAY = 0.045f

    private const val TIP_X = POLE_X + 0.20f
    private const val TIP_Y = POLE_HEIGHT - 0.06f

    /** Horizontal half-span of a post at height [y]; the posts pivot at their feet. */
    fun postX(y: Float): Float = POLE_X + y * POLE_SPLAY

    /** Where the band is tied at the top of each post. */
    val LEFT_PRONG = Vec3(-TIP_X, TIP_Y, 0f)
    val RIGHT_PRONG = Vec3(TIP_X, TIP_Y, 0f)

    /**
     * The aircraft hangs a little behind the post plane, so the band runs forward from both
     * tips and gathers on the fuselage just behind the wing root.
     */
    const val ANCHOR_Y = -0.12f
    const val ANCHOR_Z = 0.30f

    val HOLD_POINT = Vec3(0f, SEAT_HEIGHT, HOLD_Z)
}

/**
 * Two wide posts and one elastic band.
 *
 * The band is a single unit cylinder stretched per segment along a sagging curve, so the
 * model matrix of every segment is rebuilt from the current draw position each frame and the
 * band stretches, droops and snaps back as one piece.
 */
class SlingshotMesh(textures: TextureFactory) {

    val frameMesh: Mesh
    val bandMesh: Mesh

    /** Weathered timber for the two supports. */
    val frameMaterial = Material(
        albedo = textures.bark,
        tint = Vec3(0.70f, 0.53f, 0.34f),
        roughness = 0.88f,
        metallic = 0f,
        uvScale = 2.2f
    )

    /** Yellow/cream warning-striped elastic; the texture carries both colours. */
    val bandMaterial = Material(
        albedo = textures.hazard,
        tint = Vec3(1f, 1f, 1f),
        roughness = 0.72f,
        metallic = 0f,
        uvScale = 1f
    )

    private val tempDir = Vec3()
    private val tempRight = Vec3()
    private val tempBack = Vec3()
    private val tempEye = Vec3()
    private val tempCradle = Vec3()
    private val armStart = Vec3()
    private val armEnd = Vec3()

    init {
        frameMesh = Mesh("slingshotFrame", buildFrameData())
        bandMesh = Mesh("slingshotBand", buildBandData())
    }

    /** Two wide squared posts with planted feet and a chamfered crown. */
    private fun buildFrameData(): MeshData {
        val b = MeshBuilder()
        for (side in intArrayOf(-1, 1)) {
            b.pushTrs(
                side * SlingshotLayout.POLE_X, 0f, 0f,
                0f, 0f, -side * SlingshotLayout.POLE_SPLAY,
                1f
            )
            // Foot block: the post is planted in the ground, not floating.
            b.addBox(
                0f, 0.09f, 0f,
                SlingshotLayout.POLE_HALF * 1.35f, 0.09f,
                SlingshotLayout.POLE_HALF * 1.35f, 2f
            )
            // The wide squared shaft itself.
            b.addBox(
                0f, SlingshotLayout.POLE_HEIGHT * 0.5f, 0f,
                SlingshotLayout.POLE_HALF, SlingshotLayout.POLE_HEIGHT * 0.5f,
                SlingshotLayout.POLE_HALF, 2f
            )
            // Chamfered crown so the timber reads as cut wood rather than a box.
            b.pushTrs(0f, SlingshotLayout.POLE_HEIGHT - 0.10f, 0f, 0f, 0f, 0f, 1f)
            b.addBox(
                0f, 0f, 0f,
                SlingshotLayout.POLE_HALF * 0.62f, 0.10f,
                SlingshotLayout.POLE_HALF * 0.62f, 2f
            )
            b.pop()
            b.pop()
        }
        return b.build()
    }

    /** Unit cylinder: radius 1 at the base, height 1, stretched to a segment by its matrix. */
    private fun buildBandData(): MeshData {
        val b = MeshBuilder()
        // Flat, broad elastic webbing rather than a round cable: the reference shows
        // striped straps spanning the aircraft's wing plane.
        b.addBox(
            0f, 0.5f, 0f,
            BAND_WIDTH * 0.5f, 0.5f, BAND_THICKNESS * 0.5f,
            BAND_STRIPE_TILES
        )
        return b.build()
    }

    /**
     * Fills [out] with one matrix per band segment. The band leaves the left tip, sags through
     * the tow point and climbs to the right tip: one continuous elastic, no separate arms.
     */
    fun bandMatrices(draw: Float, sag: Float, out: Array<Mat4>) {
        val thickness = 1f + thicknessBoost
        val cradle = cradlePoint(draw)
        var index = 0
        index = fillArm(SlingshotLayout.LEFT_PRONG, cradle, sag, thickness, out, index)
        index = fillArm(cradle, SlingshotLayout.RIGHT_PRONG, sag, thickness, out, index)
        // Any spare slot is parked on the origin so no stale transform is drawn.
        while (index < out.size) {
            out[index].identity()
            index++
        }
    }

    private fun fillArm(
        from: Vec3,
        to: Vec3,
        sag: Float,
        thickness: Float,
        out: Array<Mat4>,
        startIndex: Int
    ): Int {
        var index = startIndex
        val segments = SlingshotMesh.BAND_SEGMENTS_PER_ARM
        for (i in 0 until segments) {
            if (index >= out.size) return index
            val t0 = i.toFloat() / segments
            val t1 = (i + 1).toFloat() / segments
            curvePoint(from, to, t0, sag, armStart)
            curvePoint(from, to, t1, sag, armEnd)
            bandMatrix(armStart, armEnd, thickness, out[index])
            index++
        }
        return index
    }

    /** Point along the arm at [t] in 0…1, dropped by a half-sine so the band hangs. */
    private fun curvePoint(from: Vec3, to: Vec3, t: Float, sag: Float, out: Vec3): Vec3 {
        val s = sin(kotlin.math.PI.toFloat() * t)
        return out.set(
            from.x + (to.x - from.x) * t,
            from.y + (to.y - from.y) * t - sag * s,
            from.z + (to.z - from.z) * t
        )
    }

    /**
     * Matrix that maps the unit band cylinder onto the segment [from] -> [to]. The band's +Y
     * axis runs along the segment, so the matrix only has to stretch it.
     */
    fun bandMatrix(from: Vec3, to: Vec3, thickness: Float, out: Mat4): Mat4 {
        tempDir.set(to).sub(from)
        val length = tempDir.length().coerceAtLeast(0.0001f)
        tempDir.scale(1f / length)

        tempRight.cross(WORLD_UP, tempDir)
        if (tempRight.lengthSq() < 1e-6f) {
            tempRight.set(1f, 0f, 0f)
        }
        tempRight.normalize()
        tempBack.cross(tempRight, tempDir).normalize()

        out.m[0] = tempRight.x * thickness
        out.m[1] = tempRight.y * thickness
        out.m[2] = tempRight.z * thickness
        out.m[3] = 0f
        out.m[4] = tempDir.x * length
        out.m[5] = tempDir.y * length
        out.m[6] = tempDir.z * length
        out.m[7] = 0f
        out.m[8] = tempBack.x * thickness
        out.m[9] = tempBack.y * thickness
        out.m[10] = tempBack.z * thickness
        out.m[11] = 0f
        out.m[12] = from.x
        out.m[13] = from.y
        out.m[14] = from.z
        out.m[15] = 1f
        return out
    }

    /** Where the band meets the aircraft: the tow point at the wing root. */
    fun cradlePoint(pull: Float): Vec3 {
        val hold = holdPoint(pull)
        return tempCradle.set(
            hold.x,
            hold.y + SlingshotLayout.ANCHOR_Y,
            hold.z + SlingshotLayout.ANCHOR_Z
        )
    }

    var thicknessBoost: Float = 0f

    /** How far the band can be drawn back at full stretch; upgrades lengthen it. */
    var drawLength: Float = DRAW_LENGTH

    /**
     * Slow vertical breath of the parked aircraft, in metres. It is added to the hold point,
     * so the band travels with the fuselage instead of leaving it behind.
     */
    var idleBob: Float = 0f

    fun holdPoint(pull: Float): Vec3 {
        val d = pull.coerceIn(0f, 1f) * drawLength
        return tempEye.set(
            SlingshotLayout.HOLD_POINT.x,
            SlingshotLayout.HOLD_POINT.y - d * 0.10f + idleBob,
            SlingshotLayout.HOLD_POINT.z + d
        )
    }

    fun dispose() {
        frameMesh.dispose()
        bandMesh.dispose()
    }

    companion object {
        const val DRAW_LENGTH = 2.7f

        /** How far the band hangs below its chord when slack. */
        const val BAND_SAG = 0.22f

        /** Visible strap width and edge thickness before the upgrade multiplier. */
        private const val BAND_WIDTH = 0.28f
        private const val BAND_THICKNESS = 0.07f

        /** Segments per arm; enough to read as one smooth elastic, few enough to stay cheap. */
        const val BAND_SEGMENTS_PER_ARM = 4

        /** Total band segments drawn each frame: both arms. */
        const val BAND_SEGMENTS = BAND_SEGMENTS_PER_ARM * 2

        /** Yellow/cream stripe repeats along the band. */
        private const val BAND_STRIPE_TILES = 3f

        private val WORLD_UP = Vec3(0f, 1f, 0f)

        fun distanceBand(a: Vec3, b: Vec3): Float {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val dz = b.z - a.z
            return sqrt(dx * dx + dy * dy + dz * dz)
        }
    }
}
