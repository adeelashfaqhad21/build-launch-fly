package com.buildlaunchfly.game.world

import android.content.res.AssetManager
import com.buildlaunchfly.game.gfx.AnimalLoader
import com.buildlaunchfly.game.gfx.Material
import com.buildlaunchfly.game.gfx.Mesh
import com.buildlaunchfly.game.gfx.MeshBuilder
import com.buildlaunchfly.game.gfx.MeshData
import com.buildlaunchfly.game.gfx.TextureFactory
import com.buildlaunchfly.game.gfx.SurfaceKind
import com.buildlaunchfly.game.math.M
import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow

/**
 * The hangar from the reference plates: a big timber shed the aircraft is launched out of.
 *
 * Reading the plates: the player stands inside a working shed. A dark sawn-plank wall runs
 * down the left, the right side is open to the sky between heavy posts, the roof is a pitched
 * gable carried on exposed rafters and tie beams, the floor is warm planks, and the clutter of
 * a working airfield sits along the walls. The aircraft faces the open front and is launched
 * out through it, over a short ramp, into the valley.
 *
 * Built in metres relative to the pad: forward is -Z, so the camera looks in from +Z and out
 * through the open front toward the valley.
 */
class HangarMesh(
    private val textures: TextureFactory,
    private val assets: AssetManager? = null
) {

    /** One material group of an imported prop, with where it stands in the shed. */
    class HangarProp(val mesh: Mesh, val material: Material, val matrix: Mat4)

    /** Plank wall, open-side posts and the front door frame. */
    val wallMesh: Mesh

    /** Gable panels, ridge, rafters, ties and purlins. */
    val roofMesh: Mesh

    /** Plank floor. The furniture standing on it is imported, see [props]. */
    val floorMesh: Mesh

    /** Hanging lamp and its shade. */
    val metalMesh: Mesh

    /** Broad tapered launch deck centred on the aircraft's -Z flight axis. */
    val rampMesh: Mesh

    /** Kenney Furniture Kit props standing inside the shed, one entry per material group. */
    val props: List<HangarProp>

    /** Weathered dark planks for the walls. */
    val wallMaterial = Material(
        albedo = textures.planks,
        tint = Vec3(0.52f, 0.45f, 0.37f),
        roughness = 0.94f,
        metallic = 0f,
        uvScale = 3.2f
    )

    /** Warm tan planks for the hangar floor and furniture. */
    val floorMaterial = Material(
        albedo = textures.planks,
        tint = Vec3(0.95f, 0.81f, 0.63f),
        roughness = 0.90f,
        metallic = 0f,
        uvScale = 3.4f
    )

    /** Neutral charcoal asphalt, deliberately distinct from every timber surface. */
    val rampMaterial = Material(
        albedo = textures.asphalt,
        tint = Vec3(0.92f, 0.92f, 0.92f),
        roughness = 0.96f,
        metallic = 0f,
        uvScale = 5.5f
    )

    /** Exposed timber structure: ridge, rafters, ties, studs and the door frame. */
    val roofMaterial = Material(
        albedo = textures.bark,
        tint = Vec3(0.42f, 0.31f, 0.22f),
        roughness = 0.90f,
        metallic = 0f,
        uvScale = 2.6f
    )

    /** Lamp and shade. */
    val metalMaterial = Material(
        albedo = textures.metal,
        tint = Vec3(0.34f, 0.33f, 0.32f),
        roughness = 0.60f,
        metallic = 0.4f,
        uvScale = 1f
    )

    init {
        wallMesh = Mesh("hangarWall", buildWallData())
        roofMesh = Mesh("hangarRoof", buildRoofData())
        floorMesh = Mesh("hangarFloor", buildFloorData())
        metalMesh = Mesh("hangarMetal", buildMetalData())
        rampMesh = Mesh("hangarRamp", buildRampData())
        props = buildProps(assets)
    }

    private fun buildWallData(): MeshData {
        val b = MeshBuilder()

        // Plank wall down the left side.
        b.addBox(-HALF_WIDTH, WALL_HEIGHT * 0.5f, 0f, 0.16f, WALL_HEIGHT * 0.5f, HALF_LENGTH, 3.2f)

        // Back wall closing the far end.
        b.addBox(0f, WALL_HEIGHT * 0.5f, HALF_LENGTH, HALF_WIDTH + 0.16f, WALL_HEIGHT * 0.5f, 0.16f, 3.2f)

        // Vertical studs on the inside face of the left wall.
        var z = -HALF_LENGTH + 1.4f
        while (z <= HALF_LENGTH - 0.6f) {
            b.addBox(-HALF_WIDTH + 0.18f, WALL_HEIGHT * 0.5f, z, 0.10f, WALL_HEIGHT * 0.5f, 0.10f, 2.4f)
            z += 2.6f
        }

        // Open right side: heavy posts holding the roof with the sky showing between them.
        for (pz in floatArrayOf(-HALF_LENGTH, -HALF_LENGTH * 0.5f, 0f, HALF_LENGTH * 0.5f, HALF_LENGTH)) {
            b.addBox(HALF_WIDTH, WALL_HEIGHT * 0.5f, pz, 0.17f, WALL_HEIGHT * 0.5f, 0.17f, 2.4f)
        }

        // Heavy door posts and header frame the exact 13.5 m × 8.5 m open front.
        b.addBox(-OPEN_DOOR_HALF_WIDTH, OPEN_DOOR_HEIGHT * 0.5f, -HALF_LENGTH,
            0.22f, OPEN_DOOR_HEIGHT * 0.5f, 0.22f, 2.4f)
        b.addBox(OPEN_DOOR_HALF_WIDTH, OPEN_DOOR_HEIGHT * 0.5f, -HALF_LENGTH,
            0.22f, OPEN_DOOR_HEIGHT * 0.5f, 0.22f, 2.4f)
        b.addBox(0f, OPEN_DOOR_HEIGHT - 0.18f, -HALF_LENGTH,
            OPEN_DOOR_HALF_WIDTH + 0.25f, 0.18f, 0.24f, 2.4f)

        return b.build()
    }

    private fun buildRoofData(): MeshData {
        val b = MeshBuilder()

        // Two gable panels running the whole length, hinged at the ridge and overhanging the
        // walls. The panel centre sits half a panel out from the ridge along the slope.
        val pitch = PITCH
        val panelX = PANEL_HALF * cos(pitch)
        val panelY = RIDGE_Y - PANEL_HALF * sin(pitch)
        b.pushTrs(-panelX, panelY, 0f, 0f, 0f, pitch, 1f)
        b.addBox(0f, 0f, 0f, PANEL_HALF, 0.11f, HALF_LENGTH + 0.5f, 2.8f)
        b.pop()
        b.pushTrs(panelX, panelY, 0f, 0f, 0f, -pitch, 1f)
        b.addBox(0f, 0f, 0f, PANEL_HALF, 0.11f, HALF_LENGTH + 0.5f, 2.8f)
        b.pop()

        // Ridge beam along the peak.
        b.addBox(0f, RIDGE_Y - 0.10f, 0f, 0.13f, 0.13f, HALF_LENGTH + 0.5f, 2.4f)

        // Rafters and tie beams every two metres, the ribs of the shed.
        var z = -HALF_LENGTH
        while (z <= HALF_LENGTH + 0.1f) {
            b.pushTrs(-panelX, panelY - 0.14f, z, 0f, 0f, pitch, 1f)
            b.addBox(0f, 0f, 0f, PANEL_HALF - 0.1f, 0.11f, 0.11f, 2.4f)
            b.pop()
            b.pushTrs(panelX, panelY - 0.14f, z, 0f, 0f, -pitch, 1f)
            b.addBox(0f, 0f, 0f, PANEL_HALF - 0.1f, 0.11f, 0.11f, 2.4f)
            b.pop()
            b.addBox(0f, TIE_Y, z, HALF_WIDTH - 0.1f, 0.10f, 0.11f, 2.4f)
            z += 2.0f
        }

        // Purlins under the panels.
        b.addBox(-HALF_WIDTH * 0.62f, TIE_Y + 0.55f, 0f, 0.09f, 0.09f, HALF_LENGTH + 0.5f, 2.4f)
        b.addBox(HALF_WIDTH * 0.62f, TIE_Y + 0.55f, 0f, 0.09f, 0.09f, HALF_LENGTH + 0.5f, 2.4f)

        return b.build()
    }

    private fun buildFloorData(): MeshData {
        val b = MeshBuilder()

        // Keep the plank floor clear of the asphalt runway. A full-width floor slab
        // underneath the deck caused coplanar overlap at the entrance and visible timber
        // beneath the runway in the first hangar section.
        // Rear workshop floor behind the slingshot plane (z = 0…+13).
        b.addBox(
            0f, -0.09f, HALF_LENGTH * 0.5f,
            HALF_WIDTH, 0.09f, HALF_LENGTH * 0.5f, 3.4f
        )
        // Forward side walkways leave the central 9 m runway footprint open (z = -13…0).
        val sideHalfWidth = (HALF_WIDTH - RAMP_WIDTH * 0.5f) * 0.5f
        val sideCenterX = RAMP_WIDTH * 0.25f + sideHalfWidth
        b.addBox(
            -sideCenterX, -0.09f, -HALF_LENGTH * 0.5f,
            sideHalfWidth, 0.09f, HALF_LENGTH * 0.5f, 3.4f
        )
        b.addBox(
            sideCenterX, -0.09f, -HALF_LENGTH * 0.5f,
            sideHalfWidth, 0.09f, HALF_LENGTH * 0.5f, 3.4f
        )

        return b.build()
    }

    /**
     * The working clutter of the shed, out of the Kenney Furniture Kit (CC0). Its models carry
     * no texture, only flat `Kd` colours, so each is split by material the way the nature and
     * castle kits are and drawn with the shared white map tinted by its own colour.
     */
    private fun buildProps(manager: AssetManager?): List<HangarProp> {
        val out = ArrayList<HangarProp>(PROP_SPECS.size + 1)

        if (manager != null) {
            for (spec in PROP_SPECS) {
                val parts = AnimalLoader.load(
                    manager,
                    "models/furniture/${spec.name}.obj",
                    "models/furniture/${spec.name}.mtl",
                    spec.height
                )
                if (parts.isEmpty()) continue

                val matrix = Mat4()
                matrix.setTrs(spec.x, 0f, spec.z, 0f, spec.heading, 0f, 1f)

                for ((index, part) in parts.withIndex()) {
                    val mesh = Mesh("hangarProp${spec.name}$index", part.data)
                    val material = Material(
                        albedo = textures.white,
                        tint = part.color,
                        roughness = 0.85f,
                        metallic = 0f,
                        uvScale = 1f
                    )
                    out.add(HangarProp(mesh, material, matrix))
                }
            }
        }

        // The six bale positions are taken directly from the Kotlin reference layout.
        val hayMatrix = Mat4()
        hayMatrix.identity()
        val hayMaterial = Material(
            albedo = textures.surface(SurfaceKind.SAND),
            tint = Vec3(1.0f, 0.82f, 0.43f),
            roughness = 0.99f,
            metallic = 0f,
            uvScale = 2.2f
        )
        out.add(HangarProp(Mesh("hangarHayBales", buildHayBalesData()), hayMaterial, hayMatrix))
        return out
    }

    private fun buildHayBalesData(): MeshData {
        val b = MeshBuilder()
        val positions = arrayOf(
            Vec3(-6.2f, 0.4f, -4.0f),
            Vec3(-5.8f, 0.4f, -2.5f),
            Vec3(-6.0f, 1.2f, -3.2f),
            Vec3(6.0f, 0.4f, -3.5f),
            Vec3(5.8f, 1.2f, -3.0f),
            Vec3(6.2f, 0.4f, -1.8f)
        )
        for (p in positions) {
            b.addBox(p.x, p.y, p.z, 0.65f, 0.40f, 0.45f, 2.0f)
        }
        return b.build()
    }

    private fun buildMetalData(): MeshData {
        val b = MeshBuilder()

        // Lamp rod from the ridge, shade opening downward, bulb inside.
        b.addCylinder(0f, RIDGE_Y - 1.1f, 0f, 0.035f, 0.035f, 1.6f, 6, false, false, 1f)
        b.pushTrs(0f, RIDGE_Y - 1.95f, 0f, M.PI.toFloat(), 0f, 0f, 1f)
        b.addCone(0f, 0f, 0f, 0.55f, 0.45f, 12, 1f)
        b.pop()
        b.addSphere(0f, RIDGE_Y - 2.10f, 0f, 0.12f, 5, 8)

        return b.build()
    }

    /**
     * Full-width forward launch deck from the Kotlin reference. The aircraft faces -Z;
     * the 9 m asphalt surface starts at the slingshot plane and rises smoothly for 48 m.
     */
    private fun buildRampData(): MeshData {
        val b = MeshBuilder()
        val halfWidth = RAMP_WIDTH * 0.5f

        // Tessellated top surface follows the reference cubic elevation profile rather than
        // one flat wedge, so the 48 m asphalt run rises smoothly from the hangar floor.
        for (i in 0 until RAMP_SEGMENTS) {
            val t0 = i.toFloat() / RAMP_SEGMENTS
            val t1 = (i + 1).toFloat() / RAMP_SEGMENTS
            val z0 = RAMP_START_Z + (RAMP_END_Z - RAMP_START_Z) * t0
            val z1 = RAMP_START_Z + (RAMP_END_Z - RAMP_START_Z) * t1
            val y0 = rampElevation(t0)
            val y1 = rampElevation(t1)

            b.addQuad(
                Vec3(-halfWidth, y0, z0),
                Vec3(halfWidth, y0, z0),
                Vec3(halfWidth, y1, z1),
                Vec3(-halfWidth, y1, z1),
                5f
            )

            // Solid side aprons close the raised deck down to ground level.
            b.addQuad(
                Vec3(-halfWidth, 0.02f, z0),
                Vec3(-halfWidth, y0, z0),
                Vec3(-halfWidth, y1, z1),
                Vec3(-halfWidth, 0.02f, z1),
                4f
            )
            b.addQuad(
                Vec3(halfWidth, 0.02f, z0),
                Vec3(halfWidth, 0.02f, z1),
                Vec3(halfWidth, y1, z1),
                Vec3(halfWidth, y0, z0),
                4f
            )
        }

        // Wooden edge curbs run the full 48 m length on both sides of the asphalt.
        for (side in intArrayOf(-1, 1)) {
            val x = side * (halfWidth + RAMP_CURB_WIDTH * 0.5f)
            b.addRod(
                x, rampElevation(0f) + 0.16f, RAMP_START_Z,
                x, rampElevation(1f) + 0.16f, RAMP_END_Z,
                RAMP_CURB_WIDTH * 0.5f, 8
            )
        }

        // Structural bearers and cross-ties support the long incline below the asphalt deck.
        for (side in intArrayOf(-1, 1)) {
            val x = side * (halfWidth - 0.22f)
            b.addRod(
                x, 0.08f, RAMP_START_Z,
                x, rampElevation(1f) - 0.12f, RAMP_END_Z,
                0.13f, 8
            )
        }
        for (t in floatArrayOf(0.20f, 0.40f, 0.60f, 0.80f)) {
            val z = RAMP_START_Z + (RAMP_END_Z - RAMP_START_Z) * t
            val y = rampElevation(t)
            b.addRod(-halfWidth + 0.18f, y - 0.12f, z,
                halfWidth - 0.18f, y - 0.12f, z, 0.09f, 8)
        }
        return b.build()
    }

    /**
     * Ramp elevation above the pad in hangar-local coordinates. The caller must add the
     * pad's world-space Y before comparing this with the world's terrain height.
     * Returns null outside the asphalt footprint.
     */
    fun rampHeightAt(localX: Float, localZ: Float): Float? {
        if (kotlin.math.abs(localX) > RAMP_WIDTH * 0.5f) return null
        if (localZ > RAMP_START_Z || localZ < RAMP_END_Z) return null
        val t = ((RAMP_START_Z - localZ) / (RAMP_START_Z - RAMP_END_Z))
            .coerceIn(0f, 1f)
        return rampElevation(t)
    }

    private fun rampElevation(t: Float): Float {
        val u = t.coerceIn(0f, 1f)
        return RAMP_ELEVATION *
            (1.15f * u.pow(1.4f) - 0.15f * u.pow(2.8f)).coerceIn(0f, 1f)
    }

    fun dispose() {
        wallMesh.dispose()
        roofMesh.dispose()
        floorMesh.dispose()
        metalMesh.dispose()
        rampMesh.dispose()
        for (prop in props) prop.mesh.dispose()
    }

    companion object {
        /** Reference interior dimensions: 16 m wide, 26 m deep, 9.5 m to the ridge. */
        const val HALF_WIDTH = 8.0f
        const val HALF_LENGTH = 13.0f
        const val WALL_HEIGHT = 7.8f
        const val RIDGE_Y = 9.5f
        const val PITCH = 0.40f
        const val PANEL_HALF = 4.25f
        const val TIE_Y = 6.7f

        /** Reference open doorway: 13.5 m wide and 8.5 m high. */
        const val OPEN_DOOR_HALF_WIDTH = 6.75f
        const val OPEN_DOOR_HEIGHT = 8.5f

        /** Reference runway starts at the slingshot plane and runs forward along -Z. */
        private const val RAMP_START_Z = 0.0f
        private const val RAMP_END_Z = -48.0f
        private const val RAMP_WIDTH = 9.0f
        private const val RAMP_ELEVATION = 4.2f
        private const val RAMP_CURB_WIDTH = 0.5f
        private const val RAMP_SEGMENTS = 32

        /** One imported prop: model name, world height, plan position and heading. */
        private class PropSpec(
            val name: String,
            val height: Float,
            val x: Float,
            val z: Float,
            val heading: Float
        )

        /**
         * The Furniture Kit props that stand on the floor: a workbench and stool along the back
         * wall, a bench against the left planks, two boxes by the open side and a drum near the
         * front right.
         */
        private val PROP_SPECS = listOf(
            PropSpec("table", 0.95f, 0f, HALF_LENGTH - 1.6f, 0f),
            PropSpec("stoolBar", 0.75f, 1.7f, HALF_LENGTH - 2.3f, 0.6f),
            PropSpec("bench", 0.90f, -HALF_WIDTH + 1.6f, 2.0f, 1.5708f),
            PropSpec("cardboardBoxClosed", 0.80f, HALF_WIDTH - 1.2f, HALF_LENGTH - 2.0f, 0.2f),
            PropSpec("cardboardBoxClosed", 0.55f, HALF_WIDTH - 1.2f, HALF_LENGTH - 2.9f, -0.4f),
            PropSpec("trashcan", 1.00f, HALF_WIDTH - 1.2f, -HALF_LENGTH + 2.6f, 0f)
        )
    }
}
