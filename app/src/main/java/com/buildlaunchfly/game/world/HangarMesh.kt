package com.buildlaunchfly.game.world

import com.buildlaunchfly.game.gfx.Material
import com.buildlaunchfly.game.gfx.Mesh
import com.buildlaunchfly.game.gfx.MeshBuilder
import com.buildlaunchfly.game.gfx.MeshData
import com.buildlaunchfly.game.gfx.TextureFactory
import com.buildlaunchfly.game.math.Vec3

/**
 * The weathered wooden barn from the reference plates: a dark plank wall down the left, a
 * heavy beamed roof over the whole rig, a warm tan plank floor, corner posts down the open
 * right side with crates and bales standing outside in the daylight.
 *
 * Built in metres relative to the pad: forward is -Z, so the camera looks in from +Z and out
 * through the open front toward the valley.
 */
class HangarMesh(textures: TextureFactory) {

    /** Left wall and posts. */
    val wallMesh: Mesh

    /** Roof slab, rafters and the front eave beam. */
    val roofMesh: Mesh

    /** Plank floor plus the crates and bales outside the open side. */
    val floorMesh: Mesh

    /** Weathered dark planks, roughly `#554940`. */
    val wallMaterial = Material(
        albedo = textures.bark,
        tint = Vec3(0.33f, 0.29f, 0.25f),
        roughness = 0.94f,
        metallic = 0f,
        uvScale = 3.4f
    )

    /** Lighter taupe roof underside, roughly `#75695D`. */
    val roofMaterial = Material(
        albedo = textures.bark,
        tint = Vec3(0.46f, 0.41f, 0.36f),
        roughness = 0.92f,
        metallic = 0f,
        uvScale = 2.6f
    )

    /** Warm tan floor, roughly `#A88A6D`. */
    val floorMaterial = Material(
        albedo = textures.bark,
        tint = Vec3(0.66f, 0.54f, 0.43f),
        roughness = 0.90f,
        metallic = 0f,
        uvScale = 4f
    )

    init {
        wallMesh = Mesh("hangarWall", buildWallData())
        roofMesh = Mesh("hangarRoof", buildRoofData())
        floorMesh = Mesh("hangarFloor", buildFloorData())
    }

    private fun buildWallData(): MeshData {
        val b = MeshBuilder()

        // Left plank wall, rising to the roof.
        b.addBox(-2.80f, 3.10f, 0.5f, 0.14f, 3.10f, 6.5f, 3.4f)

        // Right side is open to the sky; it keeps only its corner posts.
        b.addBox(2.80f, 3.10f, -5.80f, 0.16f, 3.10f, 0.16f, 2f)
        b.addBox(2.80f, 3.10f, 6.80f, 0.16f, 3.10f, 0.16f, 2f)

        // Wall posts every couple of metres give the planks their frame.
        var z = -5.4f
        while (z <= 6.4f) {
            b.addBox(-2.78f, 3.10f, z, 0.19f, 3.10f, 0.13f, 2f)
            z += 2.4f
        }

        return b.build()
    }

    private fun buildRoofData(): MeshData {
        val b = MeshBuilder()

        // Roof slab over the whole rig, long enough to hang over the launch camera.
        b.addBox(0f, 6.30f, 2.0f, 3.10f, 0.16f, 8.6f, 2.6f)

        // Heavy dark rafters across it, every 1.3 m.
        var z = -6.2f
        while (z <= 10.2f) {
            b.addBox(0f, 6.05f, z, 3.05f, 0.16f, 0.20f, 2f)
            z += 1.3f
        }

        // Two long purlins running the length of the roof.
        b.addBox(-2.2f, 6.08f, 2.0f, 0.10f, 0.12f, 8.6f, 2f)
        b.addBox(2.2f, 6.08f, 2.0f, 0.10f, 0.12f, 8.6f, 2f)

        // Front eave beam over the open end.
        b.addBox(0f, 6.00f, -6.20f, 3.10f, 0.22f, 0.20f, 2f)

        // Two diagonal braces from the left wall up to the rafters.
        b.pushTrs(-2.70f, 5.00f, -4.0f, 0f, 0f, 0.55f, 1f)
        b.addBox(0f, 0f, 0f, 0.09f, 1.30f, 0.14f, 2f)
        b.pop()
        b.pushTrs(-2.70f, 5.00f, 4.0f, 0f, 0f, 0.55f, 1f)
        b.addBox(0f, 0f, 0f, 0.09f, 1.30f, 0.14f, 2f)
        b.pop()

        return b.build()
    }

    private fun buildFloorData(): MeshData {
        val b = MeshBuilder()

        // Warm tan plank floor under the aircraft.
        b.addBox(0f, -0.09f, 0.5f, 2.85f, 0.09f, 6.5f, 4f)

        // Crates and bales outside the open right side, seen through the entrance.
        b.addBox(3.90f, 0.42f, -2.20f, 0.42f, 0.42f, 0.42f, 2f)
        b.addBox(4.05f, 1.16f, -2.10f, 0.28f, 0.28f, 0.28f, 2f)
        b.addBox(4.60f, 0.36f, 0.40f, 0.36f, 0.36f, 0.52f, 2f)
        b.addBox(3.70f, 0.30f, 2.60f, 0.30f, 0.30f, 0.30f, 2f)

        // A bale or two just inside the left wall as well.
        b.addBox(-2.15f, 0.34f, 4.30f, 0.40f, 0.34f, 0.50f, 2f)

        return b.build()
    }

    fun dispose() {
        wallMesh.dispose()
        roofMesh.dispose()
        floorMesh.dispose()
    }
}
