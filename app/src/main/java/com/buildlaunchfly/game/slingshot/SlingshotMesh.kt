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
 * The reference slingshot, kept deliberately simple: two wooden poles planted slightly wider
 * apart than the aircraft's wingspan and one elastic band that runs from tip to tip, pulled
 * back through a small leather cradle. No ramp, no braces, no metal.
 *
 * All values are relative to the pad origin.
 */
object SlingshotLayout {
    const val SEAT_HEIGHT = 3.05f

    /** Poles sit just outside the 3.56 m wingspan so the band clears the wing tips. */
    const val POST_X = 2.10f
    const val POST_HEIGHT = 3.80f

    /** Inward lean of each pole, in radians. */
    const val POST_LEAN = 0.045f

    /** X of the pole tips after the lean. */
    val POST_TIP_X: Float = POST_X - sin(POST_LEAN) * POST_HEIGHT

    val LEFT_PRONG = Vec3(-POST_TIP_X, POST_HEIGHT - 0.06f, 0f)
    val RIGHT_PRONG = Vec3(POST_TIP_X, POST_HEIGHT - 0.06f, 0f)
    val HOLD_POINT = Vec3(0f, SEAT_HEIGHT, 0f)
    val BASE = Vec3(0f, 0f, 0f)
}

/**
 * Two simple poles, one continuous elastic band drawn as two segments from the pole tips to
 * the cradle, and the leather pad that holds the aircraft.
 *
 * The band segments are unit cylinders whose model matrix is rebuilt from the current draw
 * position every frame, so the band stretches and sags with the pull.
 */
class SlingshotMesh(textures: TextureFactory) {

    val frameMesh: Mesh
    val bandMesh: Mesh
    val pouchMesh: Mesh

    val frameMaterial = Material(
        albedo = textures.bark,
        tint = Vec3(0.66f, 0.48f, 0.32f),
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

    init {
        frameMesh = Mesh("slingshotFrame", buildFrameData())
        bandMesh = Mesh("slingshotBand", buildBandData())
        pouchMesh = Mesh("slingshotPouch", buildPouchData())
    }

    /** Two poles, each a single tapered post leaning in toward the cradle. */
    private fun buildFrameData(): MeshData {
        val b = MeshBuilder()
        for (side in intArrayOf(-1, 1)) {
            b.pushTrs(
                side * SlingshotLayout.POST_X, 0f, 0f,
                0f, 0f, -side * SlingshotLayout.POST_LEAN,
                1f
            )
            b.addCylinder(
                0f, SlingshotLayout.POST_HEIGHT * 0.5f, 0f,
                0.20f, 0.13f, SlingshotLayout.POST_HEIGHT,
                10, true, true, 2f
            )
            b.pop()
        }
        return b.build()
    }

    /** Unit cylinder from y=0 to y=1, radius 1, so a model matrix can stretch it. */
    private fun buildBandData(): MeshData {
        val b = MeshBuilder()
        b.addCylinder(0f, 0.5f, 0f, 0.09f, 0.09f, 1f, 8, false, false, BAND_STRIPE_TILES)
        return b.build()
    }

    /** Small flattened leather pad where the band meets the aircraft. */
    private fun buildPouchData(): MeshData {
        val b = MeshBuilder()
        b.addBox(0f, 0f, 0f, 0.30f, 0.055f, 0.19f, 1f)
        b.addBox(0f, -0.01f, 0.07f, 0.16f, 0.045f, 0.06f, 1f)
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

    /** Left half of the single band, as two segments hung from a sagging midpoint. */
    fun leftBandMatrices(draw: Float, sag: Float, outA: Mat4, outB: Mat4) {
        val thickness = 1f + thicknessBoost
        val from = SlingshotLayout.LEFT_PRONG
        val to = holdPoint(draw)
        sagPoint(from, to, sag, tempSag)
        bandMatrix(from, tempSag, thickness, outA)
        bandMatrix(tempSag, to, thickness, outB)
    }

    /** Right half of the single band. */
    fun rightBandMatrices(draw: Float, sag: Float, outA: Mat4, outB: Mat4) {
        val thickness = 1f + thicknessBoost
        val from = SlingshotLayout.RIGHT_PRONG
        val to = holdPoint(draw)
        sagPoint(from, to, sag, tempSag)
        bandMatrix(from, tempSag, thickness, outA)
        bandMatrix(tempSag, to, thickness, outB)
    }

    /** Chord midpoint between prong and cradle, dropped by [sag] metres. */
    fun sagPoint(from: Vec3, to: Vec3, sag: Float, out: Vec3): Vec3 {
        out.set(
            (from.x + to.x) * 0.5f,
            (from.y + to.y) * 0.5f - sag.coerceAtLeast(0f),
            (from.z + to.z) * 0.5f
        )
        return out
    }

    /** World-space matrix of the leather cradle pad. */
    fun pouchMatrix(pull: Float, out: Mat4): Mat4 {
        val hold = holdPoint(pull)
        out.identity().translate(hold.x, hold.y, hold.z)
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
    }

    companion object {
        const val DRAW_LENGTH = 2.1f

        /** How far the bands hang below their chord when fully slack. */
        const val BAND_SAG = 0.17f

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
