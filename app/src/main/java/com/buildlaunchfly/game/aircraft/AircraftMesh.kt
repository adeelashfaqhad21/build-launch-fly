package com.buildlaunchfly.game.aircraft

import com.buildlaunchfly.game.gfx.Material
import com.buildlaunchfly.game.gfx.Mesh
import com.buildlaunchfly.game.gfx.MeshBuilder
import com.buildlaunchfly.game.gfx.MeshData
import com.buildlaunchfly.game.gfx.TextureFactory
import com.buildlaunchfly.game.math.M
import com.buildlaunchfly.game.math.Vec3

/**
 * Procedural aircraft geometry. The mesh is rebuilt whenever the installed part tiers
 * change, so every purchase becomes a physically visible change on the airframe.
 *
 * Local axes: forward = -Z, up = +Y, right = +X.
 */
class AircraftMesh(textures: TextureFactory) {

    var bodyMesh: Mesh? = null
        private set
    var propellerMesh: Mesh? = null
        private set

    private var signature = ""

    val bodyMaterial = Material(
        albedo = textures.metal,
        tint = Vec3(0.93f, 0.95f, 0.99f),
        roughness = 0.34f,
        metallic = 0.82f,
        uvScale = 1.6f
    )

    val propellerMaterial = Material(
        albedo = textures.metal,
        tint = Vec3(0.24f, 0.23f, 0.21f),
        roughness = 0.42f,
        metallic = 0.95f,
        uvScale = 1f
    )

    /** Rebuilds geometry when the build differs from what is currently uploaded. */
    fun updateFor(build: AircraftBuild) {
        val newSignature = PartSlot.values().joinToString(",") { "${it.name}=${build.tierOf(it)}" }
        if (newSignature == signature) return
        signature = newSignature

        bodyMesh?.dispose()
        propellerMesh?.dispose()

        bodyMesh = Mesh("aircraftBody", buildBodyData(build))
        propellerMesh = if (build.isInstalled(PartSlot.PROPELLER)) {
            Mesh("aircraftPropeller", buildPropellerData(build))
        } else {
            null
        }
    }

    fun dispose() {
        bodyMesh?.dispose()
        propellerMesh?.dispose()
        bodyMesh = null
        propellerMesh = null
        signature = ""
    }

    private fun buildBodyData(build: AircraftBuild): MeshData {
        val b = MeshBuilder()

        // Fuselage: a tapered tube along Z (nose at -1.5, tail at +1.5).
        b.pushTrs(0f, 0f, 0f, M.HALF_PI, 0f, 0f, 1f)
        b.addCylinder(0f, 0f, 0f, 0.36f, 0.20f, 3.0f, 14)
        b.pop()

        // Cockpit canopy.
        b.addEllipsoid(0f, 0.20f, -0.18f, 0.25f, 0.22f, 0.62f, 6, 10)

        if (build.isInstalled(PartSlot.ENGINE)) {

            b.addEllipsoid(0f, 0f, -1.34f, 0.30f, 0.30f, 0.36f, 6, 10)
        }

        // Wings (left = -X, right = +X) with a little dihedral.
        if (build.isInstalled(PartSlot.WING_LEFT)) {
            b.pushTrs(0f, 0f, 0f, 0f, 0f, 0.07f, 1f)
            b.addBox(-1.06f, 0.02f, 0.06f, 1.02f, 0.052f, 0.46f, 1.5f)
            b.addBox(-2.02f, 0.08f, 0.06f, 0.06f, 0.20f, 0.30f, 1.2f)
            b.pop()
            b.addBox(-0.42f, -0.04f, 0.06f, 0.05f, 0.12f, 0.16f, 1f)
        }
        if (build.isInstalled(PartSlot.WING_RIGHT)) {
            b.pushTrs(0f, 0f, 0f, 0f, 0f, -0.07f, 1f)
            b.addBox(1.06f, 0.02f, 0.06f, 1.02f, 0.052f, 0.46f, 1.5f)
            b.addBox(2.02f, 0.08f, 0.06f, 0.06f, 0.20f, 0.30f, 1.2f)
            b.pop()
            b.addBox(0.42f, -0.04f, 0.06f, 0.05f, 0.12f, 0.16f, 1f)
        }

        // Tail assembly.
        if (build.isInstalled(PartSlot.TAIL)) {
            b.addBox(0f, 0.36f, 1.24f, 0.045f, 0.34f, 0.30f, 1.2f)
            b.addBox(0f, 0.10f, 1.42f, 0.62f, 0.035f, 0.20f, 1.2f)
        }

        // Tail boom stiffeners keep the bare fuselage readable.
        b.addBox(0f, -0.16f, 0.9f, 0.20f, 0.03f, 0.42f, 1f)

        return b.build()
    }

    private fun buildPropellerData(build: AircraftBuild): MeshData {
        val b = MeshBuilder()
        val tier = build.tierOf(PartSlot.PROPELLER)

        // Spinner + hub, oriented along Z.
        b.pushTrs(0f, 0f, 0f, M.HALF_PI, 0f, 0f, 1f)
        b.addCylinder(0f, 0f, 0f, 0.09f, 0.13f, 0.22f, 10)
        b.pop()

        b.addBox(0f, 0.56f, 0f, 0.055f, 0.52f, 0.02f)
        b.addBox(0f, -0.56f, 0f, 0.055f, 0.52f, 0.02f)

        if (tier >= 2) {
            b.pushTrs(0f, 0f, 0f, 0f, 0f, 2.0943952f, 1f)
            b.addBox(0f, 0.50f, 0f, 0.05f, 0.46f, 0.02f)
            b.pop()
            b.pushTrs(0f, 0f, 0f, 0f, 0f, -2.0943952f, 1f)
            b.addBox(0f, 0.50f, 0f, 0.05f, 0.46f, 0.02f)
            b.pop()
        }

        return b.build()
    }
}
