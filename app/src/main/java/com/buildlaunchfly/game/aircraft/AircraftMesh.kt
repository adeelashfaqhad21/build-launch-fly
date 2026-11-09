package com.buildlaunchfly.game.aircraft

import android.content.res.AssetManager
import com.buildlaunchfly.game.gfx.Material
import com.buildlaunchfly.game.gfx.Mesh
import com.buildlaunchfly.game.gfx.MeshBuilder
import com.buildlaunchfly.game.gfx.MeshData
import com.buildlaunchfly.game.gfx.TextureFactory
import com.buildlaunchfly.game.math.M
import com.buildlaunchfly.game.math.Vec3
import com.buildlaunchfly.game.world.WorldId

/**
 * Procedural vintage biplane from docs/DESIGN.md §3.14: tan doped canvas wings with
 * orange-brown rib lines, a narrow timber lattice fuselage, dark struts and bracing
 * wires, a green two-blade propeller and two dark wheels.
 *
 * Local axes: forward = -Z, up = +Y, right = +X. 1 unit = 1 metre.
 *
 * Build stages: 1 fuselage (lattice body, cockpit, wheels) → 2 left wing pair → 3 right
 * wing pair → 4 tail assembly → 5 propeller → 6 engine block and Mk II surfaces.
 *
 * The geometry is split across meshes by material, so every surface keeps a flat, matte
 * look and the whole aircraft costs a handful of draw calls.
 */
class AircraftMesh(
    private val textures: TextureFactory,
    private val assets: AssetManager? = null
) {

    var canvasMesh: Mesh? = null
        private set
    var ribMesh: Mesh? = null
        private set
    var woodMesh: Mesh? = null
        private set
    var metalMesh: Mesh? = null
        private set
    var canopyMesh: Mesh? = null
        private set
    var propellerMesh: Mesh? = null
        private set
    var propellerBlurMesh: Mesh? = null
        private set

    /** One material group of the imported airframe, with the colour it is drawn in. */
    class ImportedPart(val mesh: Mesh, val material: Material)

    /**
     * The imported biplane ("samolet", CC BY 4.0), split by its flat material colours. When
     * it loads it replaces the procedural body; the procedural meshes stay built so a missing
     * asset can never leave the hangar empty.
     */
    var importedParts: List<ImportedPart> = emptyList()
        private set

    /** True when the imported airframe loaded and should be drawn instead of the built one. */
    val hasImportedModel: Boolean get() = importedParts.isNotEmpty()

    /** Tan doped canvas, flat-shaded (roughness 0.92, no metallic). */
    val canvasMaterial = Material(
        albedo = textures.white,
        tint = Vec3(0.86f, 0.58f, 0.33f),
        roughness = 0.92f,
        metallic = 0f,
        uvScale = 1f
    )

    /** Orange-brown rib lines stitched across every fabric surface. */
    val ribMaterial = Material(
        albedo = textures.white,
        tint = Vec3(0.60f, 0.34f, 0.17f),
        roughness = 0.95f,
        metallic = 0f,
        uvScale = 1f
    )

    /** Timber fuselage, longerons, struts and landing-gear legs. */
    val woodMaterial = Material(
        albedo = textures.bark,
        tint = Vec3(0.55f, 0.40f, 0.26f),
        roughness = 0.95f,
        metallic = 0f,
        uvScale = 2.2f
    )

    /** Graphite metal: engine, wires, wheel hubs and the cockpit opening. */
    val metalMaterial = Material(
        albedo = textures.metal,
        tint = Vec3(0.26f, 0.27f, 0.29f),
        roughness = 0.62f,
        metallic = 0.35f,
        uvScale = 1.4f
    )

    /** Thin cockpit glazing at 75% opacity. */
    val canopyMaterial = Material(
        albedo = textures.metal,
        tint = Vec3(0.30f, 0.40f, 0.38f),
        roughness = 0.18f,
        metallic = 0f,
        uvScale = 1f,
        emissive = 0.02f,
        alpha = 0.75f
    )

    /** Green two-blade wooden propeller. */
    val propellerMaterial = Material(
        albedo = textures.white,
        tint = Vec3(0.32f, 0.41f, 0.23f),
        roughness = 0.75f,
        metallic = 0.05f,
        uvScale = 1f
    )

    /** High-RPM motion-blur disc; alpha is driven from propeller RPM each frame. */
    val propellerBlurMaterial = Material(
        albedo = textures.rubber,
        tint = Vec3(0.58f, 0.62f, 0.56f),
        roughness = 1f,
        metallic = 0f,
        uvScale = 1f,
        alpha = 0f
    )

    private var signature = ""
    private var selectedWorld: WorldId = WorldId.FOREST

    /** Rebuilds geometry when the build differs from what is currently uploaded. */
    fun updateFor(build: AircraftBuild, worldId: WorldId? = null) {
        if (worldId != null) selectedWorld = worldId
        val newSignature = selectedWorld.name + "|" + PartSlot.values().joinToString(",") { "${it.name}=${build.tierOf(it)}" }
        if (newSignature == signature) return
        signature = newSignature

        disposeMeshes()

        canvasMesh = Mesh("aircraftCanvas", buildCanvasData(build))
        ribMesh = Mesh("aircraftRibs", buildRibData(build))
        woodMesh = Mesh("aircraftWood", buildWoodData(build))
        metalMesh = Mesh("aircraftMetal", buildMetalData(build))
        canopyMesh = Mesh("aircraftCanopy", buildCanopyData())
        if (build.isInstalled(PartSlot.PROPELLER)) {
            propellerMesh = Mesh("aircraftPropeller", buildPropellerData(build))
            propellerBlurMesh = Mesh("aircraftPropellerBlur", buildPropellerBlurData())
        }

        loadImportedModel()
    }

    fun dispose() {
        disposeMeshes()
        signature = ""
    }

    private fun disposeMeshes() {
        canvasMesh?.dispose()
        ribMesh?.dispose()
        woodMesh?.dispose()
        metalMesh?.dispose()
        canopyMesh?.dispose()
        propellerMesh?.dispose()
        propellerBlurMesh?.dispose()
        for (part in importedParts) part.mesh.dispose()
        importedParts = emptyList()
        canvasMesh = null
        ribMesh = null
        woodMesh = null
        metalMesh = null
        canopyMesh = null
        propellerMesh = null
        propellerBlurMesh = null
    }

    /**
     * Loads the imported biplane once. It is a single welded mesh with seven flat materials,
     * so it is split by material and each part keeps its own colour. The source is already
     * Y-up with its nose on -Z, which is this rig's forward axis; the loader only scales it to
     * [IMPORTED_SPAN] and centres it on the rig's origin.
     */
    private fun loadImportedModel() {
        if (importedParts.isNotEmpty()) return

        val manager = assets ?: return
        val modelName = when (selectedWorld) {
            WorldId.FOREST -> "Skyhopper"
            WorldId.DESERT -> "VintageBiplane"
            WorldId.SNOW -> "FrostFreighter"
            WorldId.OCEAN -> "AuroraScout"
            WorldId.MOUNTAIN -> "ViperStrike"
        }
        val parts = AircraftModelLoader.load(manager, "aircraft/worlds/$modelName.obj",
            "aircraft/worlds/aircraft_and_glacier.mtl", IMPORTED_SPAN)
        if (parts.isEmpty()) return

        importedParts = parts.mapIndexed { index, part ->
            ImportedPart(
                Mesh("aircraftImported$index", part.data),
                Material(
                    albedo = textures.white,
                    tint = part.color,
                    roughness = 0.72f,
                    metallic = 0f,
                    uvScale = 1f
                )
            )
        }
    }

    // ---- fabric surfaces --------------------------------------------------

    private fun buildCanvasData(build: AircraftBuild): MeshData {
        val b = MeshBuilder()
        if (build.isInstalled(PartSlot.WING_LEFT)) {
            addWingHalf(b, -1f, build.tierOf(PartSlot.WING_LEFT))
        }
        if (build.isInstalled(PartSlot.WING_RIGHT)) {
            addWingHalf(b, 1f, build.tierOf(PartSlot.WING_RIGHT))
        }
        if (build.isInstalled(PartSlot.TAIL)) {
            addTail(b, build.tierOf(PartSlot.TAIL))
        }
        return b.build()
    }

    private fun addWingHalf(b: MeshBuilder, side: Float, tier: Int) {
        val span = wingSpan(tier)
        val inner = FUSELAGE_HALF_WIDTH * 0.75f
        val halfLength = (span - inner) * 0.5f
        val centerX = side * (inner + halfLength)

        // Upper and lower wings of the biplane pair.
        b.addBox(centerX, UPPER_WING_Y, WING_Z, halfLength, 0.030f, UPPER_CHORD * 0.5f, 1.6f)
        b.addBox(centerX, LOWER_WING_Y, WING_Z, halfLength, 0.026f, LOWER_CHORD * 0.5f, 1.6f)

        // Rounded wing tips.
        b.addEllipsoid(side * span, UPPER_WING_Y, WING_Z, 0.06f, 0.042f, UPPER_CHORD * 0.46f, 5, 8)
        b.addEllipsoid(side * span, LOWER_WING_Y, WING_Z, 0.06f, 0.038f, LOWER_CHORD * 0.46f, 5, 8)
    }

    private fun addTail(b: MeshBuilder, tier: Int) {
        val finHeight = 0.24f + if (tier >= 2) 0.04f else 0f
        val half = stabiliserHalf(tier)

        b.addBox(0f, 0.28f, 1.18f, 0.022f, finHeight, 0.20f, 1.4f)
        b.addBox(0f, 0.09f, 1.32f, half, 0.020f, 0.17f, 1.4f)
        b.addEllipsoid(0f, 0.28f + finHeight, 1.18f, 0.028f, 0.05f, 0.16f, 4, 8)

        if (tier >= 2) {
            b.addBox(-half, 0.09f, 1.32f, 0.02f, 0.09f, 0.17f, 1f)
            b.addBox(half, 0.09f, 1.32f, 0.02f, 0.09f, 0.17f, 1f)
        }
    }

    // ---- rib decals -------------------------------------------------------

    private fun buildRibData(build: AircraftBuild): MeshData {
        val b = MeshBuilder()
        for (side in intArrayOf(-1, 1)) {
            val slot = if (side < 0) PartSlot.WING_LEFT else PartSlot.WING_RIGHT
            if (!build.isInstalled(slot)) continue
            val span = wingSpan(build.tierOf(slot))
            var x = 0.34f
            while (x < span - 0.14f) {
                b.addBox(side * x, UPPER_WING_Y + 0.038f, WING_Z, 0.016f, 0.008f, UPPER_CHORD * 0.46f, 1f)
                b.addBox(side * x, LOWER_WING_Y + 0.034f, WING_Z, 0.016f, 0.008f, LOWER_CHORD * 0.46f, 1f)
                x += 0.40f
            }
        }

        if (build.isInstalled(PartSlot.TAIL)) {
            val half = stabiliserHalf(build.tierOf(PartSlot.TAIL))
            var x = 0.16f
            while (x < half - 0.08f) {
                b.addBox(-x, 0.114f, 1.32f, 0.013f, 0.006f, 0.15f, 1f)
                b.addBox(x, 0.114f, 1.32f, 0.013f, 0.006f, 0.15f, 1f)
                x += 0.26f
            }
        }
        return b.build()
    }

    // ---- timber structure -------------------------------------------------

    private fun buildWoodData(build: AircraftBuild): MeshData {
        val b = MeshBuilder()

        // Narrow tapered fuselage, nose at -Z.
        b.pushTrs(0f, 0f, 0f, M.HALF_PI, 0f, 0f, 1f)
        b.addCylinder(0f, 0f, 0f, 0.22f, 0.15f, 2.6f, 10, true, true)
        b.pop()

        // Four longerons give the lattice body its corners.
        for (sx in intArrayOf(-1, 1)) {
            for (sy in intArrayOf(-1, 1)) {
                b.addBox(sx * 0.15f, sy * 0.12f, 0.05f, 0.013f, 0.013f, 1.12f, 1f)
            }
        }

        // Cabane and interplane struts, per installed wing side.
        for (side in intArrayOf(-1, 1)) {
            val slot = if (side < 0) PartSlot.WING_LEFT else PartSlot.WING_RIGHT
            if (!build.isInstalled(slot)) continue
            val outer = (wingSpan(build.tierOf(slot)) - 0.18f) * 0.86f

            b.addRod(side * 0.14f, 0.20f, -0.44f, side * 0.62f, UPPER_WING_Y, -0.34f, 0.024f)
            b.addRod(side * 0.12f, 0.20f, 0.20f, side * 0.58f, UPPER_WING_Y, 0.16f, 0.022f)
            b.addRod(side * 0.72f, LOWER_WING_Y, WING_Z, side * 0.72f, UPPER_WING_Y, WING_Z, 0.024f)
            b.addRod(side * outer, LOWER_WING_Y, WING_Z, side * outer, UPPER_WING_Y, WING_Z, 0.022f)
        }

        // Landing-gear legs and the tail skid belong to the bare airframe.
        b.addRod(-0.12f, -0.14f, -0.40f, -0.56f, -0.80f, -0.44f, 0.022f)
        b.addRod(0.12f, -0.14f, -0.40f, 0.56f, -0.80f, -0.44f, 0.022f)
        b.addRod(0f, -0.12f, 1.02f, 0f, -0.52f, 1.12f, 0.018f)

        return b.build()
    }

    // ---- metal fittings ---------------------------------------------------

    private fun buildMetalData(build: AircraftBuild): MeshData {
        val b = MeshBuilder()

        // Dark cockpit opening.
        b.addEllipsoid(0f, 0.175f, -0.30f, 0.13f, 0.035f, 0.24f, 4, 10)

        if (build.isInstalled(PartSlot.ENGINE)) {
            val tier = build.tierOf(PartSlot.ENGINE)
            b.addBox(0f, 0f, -1.38f, 0.17f, 0.17f, 0.16f, 1.6f)
            b.pushTrs(0f, 0f, 0f, M.HALF_PI, 0f, 0f, 1f)
            b.addCylinder(0f, 0f, -1.54f, 0.15f, 0.12f, 0.12f, 12, true, true)
            b.pop()
            b.addRod(0.14f, 0.04f, -1.30f, 0.22f, -0.22f, -1.05f, 0.018f)
            b.addRod(-0.14f, 0.04f, -1.30f, -0.22f, -0.22f, -1.05f, 0.018f)
            if (tier >= 2) {
                b.addBox(0f, 0.19f, -1.30f, 0.13f, 0.05f, 0.12f, 1.2f)
            }
        }

        // Wheels, hubs and axle.
        for (side in intArrayOf(-1, 1)) {
            b.pushTrs(side * 0.58f, -0.80f, -0.44f, 0f, 0f, M.HALF_PI, 1f)
            b.addCylinder(0f, 0f, 0f, 0.27f, 0.27f, 0.10f, 14)
            b.pop()
            b.pushTrs(side * 0.64f, -0.80f, -0.44f, 0f, 0f, M.HALF_PI, 1f)
            b.addCylinder(0f, 0f, 0f, 0.08f, 0.08f, 0.06f, 10)
            b.pop()
        }
        b.addRod(-0.58f, -0.80f, -0.44f, 0.58f, -0.80f, -0.44f, 0.022f)

        // Wing bracing wires in an X pattern on each installed side.
        for (side in intArrayOf(-1, 1)) {
            val slot = if (side < 0) PartSlot.WING_LEFT else PartSlot.WING_RIGHT
            if (!build.isInstalled(slot)) continue
            val tip = wingSpan(build.tierOf(slot)) - 0.20f

            b.addRod(side * 0.34f, UPPER_WING_Y, WING_Z - 0.20f, side * tip, LOWER_WING_Y, WING_Z - 0.04f, 0.010f, 5)
            b.addRod(side * 0.34f, LOWER_WING_Y, WING_Z - 0.20f, side * tip, UPPER_WING_Y, WING_Z - 0.04f, 0.010f, 5)
            b.addRod(side * 0.34f, UPPER_WING_Y, WING_Z + 0.20f, side * tip, LOWER_WING_Y, WING_Z + 0.04f, 0.010f, 5)
            b.addRod(side * 0.34f, LOWER_WING_Y, WING_Z + 0.20f, side * tip, UPPER_WING_Y, WING_Z + 0.04f, 0.010f, 5)
        }

        // Spinner.
        b.pushTrs(0f, 0f, 0f, M.HALF_PI, 0f, 0f, 1f)
        b.addCylinder(0f, 0f, -1.62f, 0.10f, 0.045f, 0.16f, 10, true, true)
        b.pop()

        return b.build()
    }

    private fun buildCanopyData(): MeshData {
        val b = MeshBuilder()
        b.addEllipsoid(0f, 0.235f, -0.52f, 0.13f, 0.10f, 0.075f, 5, 9)
        return b.build()
    }

    /** Flat translucent disc covering the blade sweep, faded in at high RPM. */
    private fun buildPropellerBlurData(): MeshData {
        val b = MeshBuilder()
        b.addEllipsoid(0f, 0f, 0f, 0.88f, 0.88f, 0.005f, 8, 20)
        return b.build()
    }

    private fun buildPropellerData(build: AircraftBuild): MeshData {
        val b = MeshBuilder()
        val tier = build.tierOf(PartSlot.PROPELLER)

        // Hub, oriented along Z.
        b.pushTrs(0f, 0f, 0f, M.HALF_PI, 0f, 0f, 1f)
        b.addCylinder(0f, 0f, 0f, 0.055f, 0.075f, 0.16f, 10)
        b.pop()

        b.addBox(0f, 0.46f, 0f, 0.045f, 0.40f, 0.016f)
        b.addBox(0f, -0.46f, 0f, 0.045f, 0.40f, 0.016f)

        if (tier >= 2) {
            b.pushTrs(0f, 0f, 0f, 0f, 0f, 2.0943952f, 1f)
            b.addBox(0f, 0.44f, 0f, 0.042f, 0.36f, 0.016f)
            b.pop()
            b.pushTrs(0f, 0f, 0f, 0f, 0f, -2.0943952f, 1f)
            b.addBox(0f, 0.44f, 0f, 0.042f, 0.36f, 0.016f)
            b.pop()
        }

        return b.build()
    }

    private fun wingSpan(tier: Int): Float = WING_SPAN + if (tier >= 2) 0.24f else 0f

    private fun stabiliserHalf(tier: Int): Float = 0.70f + if (tier >= 2) 0.08f else 0f

    private companion object {
        /** Wingspan the imported airframe is scaled to, matching the built one. */
        const val IMPORTED_SPAN = 3.56f
        const val UPPER_WING_Y = 0.62f
        const val LOWER_WING_Y = -0.34f
        const val WING_Z = -0.08f
        const val UPPER_CHORD = 0.68f
        const val LOWER_CHORD = 0.58f
        const val WING_SPAN = 1.78f
        const val FUSELAGE_HALF_WIDTH = 0.20f
    }
}
