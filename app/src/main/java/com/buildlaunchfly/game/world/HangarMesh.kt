package com.buildlaunchfly.game.world

import android.content.res.AssetManager
import com.buildlaunchfly.game.gfx.AnimalLoader
import com.buildlaunchfly.game.gfx.Material
import com.buildlaunchfly.game.gfx.Mesh
import com.buildlaunchfly.game.gfx.MeshBuilder
import com.buildlaunchfly.game.gfx.MeshData
import com.buildlaunchfly.game.gfx.TextureFactory
import com.buildlaunchfly.game.math.M
import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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

    /** Short upward ramp in front of the open end: the launch run-out. */
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

    /** Warm tan planks for the floor, the ramp and the props. */
    val floorMaterial = Material(
        albedo = textures.planks,
        tint = Vec3(0.95f, 0.81f, 0.63f),
        roughness = 0.90f,
        metallic = 0f,
        uvScale = 3.4f
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

        // Heavy door posts and header framing the open front.
        b.addBox(-HALF_WIDTH, WALL_HEIGHT * 0.5f, -HALF_LENGTH, 0.22f, WALL_HEIGHT * 0.5f, 0.22f, 2.4f)
        b.addBox(HALF_WIDTH, WALL_HEIGHT * 0.5f, -HALF_LENGTH, 0.22f, WALL_HEIGHT * 0.5f, 0.22f, 2.4f)
        b.addBox(0f, WALL_HEIGHT - 0.35f, -HALF_LENGTH, HALF_WIDTH + 0.25f, 0.26f, 0.24f, 2.4f)

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

        // Plank floor under the whole shed. The bench, stool, boxes and drum standing on it
        // are imported Furniture Kit models, see [props].
        b.addBox(0f, -0.09f, 0f, HALF_WIDTH, 0.09f, HALF_LENGTH, 3.4f)

        return b.build()
    }

    /**
     * The working clutter of the shed, out of the Kenney Furniture Kit (CC0). Its models carry
     * no texture, only flat `Kd` colours, so each is split by material the way the nature and
     * castle kits are and drawn with the shared white map tinted by its own colour.
     */
    private fun buildProps(manager: AssetManager?): List<HangarProp> {
        if (manager == null) return emptyList()

        val out = ArrayList<HangarProp>(PROP_SPECS.size)

        for (spec in PROP_SPECS) {
            val parts = AnimalLoader.load(
                manager,
                "models/furniture/${spec.name}.obj",
                "models/furniture/${spec.name}.mtl",
                spec.height
            )
            if (parts.isEmpty()) continue

            // One matrix per prop: it is kept by every part of that prop and drawn each frame.
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
        return out
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
     * The short launch ramp: about six metres of deck rising one metre, directly in front of
     * the open end so the aircraft runs out of the shed and up the slope.
     */
    private fun buildRampData(): MeshData {
        val b = MeshBuilder()

        val startZ = -HALF_LENGTH + 0.2f
        val endZ = startZ - RAMP_LENGTH
        val run = startZ - endZ
        val rise = RAMP_RISE
        val slope = atan2(rise, run)
        val deckLength = sqrt(run * run + rise * rise)
        val midZ = (startZ + endZ) * 0.5f
        val midY = rise * 0.5f

        // The deck itself: one board tilted up along its own +Z. A positive X rotation lifts
        // the far (-Z) end, which is the direction the aircraft launches.
        b.pushTrs(0f, midY, midZ, slope, 0f, 0f, 1f)
        b.addBox(0f, 0f, 0f, RAMP_HALF, 0.14f, deckLength * 0.5f, 3.0f)
        b.pop()

        // A second, deeper board under the deck gives the ramp a solid timber thickness.
        b.pushTrs(0f, midY - 0.42f, midZ, slope, 0f, 0f, 1f)
        b.addBox(0f, 0f, 0f, RAMP_HALF - 0.1f, 0.28f, deckLength * 0.46f, 2.6f)
        b.pop()

        // Cross beams stepping down to the ground under the slope.
        val steps = 4
        for (i in 0 until steps) {
            val t = (i + 0.5f) / steps
            val z = startZ + (endZ - startZ) * t
            val height = rise * t
            b.addBox(0f, height * 0.5f, z, RAMP_HALF - 0.2f, height * 0.5f, 0.11f, 2.4f)
        }

        // Low kerb rails down both edges, so the ramp reads as a built structure.
        for (side in intArrayOf(-1, 1)) {
            b.pushTrs(
                side * (RAMP_HALF - 0.12f), midY + 0.18f, midZ,
                slope, 0f, 0f, 1f
            )
            b.addBox(0f, 0f, 0f, 0.10f, 0.16f, deckLength * 0.5f, 2.4f)
            b.pop()
        }

        return b.build()
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
        /** Half the interior width; big enough for the full-scale rig and aircraft. */
        const val HALF_WIDTH = 5.30f

        /** Half the interior length; deep enough for the launch camera to sit inside. */
        const val HALF_LENGTH = 10.50f

        /** Eave height; the ridge sits two metres above it. */
        const val WALL_HEIGHT = 6.40f

        const val RIDGE_Y = 8.55f
        const val PITCH = 0.40f

        /** Half the length of one roof panel, measured along the slope. */
        const val PANEL_HALF = 3.10f
        const val TIE_Y = 6.30f

        /** Launch ramp: six metres of run, one metre of rise. */
        const val RAMP_LENGTH = 6.0f
        const val RAMP_RISE = 1.0f
        const val RAMP_HALF = 3.40f

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
