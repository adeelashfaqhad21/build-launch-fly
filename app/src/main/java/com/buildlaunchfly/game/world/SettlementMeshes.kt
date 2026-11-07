package com.buildlaunchfly.game.world

import com.buildlaunchfly.game.gfx.MeshBuilder
import com.buildlaunchfly.game.gfx.MeshData
import kotlin.random.Random

/** Three batched static meshes for the inhabited landmarks in each ecosystem. */
data class SettlementGeometry(
    val walls: MeshData,
    val roofs: MeshData,
    val details: MeshData
)

object SettlementMeshes {

    fun build(terrain: Terrain, definition: WorldDefinition, detailLevel: Int): SettlementGeometry {
        val walls = MeshBuilder()
        val roofs = MeshBuilder()
        val details = MeshBuilder()
        val rng = Random(definition.seed + 8127)
        val homesPerSide = when {
            detailLevel <= 0 -> 2
            detailLevel == 1 -> 3
            detailLevel == 2 -> 4
            else -> 5
        }
        val villageRows = floatArrayOf(-520f, -1320f, -2180f)

        for ((rowIndex, rowZ) in villageRows.withIndex()) {
            // Two hamlets flank a clear central flight lane. The offset and house
            // spacing vary by world so towns do not form repeated straight rows.
            val sideOffset = when (definition.id) {
                WorldId.DESERT -> 132f
                WorldId.SNOW -> 118f
                WorldId.OCEAN -> 145f
                WorldId.MOUNTAIN -> 152f
                WorldId.FOREST -> 126f
            }
            for (side in intArrayOf(-1, 1)) {
                for (home in 0 until homesPerSide) {
                    val x = side * (sideOffset + rng.nextFloat() * 36f) +
                        (rng.nextFloat() - 0.5f) * 76f
                    val z = rowZ + (rng.nextFloat() - 0.5f) * 150f
                    val ground = terrain.height(x, z)
                    if (ground < definition.waterLevel + 1.5f) continue
                    val width = when (definition.id) {
                        WorldId.SNOW -> 4.2f + rng.nextFloat() * 2.0f
                        WorldId.DESERT -> 4.6f + rng.nextFloat() * 2.4f
                        else -> 4.8f + rng.nextFloat() * 2.8f
                    }
                    val depth = width * (0.78f + rng.nextFloat() * 0.38f)
                    val height = when (definition.id) {
                        WorldId.DESERT -> 2.8f + rng.nextFloat() * 1.5f
                        WorldId.SNOW -> 3.3f + rng.nextFloat() * 1.8f
                        else -> 3.4f + rng.nextFloat() * 2.1f
                    }
                    val rotation = (rng.nextFloat() - 0.5f) * 0.34f
                    val igloo = definition.id == WorldId.SNOW && (home + rowIndex) % 3 == 0

                    if (igloo) {
                        // Low domed snow shelter with a recessed entrance.
                        roofs.pushTrs(x, ground + 0.02f, z, 0f, rotation, 0f, 1f)
                        roofs.addEllipsoid(0f, 1.35f, 0f, width * 0.72f, 1.34f, depth * 0.70f, 8, 12)
                        roofs.pop()
                        details.addBox(x, ground + 0.43f, z - depth * 0.62f, 0.52f, 0.48f, 0.12f, 1f)
                    } else {
                        walls.pushTrs(x, ground + height * 0.5f, z, 0f, rotation, 0f, 1f)
                        walls.addBox(0f, 0f, 0f, width, height * 0.5f, depth, 2f)

                        when (definition.id) {
                            WorldId.DESERT -> {
                                // Thick adobe parapet and flat roof, with a small raised
                                // shade canopy to break the silhouette of each hut.
                                roofs.addBox(0f, height + 0.12f, 0f, width * 1.04f, 0.18f, depth * 1.04f, 1f)
                                roofs.addBox(0f, height + 0.40f, depth * 0.16f, width * 0.48f, 0.07f, depth * 0.22f, 1f)
                            }
                            WorldId.SNOW -> {
                                // Steep paired roof planes shed snow away from the cabin.
                                roofs.pushTrs(0f, height, 0f, 0f, 0f, 0.62f, 1f)
                                roofs.addBox(-width * 0.43f, 0f, 0f, width * 0.62f, 0.15f, depth * 0.68f, 1f)
                                roofs.pop()
                                roofs.pushTrs(0f, height, 0f, 0f, 0f, -0.62f, 1f)
                                roofs.addBox(width * 0.43f, 0f, 0f, width * 0.62f, 0.15f, depth * 0.68f, 1f)
                                roofs.pop()
                            }
                            WorldId.OCEAN -> {
                                // Raised coastal homes on short stilts with broad roofs.
                                for (postX in floatArrayOf(-width * 0.72f, width * 0.72f)) {
                                    for (postZ in floatArrayOf(-depth * 0.70f, depth * 0.70f)) {
                                        walls.addBox(postX, -height * 0.48f, postZ, 0.12f, height * 0.48f, 0.12f, 1f)
                                    }
                                }
                                roofs.pushTrs(0f, height, 0f, 0f, 0f, 0.48f, 1f)
                                roofs.addBox(-width * 0.38f, 0f, 0f, width * 0.64f, 0.14f, depth * 0.68f, 1f)
                                roofs.pop()
                                roofs.pushTrs(0f, height, 0f, 0f, 0f, -0.48f, 1f)
                                roofs.addBox(width * 0.38f, 0f, 0f, width * 0.64f, 0.14f, depth * 0.68f, 1f)
                                roofs.pop()
                            }
                            WorldId.MOUNTAIN, WorldId.FOREST -> {
                                roofs.pushTrs(0f, height, 0f, 0f, 0f, 0.56f, 1f)
                                roofs.addBox(-width * 0.40f, 0f, 0f, width * 0.62f, 0.16f, depth * 0.70f, 1f)
                                roofs.pop()
                                roofs.pushTrs(0f, height, 0f, 0f, 0f, -0.56f, 1f)
                                roofs.addBox(width * 0.40f, 0f, 0f, width * 0.62f, 0.16f, depth * 0.70f, 1f)
                                roofs.pop()
                            }
                        }
                        walls.pop()
                        // Doors, window frames and a small porch create recognisable
                        // inhabited scale without adding separate scene objects.
                        details.pushTrs(x, ground, z, 0f, rotation, 0f, 1f)
                        details.addBox(0f, 0.86f, -depth - 0.035f, 0.48f, 0.86f, 0.10f, 1f)
                        details.addBox(-width * 0.52f, height * 0.70f, -depth - 0.04f, 0.42f, 0.36f, 0.08f, 1f)
                        details.addBox(width * 0.52f, height * 0.70f, -depth - 0.04f, 0.42f, 0.36f, 0.08f, 1f)
                        details.pop()
                    }
                }
            }
        }
        return SettlementGeometry(walls.build(), roofs.build(), details.build())
    }
}
