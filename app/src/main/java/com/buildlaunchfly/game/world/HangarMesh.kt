package com.buildlaunchfly.game.world

import com.buildlaunchfly.game.gfx.Material
import com.buildlaunchfly.game.gfx.Mesh
import com.buildlaunchfly.game.gfx.MeshBuilder
import com.buildlaunchfly.game.gfx.MeshData
import com.buildlaunchfly.game.gfx.TextureFactory
import com.buildlaunchfly.game.math.M
import com.buildlaunchfly.game.math.Vec3

/**
 * The wooden hangar from the reference plates: sawn-plank left wall and floor, a gable roof on
 * exposed rafters and tie beams, a heavy door frame at the open front, and the clutter of a
 * working shed — hay bales, crates, a barrel, a bench and a hanging lamp.
 *
 * Built in metres relative to the pad: forward is -Z, so the camera looks in from +Z and out
 * through the open front toward the valley.
 */
class HangarMesh(textures: TextureFactory) {

    /** Plank wall, door frame and the open side's posts. */
    val wallMesh: Mesh

    /** Gable panels, ridge, rafters, ties and door header. */
    val roofMesh: Mesh

    /** Plank floor, hay bales, crates, barrel, bench and stool. */
    val floorMesh: Mesh

    /** Hanging lamp and its shade. */
    val metalMesh: Mesh

    /** Weathered dark planks for the wall. */
    val wallMaterial = Material(
        albedo = textures.planks,
        tint = Vec3(0.56f, 0.48f, 0.40f),
        roughness = 0.94f,
        metallic = 0f,
        uvScale = 2.2f
    )

    /** Warm tan planks for the floor and the props. */
    val floorMaterial = Material(
        albedo = textures.planks,
        tint = Vec3(0.94f, 0.80f, 0.62f),
        roughness = 0.90f,
        metallic = 0f,
        uvScale = 2.6f
    )

    /** Exposed timber structure: ridge, rafters, ties, studs and the door frame. */
    val roofMaterial = Material(
        albedo = textures.bark,
        tint = Vec3(0.40f, 0.30f, 0.22f),
        roughness = 0.90f,
        metallic = 0f,
        uvScale = 2.2f
    )

    /** Lamp and shade. */
    val metalMaterial = Material(
        albedo = textures.metal,
        tint = Vec3(0.32f, 0.32f, 0.34f),
        roughness = 0.60f,
        metallic = 0.4f,
        uvScale = 1f
    )

    init {
        wallMesh = Mesh("hangarWall", buildWallData())
        roofMesh = Mesh("hangarRoof", buildRoofData())
        floorMesh = Mesh("hangarFloor", buildFloorData())
        metalMesh = Mesh("hangarMetal", buildMetalData())
    }

    private fun buildWallData(): MeshData {
        val b = MeshBuilder()

        // Plank wall down the left side.
        b.addBox(-2.90f, 2.60f, 0.75f, 0.12f, 2.60f, 6.75f, 2.2f)

        // Vertical studs on the inside face.
        var z = -5.4f
        while (z <= 6.9f) {
            b.addBox(-2.76f, 2.60f, z, 0.09f, 2.60f, 0.09f, 2f)
            z += 2.2f
        }

        // Open right side: corner and intermediate posts.
        for (pz in floatArrayOf(-5.90f, -1.90f, 2.10f, 6.10f)) {
            b.addBox(2.90f, 2.60f, pz, 0.10f, 2.60f, 0.10f, 2f)
        }

        // Heavy door posts and header at the open front.
        b.addBox(-2.90f, 2.35f, -5.90f, 0.16f, 2.35f, 0.16f, 2f)
        b.addBox(2.90f, 2.35f, -5.90f, 0.16f, 2.35f, 0.16f, 2f)
        b.addBox(0f, 4.55f, -5.90f, 3.10f, 0.20f, 0.18f, 2f)

        return b.build()
    }

    private fun buildRoofData(): MeshData {
        val b = MeshBuilder()
        val pitch = 0.437f

        // Two gable panels, running past the camera so the launch view sits under the eave.
        b.pushTrs(-1.60f, 5.95f, 2.25f, 0f, 0f, pitch, 1f)
        b.addBox(0f, 0f, 0f, 1.86f, 0.08f, 8.25f, 2.4f)
        b.pop()
        b.pushTrs(1.60f, 5.95f, 2.25f, 0f, 0f, -pitch, 1f)
        b.addBox(0f, 0f, 0f, 1.86f, 0.08f, 8.25f, 2.4f)
        b.pop()

        // Ridge beam.
        b.addBox(0f, 6.78f, 2.25f, 0.10f, 0.10f, 8.25f, 2f)

        // Rafters and ties every 1.3 m.
        var z = -5.8f
        while (z <= 10.1f) {
            b.pushTrs(-1.55f, 5.93f, z, 0f, 0f, pitch, 1f)
            b.addBox(0f, 0f, 0f, 1.62f, 0.09f, 0.09f, 2f)
            b.pop()
            b.pushTrs(1.55f, 5.93f, z, 0f, 0f, -pitch, 1f)
            b.addBox(0f, 0f, 0f, 1.62f, 0.09f, 0.09f, 2f)
            b.pop()
            b.addBox(0f, 5.15f, z, 2.95f, 0.08f, 0.09f, 2f)
            z += 1.3f
        }

        // Long purlins under the panels.
        b.addBox(-2.10f, 5.52f, 2.25f, 0.08f, 0.08f, 8.25f, 2f)
        b.addBox(2.10f, 5.52f, 2.25f, 0.08f, 0.08f, 8.25f, 2f)

        return b.build()
    }

    private fun buildFloorData(): MeshData {
        val b = MeshBuilder()

        // Plank floor.
        b.addBox(0f, -0.08f, 0.75f, 2.90f, 0.08f, 6.75f, 2.6f)

        // Hay bales stacked against the wall.
        b.addBox(-2.20f, 0.45f, -4.60f, 0.45f, 0.45f, 0.60f, 1.6f)
        b.addBox(-2.15f, 0.45f, -3.40f, 0.45f, 0.45f, 0.60f, 1.6f)
        b.addBox(-2.20f, 1.32f, -4.45f, 0.42f, 0.42f, 0.55f, 1.6f)

        // Crates by the open side.
        b.addBox(2.20f, 0.40f, -4.80f, 0.40f, 0.40f, 0.40f, 1.6f)
        b.addBox(2.30f, 1.10f, -4.70f, 0.30f, 0.30f, 0.30f, 1.6f)

        // Barrel.
        b.addCylinder(2.40f, 0.45f, 4.20f, 0.30f, 0.26f, 0.90f, 12, true, true, 1.6f)

        // Workbench and stool.
        b.addBox(0f, 0.85f, 5.60f, 1.50f, 0.06f, 0.45f, 1.8f)
        for (sx in intArrayOf(-1, 1)) {
            for (sz in intArrayOf(-1, 1)) {
                b.addBox(sx * 1.30f, 0.42f, 5.60f + sz * 0.36f, 0.06f, 0.42f, 0.06f, 1.4f)
            }
        }
        b.addCylinder(0.95f, 0.25f, 5.00f, 0.22f, 0.20f, 0.50f, 9, true, true, 1.4f)

        return b.build()
    }

    private fun buildMetalData(): MeshData {
        val b = MeshBuilder()

        // Lamp rod from the ridge, shade opening downward, bulb inside.
        b.addCylinder(0f, 6.20f, 0.80f, 0.03f, 0.03f, 0.95f, 6, false, false, 1f)
        b.pushTrs(0f, 5.62f, 0.80f, M.PI.toFloat(), 0f, 0f, 1f)
        b.addCone(0f, 0f, 0f, 0.40f, 0.34f, 12, 1f)
        b.pop()
        b.addSphere(0f, 5.55f, 0.80f, 0.09f, 5, 8)

        return b.build()
    }

    fun dispose() {
        wallMesh.dispose()
        roofMesh.dispose()
        floorMesh.dispose()
        metalMesh.dispose()
    }
}
