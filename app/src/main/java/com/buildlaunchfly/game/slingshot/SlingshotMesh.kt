package com.buildlaunchfly.game.slingshot

import com.buildlaunchfly.game.gfx.Material
import com.buildlaunchfly.game.gfx.Mesh
import com.buildlaunchfly.game.gfx.MeshBuilder
import com.buildlaunchfly.game.gfx.MeshData
import com.buildlaunchfly.game.gfx.TextureFactory
import com.buildlaunchfly.game.math.M
import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3
import kotlin.math.sqrt

/**
 * The reference slingshot: two plain squared wooden posts standing just wider than the
 * wingtips and splaying slightly outward, with one elastic sling tied to both tips and meeting
 * at a single tow point on the forward fuselage. No ramp, no crossbar, no metal.
 *
 * All values are relative to the pad origin.
 */
object SlingshotLayout {
    const val SEAT_HEIGHT = 3.05f

    /** Posts about 1.2× the 3.56 m wingspan apart, like the plate. */
    const val POLE_X = 2.15f

    /** Roughly the aircraft's nose-to-tail length. */
    const val POLE_HEIGHT = 3.15f
    const val POLE_HALF = 0.065f

    /** Slight outward splay of the posts toward their tips. */
    const val POLE_SPLAY = 0.05f

    private const val TIP_X = POLE_X + 0.16f
    private const val TIP_Y = POLE_HEIGHT - 0.04f

    /**
     * Horizontal half-span of a post at height [y]. The posts pivot at their feet, so a point
     * part-way up sits slightly inboard of the tip.
     */
    fun postX(y: Float): Float = POLE_X + y * POLE_SPLAY

    /** Where the elastic is tied at the top of each post. */
    val LEFT_PRONG = Vec3(-TIP_X, TIP_Y, 0f)
    val RIGHT_PRONG = Vec3(TIP_X, TIP_Y, 0f)

    /**
     * The sling meets the aircraft just behind the wing root, so from the rear camera the two
     * arms overlay the wings and meet visibly on the fuselage centre line, like the plate.
     */
    const val ANCHOR_Y = -0.10f
    const val ANCHOR_Z = 0.25f

    val HOLD_POINT = Vec3(0f, SEAT_HEIGHT, 0f)
}

/**
 * Two squared posts, one elastic sling drawn as two arms from the post tips to the tow point at
 * the wing root, and the small leather pad on the fuselage.
 *
 * The band segments are unit cylinders whose model matrix is rebuilt from the current draw
 * position every frame, so the sling stretches and sags with the pull.
 */
class SlingshotMesh(textures: TextureFactory) {

    val frameMesh: Mesh
    val bandMesh: Mesh
    val pouchMesh: Mesh
    val wrapMesh: Mesh

    val frameMaterial = Material(
        albedo = textures.bark,
        tint = Vec3(0.72f, 0.55f, 0.36f),
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

    /** Dark leather cradle pad at the centre of the band. */
    val pouchMaterial = Material(
        albedo = textures.rubber,
        tint = Vec3(0.45f, 0.30f, 0.20f),
        roughness = 0.80f,
        metallic = 0f,
        uvScale = 1f
    )

    private val tempDir = Vec3()
    private val tempRight = Vec3()
    private val tempBack = Vec3()
    private val tempEye = Vec3()
    private val tempSag = Vec3()
    private val tempCradle = Vec3()

    init {
        frameMesh = Mesh("slingshotFrame", buildFrameData())
        bandMesh = Mesh("slingshotBand", buildBandData())
        pouchMesh = Mesh("slingshotPouch", buildPouchData())
        wrapMesh = Mesh("slingshotWrap", buildWrapData())
    }

    /** Two squared posts with planted feet, chamfered tips and a rounded nock. */
    private fun buildFrameData(): MeshData {
        val b = MeshBuilder()
        for (side in intArrayOf(-1, 1)) {
            b.pushTrs(
                side * SlingshotLayout.POLE_X, 0f, 0f,
                0f, 0f, -side * SlingshotLayout.POLE_SPLAY,
                1f
            )
            // Foot pad: the post is planted, not floating.
            b.addBox(
                0f, 0.045f, 0f,
                SlingshotLayout.POLE_HALF * 1.7f, 0.045f,
                SlingshotLayout.POLE_HALF * 1.7f, 2f
            )
            // Post.
            b.addBox(
                0f, SlingshotLayout.POLE_HEIGHT * 0.5f, 0f,
                SlingshotLayout.POLE_HALF, SlingshotLayout.POLE_HEIGHT * 0.5f,
                SlingshotLayout.POLE_HALF, 2f
            )
            // Chamfered cap and the rounded nock the elastic rides over.
            b.pushTrs(0f, SlingshotLayout.POLE_HEIGHT - 0.19f, 0f, 0f, 0f, 0f, 1f)
            b.addBox(0f, 0f, 0f, SlingshotLayout.POLE_HALF * 0.45f, 0.10f, SlingshotLayout.POLE_HALF * 0.45f, 2f)
            b.pop()
            b.addBox(
                0f, SlingshotLayout.POLE_HEIGHT - 0.06f, 0f,
                SlingshotLayout.POLE_HALF * 1.45f, 0.075f,
                SlingshotLayout.POLE_HALF * 1.45f, 2f
            )
            b.pop()
        }
        return b.build()
    }

    /** Unit cylinder from y=0 to y=1, radius 1, so a model matrix can stretch it. */
    private fun buildBandData(): MeshData {
        val b = MeshBuilder()
        b.addCylinder(0f, 0.5f, 0f, BAND_RADIUS, BAND_RADIUS, 1f, 10, false, false, BAND_STRIPE_TILES)
        return b.build()
    }

    /** Leather cradle: a plate, two upturned flaps and a strap over the fuselage. */
    private fun buildPouchData(): MeshData {
        val b = MeshBuilder()
        b.addBox(0f, 0f, 0f, 0.20f, 0.05f, 0.15f, 1f)
        b.pushTrs(0.17f, 0.055f, 0f, 0f, 0f, -0.55f, 1f)
        b.addBox(0f, 0f, 0f, 0.09f, 0.035f, 0.13f, 1f)
        b.pop()
        b.pushTrs(-0.17f, 0.055f, 0f, 0f, 0f, 0.55f, 1f)
        b.addBox(0f, 0f, 0f, 0.09f, 0.035f, 0.13f, 1f)
        b.pop()
        b.addBox(0f, 0.055f, 0f, 0.05f, 0.02f, 0.17f, 1f)
        return b.build()
    }

    /** Leather lashings where the elastic is tied around each post tip. */
    private fun buildWrapData(): MeshData {
        val b = MeshBuilder()
        for (side in intArrayOf(-1, 1)) {
            for (i in 0 until 3) {
                // Three collars binding the elastic just under the nock.
                val y = SlingshotLayout.POLE_HEIGHT - 0.36f + i * 0.07f
                b.pushTrs(
                    side * SlingshotLayout.postX(y), y, 0f,
                    0f, 0f, M.HALF_PI, 1f
                )
                b.addCylinder(0f, 0f, 0f, WRAP_RADIUS, WRAP_RADIUS, 0.05f, 8, true, true, 1f)
                b.pop()
            }
        }
        return b.build()
    }

    /**
     * Builds a matrix that maps the unit band cylinder onto the segment [from] -> [to].
     * The band's +Y axis runs along the segment, so the matrix only needs to stretch Y.
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

    /** Left arm of the sling, as two segments hung from a sagging midpoint. */
    fun leftBandMatrices(draw: Float, sag: Float, outA: Mat4, outB: Mat4) {
        val thickness = 1f + thicknessBoost
        val from = SlingshotLayout.LEFT_PRONG
        val to = cradlePoint(draw)
        sagPoint(from, to, sag, tempSag)
        bandMatrix(from, tempSag, thickness, outA)
        bandMatrix(tempSag, to, thickness, outB)
    }

    /** Right arm of the sling. */
    fun rightBandMatrices(draw: Float, sag: Float, outA: Mat4, outB: Mat4) {
        val thickness = 1f + thicknessBoost
        val from = SlingshotLayout.RIGHT_PRONG
        val to = cradlePoint(draw)
        sagPoint(from, to, sag, tempSag)
        bandMatrix(from, tempSag, thickness, outA)
        bandMatrix(tempSag, to, thickness, outB)
    }

    /** Chord midpoint between tip and cradle, dropped by [sag] metres. */
    fun sagPoint(from: Vec3, to: Vec3, sag: Float, out: Vec3): Vec3 {
        out.set(
            (from.x + to.x) * 0.5f,
            (from.y + to.y) * 0.5f - sag.coerceAtLeast(0f),
            (from.z + to.z) * 0.5f
        )
        return out
    }

    /** Where the sling meets the aircraft: the tow point at the wing root. */
    fun cradlePoint(pull: Float): Vec3 {
        val hold = holdPoint(pull)
        return tempCradle.set(
            hold.x,
            hold.y + SlingshotLayout.ANCHOR_Y,
            hold.z + SlingshotLayout.ANCHOR_Z
        )
    }

    /** World-space matrix of the leather cradle pad. */
    fun pouchMatrix(pull: Float, out: Mat4): Mat4 {
        val cradle = cradlePoint(pull)
        out.identity().translate(cradle.x, cradle.y, cradle.z)
        return out
    }

    var thicknessBoost: Float = 0f

    /** Rendered draw length at full stretch; the frame upgrades change this. */
    var drawLength: Float = DRAW_LENGTH

    fun holdPoint(pull: Float): Vec3 {
        val d = pull.coerceIn(0f, 1f) * drawLength
        return tempEye.set(
            SlingshotLayout.HOLD_POINT.x,
            SlingshotLayout.HOLD_POINT.y - d * 0.12f,
            SlingshotLayout.HOLD_POINT.z + d
        )
    }

    fun dispose() {
        frameMesh.dispose()
        bandMesh.dispose()
        pouchMesh.dispose()
        wrapMesh.dispose()
    }

    companion object {
        const val DRAW_LENGTH = 2.1f

        /** How far the bands hang below their chord when fully slack. */
        const val BAND_SAG = 0.17f

        /** Radius of the band cylinder before the tension/upgrade thickness scale. */
        private const val BAND_RADIUS = 0.075f

        /** Radius of a lashing collar, which has to clear the squared post. */
        private const val WRAP_RADIUS = 0.115f

        /** Yellow/cream stripe repeats along a band; the texture carries four per tile. */
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
