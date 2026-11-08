package com.buildlaunchfly.game.world

import com.buildlaunchfly.game.gfx.Material
import com.buildlaunchfly.game.gfx.Mesh
import com.buildlaunchfly.game.gfx.MeshBuilder
import com.buildlaunchfly.game.gfx.MeshData
import com.buildlaunchfly.game.gfx.TextureFactory
import com.buildlaunchfly.game.math.Vec3

/**
 * The wooden hangar from the reference plates: a plank floor, side walls, a back wall, corner
 * posts, roof panels with a wide sky opening over the aircraft and a rafter across every bay.
 * The front is left open toward the camera, and a couple of crates sit against the back wall.
 *
 * Built in metres, pad origin at (0, 0, 0), forward -Z so the camera looks in from +Z.
 */
class HangarMesh(textures: TextureFactory) {

    val shellMesh: Mesh
    val frameMesh: Mesh

    /** Wide plank floor, side and back walls, roof panels. */
    val shellMaterial = Material(
        albedo = textures.bark,
        tint = Vec3(0.80f, 0.64f, 0.46f),
        roughness = 0.88f,
        metallic = 0f,
        uvScale = 3f
    )

    /** Darker corner posts, rafters, door header and crates. */
    val frameMaterial = Material(
        albedo = textures.bark,
        tint = Vec3(0.46f, 0.33f, 0.22f),
        roughness = 0.86f,
        metallic = 0f,
        uvScale = 2f
    )

    init {
        shellMesh = Mesh("hangarShell", buildShellData())
        frameMesh = Mesh("hangarFrame", buildFrameData())
    }

    private fun buildShellData(): MeshData {
        val b = MeshBuilder()

        // Floor deck.
        b.addBox(0f, -0.10f, 0.5f, 3.4f, 0.10f, 6.0f, 3f)

        // Side walls.
        b.addBox(-3.5f, 3.0f, 0.5f, 0.15f, 3.0f, 6.0f, 3f)
        b.addBox(3.5f, 3.0f, 0.5f, 0.15f, 3.0f, 6.0f, 3f)

        // Back wall and a low back kerb.
        b.addBox(0f, 3.0f, -5.5f, 3.6f, 3.0f, 0.15f, 3f)
        b.addBox(0f, 0.35f, -5.2f, 3.2f, 0.35f, 0.20f, 3f)

        // Roof panels, leaving a 2.4 m opening over the aircraft.
        b.addBox(-2.4f, 6.10f, 0.5f, 1.2f, 0.12f, 6.0f, 3f)
        b.addBox(2.4f, 6.10f, 0.5f, 1.2f, 0.12f, 6.0f, 3f)

        return b.build()
    }

    private fun buildFrameData(): MeshData {
        val b = MeshBuilder()

        // Corner posts.
        for (sx in intArrayOf(-1, 1)) {
            for (sz in intArrayOf(-1, 1)) {
                b.addBox(sx * 3.42f, 3.0f, if (sz < 0) -5.42f else 6.42f, 0.18f, 3.0f, 0.18f, 2f)
            }
        }

        // Rafters across the roof, every 1.2 m, plus the ridge beams.
        var z = -5.0f
        while (z <= 6.2f) {
            b.addBox(0f, 5.88f, z, 3.5f, 0.10f, 0.10f, 2f)
            z += 1.2f
        }
        b.addBox(-1.25f, 6.20f, 0.5f, 0.10f, 0.10f, 6.0f, 2f)
        b.addBox(1.25f, 6.20f, 0.5f, 0.10f, 0.10f, 6.0f, 2f)

        // Front header over the open side.
        b.addBox(0f, 5.92f, 6.45f, 3.6f, 0.16f, 0.16f, 2f)

        // Props: two crates against the back wall.
        b.addBox(-2.55f, 0.42f, -4.60f, 0.42f, 0.42f, 0.42f, 2f)
        b.addBox(-2.30f, 1.18f, -4.55f, 0.28f, 0.28f, 0.28f, 2f)
        b.addBox(2.60f, 0.34f, -4.70f, 0.34f, 0.34f, 0.34f, 2f)

        return b.build()
    }

    fun dispose() {
        shellMesh.dispose()
        frameMesh.dispose()
    }
}
