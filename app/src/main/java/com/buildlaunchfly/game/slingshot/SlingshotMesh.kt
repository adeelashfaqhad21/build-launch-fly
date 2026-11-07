package com.buildlaunchfly.game.slingshot

import com.buildlaunchfly.game.gfx.Material
import com.buildlaunchfly.game.gfx.Mesh
import com.buildlaunchfly.game.gfx.MeshBuilder
import com.buildlaunchfly.game.gfx.MeshData
import com.buildlaunchfly.game.gfx.TextureFactory
import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3
import kotlin.math.sqrt

/** Fixed local geometry of the launching rig. All values are relative to the pad origin. */
object SlingshotLayout {
    const val SEAT_HEIGHT = 3.05f
    // Wide, elevated anchor points create the unmistakable V-shaped sling silhouette.
    val LEFT_PRONG = Vec3(-3.05f, 4.18f, -0.18f)
    val RIGHT_PRONG = Vec3(3.05f, 4.18f, -0.18f)
    val HOLD_POINT = Vec3(0f, 3.05f, 0f)
    val BASE = Vec3(0f, 0f, 0f)
}

/**
 * Static slingshot frame plus two elastic bands drawn as unit cylinders whose model matrix
 * is rebuilt from the current draw position every frame.
 */
class SlingshotMesh(textures: TextureFactory) {

    val frameMesh: Mesh
    val bandMesh: Mesh
    val cradleMesh: Mesh
    val hydraulicRodMesh: Mesh

    val frameMaterial = Material(
        albedo = textures.metal,
        tint = Vec3(0.40f, 0.48f, 0.56f),
        roughness = 0.42f,
        metallic = 0.68f,
        uvScale = 2.2f
    )

    val cradleMaterial = Material(
        albedo = textures.metal,
        tint = Vec3(0.18f, 0.25f, 0.32f),
        roughness = 0.34f,
        metallic = 0.78f,
        uvScale = 1.5f
    )

    val hydraulicMaterial = Material(
        albedo = textures.metal,
        tint = Vec3(0.72f, 0.80f, 0.88f),
        roughness = 0.22f,
        metallic = 0.92f,
        uvScale = 2f
    )

    val bandMaterial = Material(
        albedo = textures.rubber,
        tint = Vec3(0.85f, 0.30f, 0.28f),
        roughness = 0.55f,
        metallic = 0f,
        uvScale = 1f
    )

    private val tempDir = Vec3()
    private val tempRight = Vec3()
    private val tempBack = Vec3()
    private val tempEye = Vec3()
    private val bandMatrixLeft = Mat4()
    private val bandMatrixRight = Mat4()

    init {
        frameMesh = Mesh("slingshotFrame", buildFrameData())
        bandMesh = Mesh("slingshotBand", buildBandData())
        cradleMesh = Mesh("slingshotCradle", buildCradleData())
        hydraulicRodMesh = Mesh("slingshotHydraulicRod", buildHydraulicRodData())
    }

    private fun buildFrameData(): MeshData {
        val b = MeshBuilder()

        // Broad, weighted launch deck with a raised service spine.
        b.addBox(0f, 0.14f, 0f, 3.45f, 0.16f, 1.55f, 2f)
        b.addBox(0f, 0.36f, 0.10f, 1.18f, 0.16f, 0.88f, 2f)
        b.addBox(0f, 0.58f, 0.72f, 0.52f, 0.08f, 0.58f, 2f)

        // Central lattice mast with four load-bearing columns.
        for (x in floatArrayOf(-0.34f, 0.34f)) {
            for (z in floatArrayOf(-0.30f, 0.30f)) {
                b.addBox(x, 1.82f, z, 0.075f, 1.52f, 0.075f, 2f)
                b.addBox(x * 1.55f, 0.42f, z * 1.55f, 0.16f, 0.12f, 0.16f, 2f)
            }
        }
        // X-bracing on both visible sides gives the mast a fabricated truss silhouette.
        for (z in floatArrayOf(-0.34f, 0.34f)) {
            b.pushTrs(0f, 0f, 0f, 0f, 0f, 0.62f, 1f)
            b.addBox(0f, 1.82f, z, 0.055f, 1.78f, 0.055f, 2f)
            b.pop()
            b.pushTrs(0f, 0f, 0f, 0f, 0f, -0.62f, 1f)
            b.addBox(0f, 1.82f, z, 0.055f, 1.78f, 0.055f, 2f)
            b.pop()
        }
        for (y in floatArrayOf(0.62f, 1.55f, 2.48f, 3.18f)) {
            b.addBox(0f, y, 0f, 0.48f, 0.065f, 0.47f, 2f)
        }

        // Elevated swept fork arms and reinforced crosshead form a wide V-shaped launcher.
        b.addBox(0f, 3.58f, -0.12f, 3.12f, 0.16f, 0.25f, 2f)
        b.pushTrs(0f, 0f, 0f, 0f, 0f, -0.17f, 1f)
        b.addBox(-1.58f, 3.88f, -0.12f, 1.72f, 0.17f, 0.22f, 2f)
        b.pop()
        b.pushTrs(0f, 0f, 0f, 0f, 0f, 0.17f, 1f)
        b.addBox(1.58f, 3.88f, -0.12f, 1.72f, 0.17f, 0.22f, 2f)
        b.pop()

        // Machined anchor clamps, prong caps and visible fastener bosses.
        b.addBox(-3.02f, 4.15f, -0.12f, 0.25f, 0.22f, 0.30f, 2f)
        b.addBox(3.02f, 4.15f, -0.12f, 0.25f, 0.22f, 0.30f, 2f)
        b.addSphere(-3.05f, 4.18f, -0.18f, 0.22f, 6, 10)
        b.addSphere(3.05f, 4.18f, -0.18f, 0.22f, 6, 10)

        // Hydraulic recoil housings and reinforced lower cross-member.
        b.addBox(0f, 0.72f, 0.72f, 2.35f, 0.14f, 0.18f, 2f)
        b.addCylinder(-1.48f, 0.96f, 0.82f, 0.12f, 0.12f, 1.42f, 12, true, true, 1f)
        b.addCylinder(1.48f, 0.96f, 0.82f, 0.12f, 0.12f, 1.42f, 12, true, true, 1f)

        // Rear diagonal stays transfer launch loads into the deck.
        b.pushTrs(0f, 0f, 0f, 0.38f, 0f, 0.18f, 1f)
        b.addCylinder(-1.12f, 1.55f, 0.95f, 0.11f, 0.10f, 2.75f, 8, false, false, 2f)
        b.pop()
        b.pushTrs(0f, 0f, 0f, 0.38f, 0f, -0.18f, 1f)
        b.addCylinder(1.12f, 1.55f, 0.95f, 0.11f, 0.10f, 2.75f, 8, false, false, 2f)
        b.pop()

        return b.build()
    }

    /** Aircraft carriage: padded cradle bed, raised side cheeks and a quick-release nose stop. */
    private fun buildCradleData(): MeshData {
        val b = MeshBuilder()
        b.addBox(0f, 0.10f, 0f, 0.78f, 0.10f, 1.02f, 2f)
        b.addBox(-0.66f, 0.25f, 0.05f, 0.10f, 0.20f, 0.76f, 2f)
        b.addBox(0.66f, 0.25f, 0.05f, 0.10f, 0.20f, 0.76f, 2f)
        b.addBox(0f, 0.28f, -0.88f, 0.48f, 0.18f, 0.12f, 2f)
        b.addBox(0f, 0.24f, 0.86f, 0.54f, 0.12f, 0.10f, 2f)
        return b.build()
    }

    /** Exposed chrome piston rod. Its visible extension is compressed during the draw. */
    private fun buildHydraulicRodData(): MeshData {
        val b = MeshBuilder()
        b.addCylinder(0f, 0.5f, 0f, 0.052f, 0.052f, 1f, 12, true, true, 2f)
        b.addCylinder(0f, 0.94f, 0f, 0.078f, 0.078f, 0.10f, 12, true, true, 2f)
        return b.build()
    }

    /** Unit cylinder from y=0 to y=1, radius 1, so a model matrix can stretch it. */
    private fun buildBandData(): MeshData {
        val b = MeshBuilder()
        b.addCylinder(0f, 0.5f, 0f, 0.045f, 0.045f, 1f, 6, false, false, 1f)
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

    /** Left band transform for the current draw offset. */
    fun leftBandMatrix(pull: Float, out: Mat4): Mat4 {
        val thickness = 1f + thicknessBoost
        return bandMatrix(SlingshotLayout.LEFT_PRONG, holdPoint(pull), thickness, out)
    }

    fun rightBandMatrix(pull: Float, out: Mat4): Mat4 {
        val thickness = 1f + thicknessBoost
        return bandMatrix(SlingshotLayout.RIGHT_PRONG, holdPoint(pull), thickness, out)
    }

    var thicknessBoost: Float = 0f

    fun holdPoint(pull: Float): Vec3 {
        val d = pull.coerceIn(0f, 1f) * DRAW_LENGTH
        return tempEye.set(
            SlingshotLayout.HOLD_POINT.x,
            SlingshotLayout.HOLD_POINT.y - d * 0.12f,
            SlingshotLayout.HOLD_POINT.z + d
        )
    }

    fun dispose() {
        frameMesh.dispose()
        bandMesh.dispose()
        cradleMesh.dispose()
        hydraulicRodMesh.dispose()
    }

    companion object {
        const val DRAW_LENGTH = 2.1f
        private val WORLD_UP = Vec3(0f, 1f, 0f)

        fun distanceBand(a: Vec3, b: Vec3): Float {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val dz = b.z - a.z
            return sqrt(dx * dx + dy * dy + dz * dz)
        }
    }
}
