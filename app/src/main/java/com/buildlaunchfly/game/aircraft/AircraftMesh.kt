package com.buildlaunchfly.game.aircraft

import com.buildlaunchfly.game.gfx.Material
import com.buildlaunchfly.game.gfx.Mesh
import com.buildlaunchfly.game.gfx.MeshBuilder
import com.buildlaunchfly.game.gfx.MeshData
import com.buildlaunchfly.game.gfx.TextureFactory
import com.buildlaunchfly.game.math.M
import com.buildlaunchfly.game.math.Vec3

/**
 * Procedural aircraft geometry. The meshes are rebuilt whenever the installed part tiers
 * change, so every purchase becomes a physically visible change on the airframe.
 *
 * Local axes: forward = -Z, up = +Y, right = +X. 1 unit = 1 metre.
 *
 * Stage 1 bare fuselage → stage 2 left wing → stage 3 right wing → stage 4 tail →
 * stage 5 functional propeller → stage 6 canopy, winglets and aerodynamic fairings.
 */
class AircraftMesh(textures: TextureFactory) {

    var bodyMesh: Mesh? = null
        private set
    var canopyMesh: Mesh? = null
        private set
    var propellerMesh: Mesh? = null
        private set
    var propellerBlurMesh: Mesh? = null
        private set

    private var signature = ""

    /** Aerospace composite lacquer per design.md §5.1: roughness 0.35, metallic 0.15. */
    val bodyMaterial = Material(
        albedo = textures.metal,
        tint = Vec3(0.93f, 0.95f, 0.99f),
        roughness = 0.35f,
        metallic = 0.15f,
        uvScale = 1.6f
    )

    /** Tinted polarized cockpit glazing. */
    val canopyMaterial = Material(
        albedo = textures.metal,
        tint = Vec3(0.10f, 0.20f, 0.28f),
        roughness = 0.08f,
        metallic = 0.0f,
        uvScale = 1f,
        emissive = 0.04f
    )

    val propellerMaterial = Material(
        albedo = textures.rubber,
        tint = Vec3(0.30f, 0.30f, 0.32f),
        roughness = 0.35f,
        metallic = 0.55f,
        uvScale = 1f
    )

    /** Rebuilds geometry when the build differs from what is currently uploaded. */
    fun updateFor(build: AircraftBuild) {
        val newSignature = PartSlot.values().joinToString(",") { "${it.name}=${build.tierOf(it)}" }
        if (newSignature == signature) return
        signature = newSignature

        bodyMesh?.dispose()
        canopyMesh?.dispose()
        propellerMesh?.dispose()
        propellerBlurMesh?.dispose()

        bodyMesh = Mesh("aircraftBody", buildBodyData(build))
        canopyMesh = if (hasAdvancedAirframe(build)) {
            Mesh("aircraftCanopy", buildCanopyData())
        } else {
            null
        }
        propellerMesh = if (build.isInstalled(PartSlot.PROPELLER)) {
            Mesh("aircraftPropeller", buildPropellerData(build))
        } else {
            null
        }
        propellerBlurMesh = if (build.isInstalled(PartSlot.PROPELLER)) {
            val blur = MeshBuilder()
            blur.addEllipsoid(0f, 0f, 0f, 1.48f, 1.48f, 0.006f, 16, 32)
            Mesh("aircraftPropellerBlurDisc", blur.build())
        } else {
            null
        }
    }

    fun dispose() {
        bodyMesh?.dispose()
        canopyMesh?.dispose()
        propellerMesh?.dispose()
        propellerBlurMesh?.dispose()
        bodyMesh = null
        canopyMesh = null
        propellerMesh = null
        propellerBlurMesh = null
        signature = ""
    }

    private fun buildBodyData(build: AircraftBuild): MeshData {
        val b = MeshBuilder()

        // Fuselage: a tapered tube along Z (nose at -1.5, tail at +1.5).
        b.pushTrs(0f, 0f, 0f, M.HALF_PI, 0f, 0f, 1f)
        b.addCylinder(0f, 0f, 0f, 0.36f, 0.20f, 3.0f, 14)
        b.pop()

        // Engine cowling / nose fairing.
        if (build.isInstalled(PartSlot.ENGINE)) {
            b.addEllipsoid(0f, 0f, -1.34f, 0.30f, 0.30f, 0.36f, 6, 10)
            if (build.tierOf(PartSlot.ENGINE) >= 2) {
                b.pushTrs(0f, 0f, 0f, M.HALF_PI, 0f, 0f, 1f)
                b.addCylinder(0f, 0f, -1.50f, 0.315f, 0.315f, 0.14f, 14, false, false, 1f)
                b.pop()
            }
        }

        // Wings (left = -X, right = +X) with a little dihedral.
        if (build.isInstalled(PartSlot.WING_LEFT)) {
            b.pushTrs(0f, 0f, 0f, 0f, 0f, 0.07f, 1f)
            b.addBox(-1.06f, 0.02f, 0.06f, 1.02f, 0.052f, 0.46f, 1.5f)
            b.addBox(-2.02f, 0.08f, 0.06f, 0.06f, 0.20f, 0.30f, 1.2f)
            if (build.tierOf(PartSlot.WING_LEFT) >= 2) {
                // Stage 6: canted winglet at the port tip.
                b.pushTrs(-2.02f, 0.28f, 0.06f, 0f, 0f, -0.35f, 1f)
                b.addBox(0f, 0f, 0f, 0.035f, 0.20f, 0.24f, 1f)
                b.pop()
            }
            b.pop()
            b.addBox(-0.42f, -0.04f, 0.06f, 0.05f, 0.12f, 0.16f, 1f)
        }
        if (build.isInstalled(PartSlot.WING_RIGHT)) {
            b.pushTrs(0f, 0f, 0f, 0f, 0f, -0.07f, 1f)
            b.addBox(1.06f, 0.02f, 0.06f, 1.02f, 0.052f, 0.46f, 1.5f)
            b.addBox(2.02f, 0.08f, 0.06f, 0.06f, 0.20f, 0.30f, 1.2f)
            if (build.tierOf(PartSlot.WING_RIGHT) >= 2) {
                b.pushTrs(2.02f, 0.28f, 0.06f, 0f, 0f, 0.35f, 1f)
                b.addBox(0f, 0f, 0f, 0.035f, 0.20f, 0.24f, 1f)
                b.pop()
            }
            b.pop()
            b.addBox(0.42f, -0.04f, 0.06f, 0.05f, 0.12f, 0.16f, 1f)
        }

        // Tail assembly.
        if (build.isInstalled(PartSlot.TAIL)) {
            b.addBox(0f, 0.36f, 1.24f, 0.045f, 0.34f, 0.30f, 1.2f)
            b.addBox(0f, 0.10f, 1.42f, 0.62f, 0.035f, 0.20f, 1.2f)
            if (build.tierOf(PartSlot.TAIL) >= 2) {
                // Stage 6: end-plate fairings.
                b.addBox(-0.60f, 0.12f, 1.42f, 0.025f, 0.11f, 0.18f, 1f)
                b.addBox(0.60f, 0.12f, 1.42f, 0.025f, 0.11f, 0.18f, 1f)
            }
        }

        // Tail boom stiffener keeps the bare fuselage readable.
        b.addBox(0f, -0.16f, 0.9f, 0.20f, 0.03f, 0.42f, 1f)

        return b.build()
    }

    /** Stage-6 airframe refinements unlock the streamlined cockpit canopy. */
    private fun hasAdvancedAirframe(build: AircraftBuild): Boolean =
        build.tierOf(PartSlot.WING_LEFT) >= 2 ||
            build.tierOf(PartSlot.WING_RIGHT) >= 2 ||
            build.tierOf(PartSlot.TAIL) >= 2

    private fun buildCanopyData(): MeshData {
        val b = MeshBuilder()
        b.addEllipsoid(0f, 0.20f, -0.18f, 0.25f, 0.22f, 0.62f, 6, 10)
        return b.build()
    }

    private fun buildPropellerData(build: AircraftBuild): MeshData {
        val b = MeshBuilder()
        val tier = build.tierOf(PartSlot.PROPELLER)

        // Spinner, thrust washer and stepped pitch-control hub, all coaxial with the shaft.
        b.pushTrs(0f, 0f, 0f, M.HALF_PI, 0f, 0f, 1f)
        b.addCylinder(0f, 0f, 0f, 0.09f, 0.13f, 0.22f, 12)
        b.addCylinder(0f, 0.10f, 0f, 0.17f, 0.17f, 0.055f, 12)
        b.addCylinder(0f, -0.11f, 0f, 0.125f, 0.125f, 0.06f, 12)
        b.pop()
        // The three collars and their radial pitch links make the variable-pitch
        // mechanism readable as hardware rather than three blades glued to a spinner.
        for (blade in 0 until 3) {
            val angle = blade * (M.TWO_PI / 3f)
            b.pushTrs(0f, 0f, 0f, 0f, 0f, angle, 1f)
            b.addCylinder(0f, 0.17f, 0f, 0.105f, 0.105f, 0.12f, 10)
            b.addBox(0f, 0.29f, 0f, 0.035f, 0.09f, 0.035f)
            val bladeLength = if (tier >= 2) 0.62f else 0.54f
            val bladeWidth = if (tier >= 2) 0.070f else 0.055f
            // The broad blade and narrower outer section form a tapered planform.
            // The blade is deliberately built in the same radial transform as its collar.
            b.addBox(0f, 0.34f + bladeLength * 0.88f, 0f, bladeWidth, bladeLength * 0.88f, 0.018f)
            b.addBox(0f, 0.34f + bladeLength * 1.62f, 0f, bladeWidth * 0.66f, bladeLength * 0.13f, 0.014f)
            if (tier >= 2) {
                // Forged-carbon tapered tip and reinforced root.
                b.addEllipsoid(0f, 0.34f + bladeLength * 1.76f, 0f, bladeWidth * 0.72f, 0.018f, 0.024f, 4, 6)
                b.addEllipsoid(0f, 0.40f, 0f, bladeWidth * 1.45f, 0.09f, 0.055f, 5, 7)
            }
            b.pop()
        }

        return b.build()
    }
}
