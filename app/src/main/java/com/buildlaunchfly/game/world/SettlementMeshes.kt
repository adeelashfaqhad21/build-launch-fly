package com.buildlaunchfly.game.world

import com.buildlaunchfly.game.gfx.MeshBuilder
import com.buildlaunchfly.game.gfx.MeshData
import kotlin.random.Random

/** Three batched static meshes for the inhabited landmarks in each ecosystem. */
data class SettlementGeometry(
    val walls: MeshData,
    val roofs: MeshData,
    val details: MeshData,
    /** Collision records: centre XYZ, horizontal radius, vertical half-extent. */
    val obstacles: FloatArray
)

object SettlementMeshes {

    fun build(terrain: Terrain, definition: WorldDefinition, detailLevel: Int): SettlementGeometry {
        val walls = MeshBuilder()
        val roofs = MeshBuilder()
        val details = MeshBuilder()
        val obstacles = ArrayList<Float>()
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

                    // Houses and igloos are genuine flight hazards, not decorative
                    // geometry that the aircraft can pass through.
                    obstacles.add(x)
                    obstacles.add(ground + if (igloo) 1.35f else height * 0.52f)
                    obstacles.add(z)
                    obstacles.add(if (igloo) maxOf(width * 0.72f, depth * 0.70f) + 0.55f else maxOf(width, depth) * 0.82f + 0.65f)
                    obstacles.add(if (igloo) 1.55f else height * 0.72f + 0.45f)

                    if (igloo) {
                        // Low domed snow shelter with a recessed entrance.
                        roofs.pushTrs(x, ground + 0.02f, z, 0f, rotation, 0f, 1f)
                        roofs.addEllipsoid(0f, 1.35f, 0f, width * 0.72f, 1.34f, depth * 0.70f, 8, 12)
                        roofs.pop()
                        details.addBox(x, ground + 0.43f, z - depth * 0.62f, 0.52f, 0.48f, 0.12f, 1f)
                    } else {
                        walls.pushTrs(x, ground + height * 0.5f, z, 0f, rotation, 0f, 1f)
                        if (definition.id == WorldId.DESERT) {
                            // Traditional low-rise mud-brick home: connected rooms,
                            // irregular courtyard wall and a shaded entrance. The forms
                            // are broad, low and earthen rather than tall geometric towers.
                            walls.addBox(-width * 0.12f, 0f, 0f, width * 0.88f, height * 0.50f, depth * 0.78f, 1.25f)
                            walls.addBox(width * 0.66f, -height * 0.08f, depth * 0.28f, width * 0.34f, height * 0.42f, depth * 0.48f, 1.1f)
                            walls.addBox(-width * 0.95f, -height * 0.24f, -depth * 0.62f, 0.16f, height * 0.26f, depth * 0.12f, 1f)
                            walls.addBox(width * 0.62f, -height * 0.24f, -depth * 0.62f, 0.16f, height * 0.26f, depth * 0.12f, 1f)
                            walls.addBox(-width * 0.18f, -height * 0.24f, -depth * 1.02f, width * 0.80f, height * 0.26f, 0.14f, 1f)
                            // Roof pieces must share the house transform. Previously these
                            // boxes were emitted at world origin, stacking every desert roof
                            // into one strange tower-like cluster instead of sitting on homes.
                            // Low, irregular flat mud-brick roofs with modest parapets.
                            roofs.addBox(-width * 0.12f, height + 0.02f, 0f, width * 0.90f, 0.12f, depth * 0.80f, 1.2f)
                            roofs.addBox(width * 0.66f, height * 0.76f, depth * 0.28f, width * 0.36f, 0.10f, depth * 0.50f, 1.1f)
                            roofs.addBox(-width * 0.12f, height + 0.24f, -depth * 0.73f, width * 0.90f, 0.10f, 0.09f, 1f)
                            roofs.addBox(-width * 0.94f, height + 0.24f, -depth * 0.20f, 0.09f, 0.10f, depth * 0.55f, 1f)
                            // Timber/reed shade canopy over the recessed doorway.
                            roofs.addBox(-width * 0.18f, height * 0.72f, -depth * 1.20f, width * 0.42f, 0.055f, depth * 0.28f, 1f)
                            details.addCylinder(-width * 0.55f, height * 0.34f, -depth * 1.18f, 0.055f, 0.055f, height * 0.68f, 6, true, true, 1f)
                            details.addCylinder(width * 0.18f, height * 0.34f, -depth * 1.18f, 0.055f, 0.055f, height * 0.68f, 6, true, true, 1f)
                        } else {
                            walls.addBox(0f, 0f, 0f, width, height * 0.5f, depth, 2f)
                        }

                        // All roof meshes are authored in building-local coordinates.
                        // Anchor them to the same world transform as the corresponding walls.
                        roofs.pushTrs(x, ground, z, 0f, rotation, 0f, 1f)
                        when (definition.id) {
                            WorldId.DESERT -> Unit
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
                        roofs.pop()
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
        return SettlementGeometry(walls.build(), roofs.build(), details.build(), obstacles.toFloatArray())
    }
}
