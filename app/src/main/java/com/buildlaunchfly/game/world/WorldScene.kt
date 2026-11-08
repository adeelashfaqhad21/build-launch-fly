package com.buildlaunchfly.game.world

import com.buildlaunchfly.game.gfx.Camera
import com.buildlaunchfly.game.gfx.InstancedMesh
import com.buildlaunchfly.game.gfx.InstanceSet
import com.buildlaunchfly.game.gfx.Material
import com.buildlaunchfly.game.gfx.Mesh
import com.buildlaunchfly.game.gfx.MeshBuilder
import com.buildlaunchfly.game.gfx.MeshData
import com.buildlaunchfly.game.gfx.ParticleRenderer
import com.buildlaunchfly.game.gfx.TextureFactory
import com.buildlaunchfly.game.math.Vec3

/** One instanced mesh with the material it is drawn with. */
class InstancedPart(
    val mesh: Mesh,
    val instanced: InstancedMesh,
    val material: Material
)

/** A tree species: its own trunk and canopy instances, drawn with their own materials. */
class TreePart(
    val trunk: InstancedPart,
    val canopy: InstancedPart
)

/**
 * Everything that makes up one playable world: terrain, water, vegetation, wildlife and
 * weather. Built on the GL thread; the caller owns disposal.
 *
 * Vegetation is split by species — three tree shapes, three shrubs, three rock shapes and
 * three grass clumps — so a world reads as a mixed habitat rather than a field of clones.
 */
class WorldScene(
    val definition: WorldDefinition,
    textures: TextureFactory,
    detailLevel: Int = 2
) {

    val terrain = Terrain(definition)
    val terrainMesh: Mesh
    val waterMesh: Mesh?

    /** Tree species present in this world, each with its own trunk and canopy. */
    val trees: List<TreePart>

    /** Bushes and low scrub, no trunk. */
    val shrubs: List<InstancedPart>

    /** Boulder, outcrop and slab shapes. */
    val rockParts: List<InstancedPart>

    /** Tall meadow grass, split into three clump shapes. */
    val grassParts: List<InstancedPart>

    val birdMaterial: Material
    val animalMaterial: Material

    val birdsMesh: InstancedMesh
    val animalsMesh: InstancedMesh

    val birds: Wildlife.Birds
    val animals: Wildlife.GroundAnimals
    val weather = WeatherSystem(definition)

    val waterDeep: Vec3
    val waterShallow: Vec3

    init {
        val scale = when {
            detailLevel <= 0 -> 0.35f
            detailLevel == 1 -> 0.7f
            detailLevel == 2 -> 1f
            else -> 1.25f
        }

        terrainMesh = Mesh("terrain", TerrainMesh.build(terrain))
        waterMesh = if (definition.hasWater) Mesh("water", buildWaterData()) else null

        val trunkData = buildTrunkData()

        val trunkMaterial = Material(
            albedo = textures.bark,
            tint = Vec3(0.82f, 0.76f, 0.68f),
            roughness = 0.92f,
            metallic = 0f,
            uvScale = 2f
        )
        val canopyMaterials = listOf(
            // Broadleaf.
            Material(
                albedo = textures.leaf,
                tint = Vec3(0.98f, 1.06f, 0.80f),
                roughness = 0.85f,
                metallic = 0f,
                uvScale = 2.5f
            ),
            // Conifer.
            Material(
                albedo = textures.leaf,
                tint = Vec3(0.60f, 0.86f, 0.64f),
                roughness = 0.88f,
                metallic = 0f,
                uvScale = 2.5f
            ),
            // Poplar, a little yellower.
            Material(
                albedo = textures.leaf,
                tint = Vec3(0.88f, 1.04f, 0.62f),
                roughness = 0.86f,
                metallic = 0f,
                uvScale = 2.5f
            )
        )
        val shrubMaterial = Material(
            albedo = textures.leaf,
            tint = Vec3(0.86f, 1.0f, 0.66f),
            roughness = 0.90f,
            metallic = 0f,
            uvScale = 1.8f
        )
        val rockMaterials = listOf(
            Material(
                albedo = textures.surface(definition.surfaceB),
                tint = definition.tintB,
                roughness = 0.95f,
                metallic = 0f,
                uvScale = 1.6f
            ),
            Material(
                albedo = textures.surface(definition.surfaceB),
                tint = multiply(definition.tintB, 0.86f),
                roughness = 0.97f,
                metallic = 0f,
                uvScale = 1.2f
            ),
            Material(
                albedo = textures.surface(definition.surfaceB),
                tint = multiply(definition.tintB, 1.08f),
                roughness = 0.93f,
                metallic = 0f,
                uvScale = 2.1f
            )
        )
        val grassMaterials = listOf(
            Material(
                albedo = textures.leaf,
                tint = Vec3(0.92f, 1.08f, 0.72f),
                roughness = 0.90f,
                metallic = 0f,
                uvScale = 1f
            ),
            Material(
                albedo = textures.leaf,
                tint = Vec3(0.78f, 0.96f, 0.58f),
                roughness = 0.92f,
                metallic = 0f,
                uvScale = 1f
            ),
            Material(
                albedo = textures.leaf,
                tint = Vec3(1.02f, 1.02f, 0.66f),
                roughness = 0.91f,
                metallic = 0f,
                uvScale = 1f
            )
        )

        // ---- trees: one instanced pair per species ----
        val treeSets = Vegetation.scatterTrees(
            terrain, definition, (definition.treeCount * scale).toInt(), definition.seed
        )
        val treeBuilder = ArrayList<TreePart>(treeSets.size)
        for (species in treeSets.indices) {
            val set = treeSets[species]
            if (set.count == 0) continue
            val canopyData = buildCanopyData(species)
            val trunkMesh = Mesh("treeTrunk$species", trunkData)
            val canopyMesh = Mesh("treeCanopy$species", canopyData)
            val trunks = InstancedMesh("treeTrunks$species", trunkData, set.count)
            trunks.setInstances(set.matrices, set.count)
            val canopies = InstancedMesh("treeCanopies$species", canopyData, set.count)
            canopies.setInstances(set.matrices, set.count)
            treeBuilder.add(
                TreePart(
                    InstancedPart(trunkMesh, trunks, trunkMaterial),
                    InstancedPart(
                        canopyMesh,
                        canopies,
                        canopyMaterials[species.coerceIn(0, canopyMaterials.size - 1)]
                    )
                )
            )
        }
        trees = treeBuilder

        // ---- shrubs ----
        val shrubSets = Vegetation.scatterShrubs(
            terrain,
            definition,
            (definition.treeCount * 0.55f * scale).toInt().coerceAtLeast(0),
            definition.seed
        )
        shrubs = buildParts(shrubSets, "shrub", listOf(shrubMaterial)) { buildShrubData(it) }

        // ---- rocks ----
        val rockSets = Vegetation.scatterRocks(
            terrain, definition, (definition.rockCount * scale).toInt(), definition.seed
        )
        rockParts = buildParts(rockSets, "rock", rockMaterials) { buildRockData(it) }

        // ---- grass meadows ----
        val grassSets = Vegetation.scatterGrass(
            terrain,
            definition,
            (definition.grassCount * scale).toInt(),
            definition.seed,
            0f,
            -90f,
            320f
        )
        grassParts = buildParts(grassSets, "grass", grassMaterials) { buildGrassData(it) }

        birdMaterial = Material(
            albedo = textures.rubber,
            tint = Vec3(0.16f, 0.15f, 0.16f),
            roughness = 0.85f,
            metallic = 0f,
            uvScale = 1f
        )
        animalMaterial = Material(
            albedo = textures.bark,
            tint = Vec3(0.60f, 0.46f, 0.34f),
            roughness = 0.9f,
            metallic = 0f,
            uvScale = 1f
        )

        val birdCount = (definition.birdCount * scale).toInt().coerceAtLeast(0)
        val animalCount = (definition.animalCount * scale).toInt().coerceAtLeast(0)

        birds = Wildlife.Birds(
            birdCount,
            Vec3(0f, terrain.padHeight + 20f, -220f),
            definition.seed
        )
        animals = Wildlife.GroundAnimals(
            animalCount,
            Vec3(0f, terrain.padHeight, -90f),
            definition.seed
        )

        birdsMesh = InstancedMesh("birds", buildBirdData(), birdCount.coerceAtLeast(1))
        animalsMesh = InstancedMesh("animals", buildAnimalData(), animalCount.coerceAtLeast(1))

        waterDeep = definition.waterDeep
        waterShallow = definition.waterShallow
    }

    fun update(dt: Float, time: Float, camera: Camera, particles: ParticleRenderer) {
        if (birds.instanceCount > 0) {
            birds.update(dt, time)
            birdsMesh.setInstances(birds.matrices(), birds.instanceCount)
        }
        if (animals.instanceCount > 0) {
            animals.update(dt, time, terrain)
            animalsMesh.setInstances(animals.matrices(), animals.instanceCount)
        }
        weather.updateAndEmit(camera, dt, time, particles)
    }

    fun dispose() {
        terrainMesh.dispose()
        waterMesh?.dispose()
        for (tree in trees) {
            tree.trunk.mesh.dispose()
            tree.trunk.instanced.dispose()
            tree.canopy.mesh.dispose()
            tree.canopy.instanced.dispose()
        }
        for (part in shrubs) {
            part.mesh.dispose()
            part.instanced.dispose()
        }
        for (part in rockParts) {
            part.mesh.dispose()
            part.instanced.dispose()
        }
        for (part in grassParts) {
            part.mesh.dispose()
            part.instanced.dispose()
        }
        birdsMesh.dispose()
        animalsMesh.dispose()
    }

    // ---- geometry ---------------------------------------------------------

    /**
     * Builds one mesh + instance set per species, skipping empty sets and indexing the
     * per-species material list.
     */
    private fun buildParts(
        sets: List<InstanceSet>,
        label: String,
        materials: List<Material>,
        build: (Int) -> MeshData
    ): List<InstancedPart> {
        val parts = ArrayList<InstancedPart>(sets.size)
        if (materials.isEmpty()) return parts
        for (species in sets.indices) {
            val set = sets[species]
            if (set.count == 0) continue
            val data = build(species)
            val mesh = Mesh("$label$species", data)
            val instanced = InstancedMesh("$label$species", data, set.count)
            instanced.setInstances(set.matrices, set.count)
            val material = materials[species.coerceIn(0, materials.size - 1)]
            parts.add(InstancedPart(mesh, instanced, material))
        }
        return parts
    }

    private fun multiply(color: Vec3, factor: Float): Vec3 =
        Vec3(color.x * factor, color.y * factor, color.z * factor)

    private fun buildWaterData(): MeshData {
        val y = definition.waterLevel
        val builder = MeshBuilder()
        builder.addQuad(
            Vec3(-3600f, y, 1600f),
            Vec3(5200f, y, 1600f),
            Vec3(5200f, y, -5200f),
            Vec3(-3600f, y, -5200f),
            uvScale = 1f
        )
        return builder.build()
    }

    private fun buildTrunkData(): MeshData {
        val builder = MeshBuilder()
        // Unit-height trunk with its base at y = 0 so instances scale cleanly.
        builder.addCylinder(0f, 0.5f, 0f, 0.085f, 0.055f, 1f, 7, false, false, 1f)
        return builder.build()
    }

    /** 0 broadleaf, 1 conifer, 2 poplar. */
    private fun buildCanopyData(species: Int): MeshData {
        val builder = MeshBuilder()
        when (species) {
            1 -> {
                builder.addCone(0f, 0.26f, 0f, 0.54f, 0.95f, 9, 1.5f)
                builder.addCone(0f, 0.70f, 0f, 0.41f, 0.80f, 9, 1.5f)
                builder.addCone(0f, 1.06f, 0f, 0.26f, 0.64f, 8, 1.5f)
            }
            2 -> {
                builder.addEllipsoid(0f, 1.02f, 0f, 0.34f, 0.92f, 0.34f, 7, 10, 1.4f)
                builder.addEllipsoid(0.09f, 1.52f, 0.05f, 0.20f, 0.42f, 0.20f, 5, 8, 1.4f)
            }
            else -> {
                builder.addSphere(0f, 1.02f, 0f, 0.50f, 6, 9, 1.4f)
                builder.addSphere(0.22f, 0.86f, 0.12f, 0.34f, 5, 8, 1.4f)
                builder.addSphere(-0.20f, 0.92f, -0.14f, 0.31f, 5, 8, 1.4f)
            }
        }
        return builder.build()
    }

    /** 0 low round bush, 1 wide creeping scrub, 2 spiky. */
    private fun buildShrubData(species: Int): MeshData {
        val builder = MeshBuilder()
        when (species) {
            1 -> {
                builder.addEllipsoid(0f, 0.20f, 0f, 0.58f, 0.24f, 0.46f, 5, 9, 1.2f)
                builder.addEllipsoid(0.34f, 0.16f, 0.16f, 0.34f, 0.18f, 0.30f, 4, 8, 1.2f)
            }
            2 -> {
                builder.addCone(0f, 0f, 0f, 0.38f, 0.62f, 8, 1.2f)
                builder.addCone(0.22f, 0f, 0.12f, 0.26f, 0.48f, 7, 1.2f)
                builder.addCone(-0.20f, 0f, -0.10f, 0.24f, 0.42f, 7, 1.2f)
            }
            else -> {
                builder.addSphere(0f, 0.30f, 0f, 0.38f, 5, 9, 1.2f)
                builder.addSphere(0.22f, 0.24f, 0.10f, 0.26f, 4, 8, 1.2f)
            }
        }
        return builder.build()
    }

    /** 0 rounded boulder, 1 blocky outcrop, 2 flat slab. */
    private fun buildRockData(species: Int): MeshData {
        val builder = MeshBuilder()
        when (species) {
            1 -> {
                builder.addBox(0f, 0.34f, 0f, 0.52f, 0.34f, 0.44f, 1.2f)
                builder.addBox(0.34f, 0.22f, 0.20f, 0.30f, 0.22f, 0.26f, 1.2f)
                builder.pushTrs(0f, 0.58f, 0f, 0.18f, 0.5f, 0.12f, 1f)
                builder.addBox(0f, 0f, 0f, 0.26f, 0.18f, 0.30f, 1.2f)
                builder.pop()
            }
            2 -> {
                builder.addBox(0f, 0.16f, 0f, 0.72f, 0.16f, 0.46f, 1.2f)
                builder.pushTrs(0.10f, 0.28f, 0f, 0f, 0.4f, 0.05f, 1f)
                builder.addBox(0f, 0f, 0f, 0.40f, 0.10f, 0.30f, 1.2f)
                builder.pop()
            }
            else -> {
                builder.addSphere(0f, 0.42f, 0f, 0.55f, 6, 9, 1.2f)
            }
        }
        return builder.build()
    }

    /** 0 crossed tall blades, 1 narrow tuft, 2 low clump. */
    private fun buildGrassData(species: Int): MeshData {
        val builder = MeshBuilder()
        when (species) {
            1 -> {
                for (i in 0 until 3) {
                    val offset = (i - 1) * 0.14f
                    builder.addQuad(
                        Vec3(-0.10f + offset, 0f, offset),
                        Vec3(0.10f + offset, 0f, offset),
                        Vec3(0.10f + offset, 1f, offset),
                        Vec3(-0.10f + offset, 1f, offset)
                    )
                }
            }
            2 -> {
                builder.addQuad(
                    Vec3(-0.24f, 0f, 0f), Vec3(0.24f, 0f, 0f),
                    Vec3(0.20f, 0.55f, 0f), Vec3(-0.20f, 0.55f, 0f)
                )
                builder.addQuad(
                    Vec3(0f, 0f, -0.24f), Vec3(0f, 0f, 0.24f),
                    Vec3(0f, 0.50f, 0.20f), Vec3(0f, 0.50f, -0.20f)
                )
            }
            else -> {
                builder.addQuad(
                    Vec3(-0.26f, 0f, 0f), Vec3(0.26f, 0f, 0f),
                    Vec3(0.26f, 1f, 0f), Vec3(-0.26f, 1f, 0f)
                )
                builder.addQuad(
                    Vec3(0f, 0f, -0.26f), Vec3(0f, 0f, 0.26f),
                    Vec3(0f, 1f, 0.26f), Vec3(0f, 1f, -0.26f)
                )
            }
        }
        return builder.build()
    }

    private fun buildBirdData(): MeshData {
        val builder = MeshBuilder()
        builder.addBox(0f, 0f, 0f, 0.05f, 0.05f, 0.24f)
        builder.addBox(0.30f, 0f, 0f, 0.30f, 0.012f, 0.09f)
        builder.addBox(-0.30f, 0f, 0f, 0.30f, 0.012f, 0.09f)
        return builder.build()
    }

    private fun buildAnimalData(): MeshData {
        val builder = MeshBuilder()
        builder.addEllipsoid(0f, 0.48f, 0f, 0.26f, 0.22f, 0.52f, 5, 8)
        builder.addEllipsoid(0f, 0.66f, -0.42f, 0.15f, 0.15f, 0.20f, 4, 7)
        builder.addBox(0.16f, 0.20f, 0.30f, 0.05f, 0.20f, 0.05f)
        builder.addBox(-0.16f, 0.20f, 0.30f, 0.05f, 0.20f, 0.05f)
        builder.addBox(0.16f, 0.20f, -0.30f, 0.05f, 0.20f, 0.05f)
        builder.addBox(-0.16f, 0.20f, -0.30f, 0.05f, 0.20f, 0.05f)
        return builder.build()
    }
}
