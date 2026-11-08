package com.buildlaunchfly.game.world

import android.content.res.AssetManager
import com.buildlaunchfly.game.gfx.AssetTextures
import com.buildlaunchfly.game.gfx.Camera
import com.buildlaunchfly.game.gfx.InstancedMesh
import com.buildlaunchfly.game.gfx.InstanceSet
import com.buildlaunchfly.game.gfx.Material
import com.buildlaunchfly.game.gfx.Mesh
import com.buildlaunchfly.game.gfx.MeshBuilder
import com.buildlaunchfly.game.gfx.MeshData
import com.buildlaunchfly.game.gfx.ObjLoader
import com.buildlaunchfly.game.gfx.ParticleRenderer
import com.buildlaunchfly.game.gfx.TextureFactory
import com.buildlaunchfly.game.math.M
import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3
import kotlin.random.Random

/** Which plant family a realm grows; the same scatter rules, different silhouettes. */
private enum class PlantStyle { FOREST, DESERT, ALPINE, COAST }

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
    detailLevel: Int = 2,
    private val assets: AssetManager? = null
) {

    val terrain = Terrain(definition)

    /** Realm-specific plant family: cacti and dead trees in the desert, palms on the coast. */
    private val plantStyle: PlantStyle = when (definition.id) {
        WorldId.DESERT -> PlantStyle.DESERT
        WorldId.SNOW, WorldId.MOUNTAIN -> PlantStyle.ALPINE
        WorldId.OCEAN -> PlantStyle.COAST
        WorldId.FOREST -> PlantStyle.FOREST
    }

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

    /**
     * Hand-authored Quaternius trees, scattered only in the near field where the player
     * actually looks; the procedural scatter still fills the distance at a fraction of the
     * triangle cost.
     */
    val heroTrees: List<TreePart>

    /** Hand-authored bushes, flowers, grass and rocks for the near field. */
    val heroPlants: List<InstancedPart>

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
            val canopyData = buildCanopyData(plantStyle, species)
            val trunkMesh = Mesh("treeTrunk$species", trunkData)
            val canopyMesh = Mesh("treeCanopy$species", canopyData)
            val trunks = if (showsTrunk(plantStyle, species)) {
                InstancedMesh("treeTrunks$species", trunkData, set.count).also {
                    it.setInstances(set.matrices, set.count)
                }
            } else {
                InstancedMesh("treeTrunks$species", trunkData, 1)
            }
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
        shrubs = buildParts(shrubSets, "shrub", listOf(shrubMaterial)) {
            buildShrubData(plantStyle, it)
        }

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
        grassParts = buildParts(grassSets, "grass", grassMaterials) {
            buildGrassData(plantStyle, it)
        }

        // ---- near-field hand-authored props ----
        val heroTreeBuilder = ArrayList<TreePart>(2)
        val heroPlantBuilder = ArrayList<InstancedPart>(5)
        if (assets != null && detailLevel >= 2) {
            buildHeroProps(assets, textures, heroTreeBuilder, heroPlantBuilder)
        }
        heroTrees = heroTreeBuilder
        heroPlants = heroPlantBuilder

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
        for (tree in heroTrees) {
            tree.trunk.mesh.dispose()
            tree.trunk.instanced.dispose()
            tree.canopy.mesh.dispose()
            tree.canopy.instanced.dispose()
        }
        for (part in heroPlants) {
            part.mesh.dispose()
            part.instanced.dispose()
        }
        birdsMesh.dispose()
        animalsMesh.dispose()
    }

    // ---- geometry ---------------------------------------------------------

    /** Palms and cacti carry their own stems; only the plain trees use the shared trunk mesh. */
    private fun showsTrunk(style: PlantStyle, species: Int): Boolean = when (style) {
        PlantStyle.DESERT -> species == 1
        PlantStyle.COAST -> species != 0
        else -> true
    }

    // ---- hand-authored near-field props -----------------------------------

    /** One asset file plus how many to place and how tall it grows, in metres. */
    private class HeroSpec(
        val file: String,
        val texture: String,
        val count: Int,
        val height: Float,
        val seedOffset: Int
    )

    private fun heroTreeSpecs(): List<HeroSpec> = when (plantStyle) {
        PlantStyle.FOREST -> listOf(
            HeroSpec("BirchTree_1", "BirchTree_Leaves.png", 10, 7.0f, 1),
            HeroSpec("BirchTree_2", "BirchTree_Leaves.png", 9, 6.5f, 21),
            HeroSpec("MapleTree_1", "MapleTree_Leaves.png", 9, 7.5f, 2),
            HeroSpec("MapleTree_2", "MapleTree_Leaves.png", 8, 8.0f, 22)
        )
        PlantStyle.ALPINE -> listOf(
            HeroSpec("PineTree_1", "PineTree_Leaves.png", 12, 7.5f, 3),
            HeroSpec("PineTree_2", "PineTree_Leaves.png", 10, 6.8f, 23)
        )
        PlantStyle.COAST -> listOf(
            HeroSpec("PalmTree_1", "PalmTree_Leaves.png", 8, 8.5f, 4),
            HeroSpec("PalmTree_2", "PalmTree_Leaves.png", 7, 7.5f, 24)
        )
        PlantStyle.DESERT -> emptyList()
    }

    private fun heroPlantSpecs(): List<HeroSpec> {
        val plants = ArrayList<HeroSpec>(7)
        when (plantStyle) {
            PlantStyle.FOREST -> {
                plants.add(HeroSpec("Bush", "Bush_Leaves.png", 50, 1.5f, 11))
                plants.add(HeroSpec("Bush_Small", "Bush_Leaves.png", 40, 0.9f, 27))
                plants.add(HeroSpec("Flower_1_Clump", "Flowers.png", 40, 0.7f, 12))
                plants.add(HeroSpec("Flower_2_Clump", "Flowers.png", 30, 0.6f, 28))
                plants.add(HeroSpec("Grass_Large", "Grass.png", 50, 1.0f, 13))
            }
            PlantStyle.ALPINE -> {
                plants.add(HeroSpec("Bush", "Bush_Leaves.png", 34, 1.2f, 14))
                plants.add(HeroSpec("Grass_Large", "Grass.png", 40, 0.9f, 15))
            }
            PlantStyle.COAST -> {
                plants.add(HeroSpec("Bush", "Bush_Leaves.png", 30, 1.5f, 16))
                plants.add(HeroSpec("Grass_Large", "Grass.png", 50, 1.3f, 17))
            }
            PlantStyle.DESERT -> Unit
        }
        plants.add(HeroSpec("Rock_1", "Rocks.png", 20, 2.0f, 18))
        plants.add(HeroSpec("Rock_2", "Rocks.png", 14, 1.6f, 29))
        return plants
    }

    private fun buildHeroProps(
        assets: AssetManager,
        textures: TextureFactory,
        outTrees: MutableList<TreePart>,
        outPlants: MutableList<InstancedPart>
    ) {
        val trunkMaterial = Material(
            albedo = textures.bark,
            tint = Vec3(0.72f, 0.56f, 0.40f),
            roughness = 0.90f,
            metallic = 0f,
            uvScale = 2f
        )

        for (spec in heroTreeSpecs()) {
            val model = ObjLoader.load(assets, "quaternius/${spec.file}.obj") ?: continue
            val barkData = model.bark ?: continue
            val foliageData = model.foliage ?: continue
            val foliageTexture =
                AssetTextures.load(assets, "quaternius/${spec.texture}") ?: continue

            val set = heroScatter(spec.count, spec.seedOffset, spec.height)
            if (set.count == 0) continue

            val trunkMesh = Mesh("heroTrunk${spec.file}", barkData)
            val canopyMesh = Mesh("heroCanopy${spec.file}", foliageData)
            val trunks = InstancedMesh("heroTrunks${spec.file}", barkData, set.count)
            trunks.setInstances(set.matrices, set.count)
            val canopies = InstancedMesh("heroCanopies${spec.file}", foliageData, set.count)
            canopies.setInstances(set.matrices, set.count)

            val foliageMaterial = Material(
                albedo = foliageTexture,
                tint = Vec3(1f, 1f, 1f),
                roughness = 0.86f,
                metallic = 0f,
                uvScale = 1f
            )
            outTrees.add(
                TreePart(
                    InstancedPart(trunkMesh, trunks, trunkMaterial),
                    InstancedPart(canopyMesh, canopies, foliageMaterial)
                )
            )
        }

        for (spec in heroPlantSpecs()) {
            val model = ObjLoader.load(assets, "quaternius/${spec.file}.obj") ?: continue
            val data = model.foliage ?: model.bark ?: continue
            val texture = AssetTextures.load(assets, "quaternius/${spec.texture}") ?: continue

            val set = heroScatter(spec.count, spec.seedOffset, spec.height)
            if (set.count == 0) continue

            val mesh = Mesh("heroProp${spec.file}", data)
            val instanced = InstancedMesh("heroProps${spec.file}", data, set.count)
            instanced.setInstances(set.matrices, set.count)
            val material = Material(
                albedo = texture,
                tint = Vec3(1f, 1f, 1f),
                roughness = 0.88f,
                metallic = 0f,
                uvScale = 1f
            )
            outPlants.add(InstancedPart(mesh, instanced, material))
        }

        // The desert has no leafy trees in this pack; its dead trees are bark-only models.
        if (plantStyle == PlantStyle.DESERT) {
            for (spec in listOf(
                HeroSpec("DeadTree_1", "", 10, 5.5f, 31),
                HeroSpec("DeadTree_2", "", 8, 6.5f, 32)
            )) {
                val model = ObjLoader.load(assets, "quaternius/${spec.file}.obj") ?: continue
                val data = model.bark ?: model.foliage ?: continue
                val set = heroScatter(spec.count, spec.seedOffset, spec.height)
                if (set.count == 0) continue
                val mesh = Mesh("heroProp${spec.file}", data)
                val instanced = InstancedMesh("heroProps${spec.file}", data, set.count)
                instanced.setInstances(set.matrices, set.count)
                outPlants.add(InstancedPart(mesh, instanced, trunkMaterial))
            }
        }
    }

    /** Places [count] props in a ring around the launch area, avoiding pad and water. */
    private fun heroScatter(count: Int, seedOffset: Int, height: Float): InstanceSet {
        if (count <= 0) return InstanceSet.EMPTY
        val out = FloatArray(count * 16)
        val rng = Random(definition.seed + 7919 + seedOffset)
        val normal = Vec3()
        val matrix = Mat4()
        var placed = 0
        var attempts = 0
        val maxAttempts = count * 24

        while (placed < count && attempts < maxAttempts) {
            attempts++
            val angle = rng.nextFloat() * M.TWO_PI
            val radius = 20f + rng.nextFloat() * 70f
            val x = kotlin.math.cos(angle) * radius
            val z = -55f + kotlin.math.sin(angle) * radius
            if (x * x + z * z < 26f * 26f) continue

            val h = terrain.height(x, z)
            if (h < definition.waterLevel + 1.5f) continue
            terrain.normal(x, z, normal)
            if (normal.y < 0.84f) continue

            val scale = height * (0.85f + rng.nextFloat() * 0.30f)
            matrix.setTrs(
                x, h - 0.12f, z,
                0f, rng.nextFloat() * M.TWO_PI, 0f,
                scale, scale, scale
            )
            System.arraycopy(matrix.m, 0, out, placed * 16, 16)
            placed++
        }

        return if (placed == 0) InstanceSet.EMPTY else InstanceSet(placed, out)
    }

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

    /**
     * One canopy per species, and the silhouette set changes with the realm: broadleaf,
     * conifer and poplar in temperate worlds; saguaro and dead trees in the desert; palms on
     * the coast; and three conifer forms in the alpine worlds.
     */
    private fun buildCanopyData(style: PlantStyle, species: Int): MeshData {
        val builder = MeshBuilder()
        when (style) {
            PlantStyle.DESERT -> when (species) {
               1 -> {
                    // Bare, wind-worn desert tree; the shared trunk carries it.
                    builder.addRod(0f, 0.62f, 0f, -0.42f, 1.02f, 0.18f, 0.045f, 6)
                    builder.addRod(0f, 0.78f, 0f, 0.38f, 1.12f, -0.12f, 0.04f, 6)
                    builder.addRod(0f, 0.55f, 0f, 0.30f, 0.82f, 0.24f, 0.035f, 6)
                }
                2 -> {
                    // Low desert scrub.
                    builder.addSphere(0f, 0.30f, 0f, 0.40f, 5, 8, 1.2f)
                    builder.addSphere(0.24f, 0.24f, 0.10f, 0.26f, 4, 7, 1.2f)
                }
                else -> {
                    // Saguaro: ribbed column with two arms.
                    builder.addCylinder(0f, 0.55f, 0f, 0.155f, 0.135f, 1.10f, 9, true, true, 1.2f)
                    builder.addRod(0f, 0.52f, 0f, -0.22f, 0.52f, 0f, 0.09f, 7)
                    builder.addRod(-0.22f, 0.52f, 0f, -0.22f, 0.90f, 0f, 0.09f, 7)
                    builder.addRod(0f, 0.70f, 0f, 0.20f, 0.70f, 0f, 0.085f, 7)
                    builder.addRod(0.20f, 0.70f, 0f, 0.20f, 1.02f, 0f, 0.085f, 7)
                    builder.addSphere(0f, 1.09f, 0f, 0.15f, 4, 8)
                }
            }
            PlantStyle.COAST -> when (species) {
                0 -> {
                    // Palm: leaning trunk with a crown of fronds.
                    builder.pushTrs(0f, 0f, 0f, 0f, 0f, -0.14f, 1f)
                    builder.addCylinder(0f, 0.62f, 0f, 0.10f, 0.07f, 1.24f, 8, true, true, 1.2f)
                    builder.pop()
                    for (i in 0 until 6) {
                        builder.pushTrs(0.02f, 1.18f, 0f, 0f, i * M.TWO_PI / 6f, 0.45f, 1f)
                        builder.addEllipsoid(0.42f, 0f, 0f, 0.44f, 0.045f, 0.13f, 4, 6, 1.2f)
                        builder.pop()
                    }
                }
                1 -> {
                    builder.addSphere(0f, 1.02f, 0f, 0.50f, 6, 9, 1.4f)
                    builder.addSphere(0.22f, 0.86f, 0.12f, 0.34f, 5, 8, 1.4f)
                    builder.addSphere(-0.20f, 0.92f, -0.14f, 0.31f, 5, 8, 1.4f)
                }
                else -> {
                    builder.addEllipsoid(0f, 1.02f, 0f, 0.34f, 0.92f, 0.34f, 7, 10, 1.4f)
                    builder.addEllipsoid(0.09f, 1.52f, 0.05f, 0.20f, 0.42f, 0.20f, 5, 8, 1.4f)
                }
            }
            PlantStyle.ALPINE -> when (species) {
                0 -> {
                    builder.addCone(0f, 0.30f, 0f, 0.46f, 1.05f, 8, 1.5f)
                    builder.addCone(0f, 0.86f, 0f, 0.34f, 0.90f, 8, 1.5f)
                    builder.addCone(0f, 1.36f, 0f, 0.20f, 0.66f, 7, 1.5f)
                }
                1 -> {
                    builder.addCone(0f, 0.26f, 0f, 0.54f, 0.95f, 9, 1.5f)
                    builder.addCone(0f, 0.70f, 0f, 0.41f, 0.80f, 9, 1.5f)
                    builder.addCone(0f, 1.06f, 0f, 0.26f, 0.64f, 8, 1.5f)
                }
                else -> {
                    // Low, wide pine with a rounded crown.
                    builder.addCone(0f, 0f, 0f, 0.62f, 0.85f, 9, 1.5f)
                    builder.addSphere(0f, 0.88f, 0f, 0.30f, 5, 8, 1.3f)
                }
            }
            PlantStyle.FOREST -> when (species) {
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
        }
        return builder.build()
    }

    /** 0 low round bush, 1 wide creeping scrub, 2 spiky. */
    private fun buildShrubData(style: PlantStyle, species: Int): MeshData {
        val builder = MeshBuilder()
        when (style) {
            PlantStyle.DESERT -> when (species) {
                1 -> {
                    builder.addSphere(0f, 0.26f, 0f, 0.34f, 5, 8, 1.2f)
                    builder.addSphere(0.20f, 0.20f, 0.08f, 0.22f, 4, 7, 1.2f)
                }
                2 -> {
                    builder.addCone(0f, 0f, 0f, 0.32f, 0.58f, 8, 1.2f)
                    builder.addCone(0.18f, 0f, 0.10f, 0.22f, 0.42f, 7, 1.2f)
                }
                else -> {
                    // Small barrel cactus.
                    builder.addCylinder(0f, 0.24f, 0f, 0.24f, 0.20f, 0.48f, 9, true, true, 1.2f)
                    builder.addSphere(0f, 0.48f, 0f, 0.20f, 4, 8)
                    builder.addRod(0f, 0.30f, 0f, -0.26f, 0.44f, 0f, 0.06f, 6)
                }
            }
            PlantStyle.COAST -> when (species) {
                0 -> {
                    // Reeds: a tight cluster of narrow blades.
                    for (i in 0 until 5) {
                        val a = i * M.TWO_PI / 5f
                        val cx = kotlin.math.cos(a) * 0.10f
                        val cz = kotlin.math.sin(a) * 0.10f
                        builder.addCone(cx, 0f, cz, 0.05f, 0.95f, 5, 1f)
                    }
                }
                1 -> {
                    builder.addEllipsoid(0f, 0.20f, 0f, 0.58f, 0.24f, 0.46f, 5, 9, 1.2f)
                    builder.addEllipsoid(0.34f, 0.16f, 0.16f, 0.34f, 0.18f, 0.30f, 4, 8, 1.2f)
                }
                else -> {
                    builder.addSphere(0f, 0.30f, 0f, 0.38f, 5, 9, 1.2f)
                    builder.addSphere(0.22f, 0.24f, 0.10f, 0.26f, 4, 8, 1.2f)
                }
            }
            PlantStyle.ALPINE -> when (species) {
                0 -> {
                    builder.addCone(0f, 0f, 0f, 0.42f, 0.55f, 8, 1.2f)
                    builder.addCone(0f, 0.30f, 0f, 0.26f, 0.40f, 7, 1.2f)
                }
                1 -> {
                    builder.addEllipsoid(0f, 0.20f, 0f, 0.58f, 0.24f, 0.46f, 5, 9, 1.2f)
                    builder.addEllipsoid(0.34f, 0.16f, 0.16f, 0.34f, 0.18f, 0.30f, 4, 8, 1.2f)
                }
                else -> {
                    builder.addCone(0f, 0f, 0f, 0.38f, 0.62f, 8, 1.2f)
                    builder.addCone(0.22f, 0f, 0.12f, 0.26f, 0.48f, 7, 1.2f)
                }
            }
            PlantStyle.FOREST -> when (species) {
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

    /** 0 crossed tall blades, 1 narrow tuft, 2 low clump; desert is shorter, coast grows reeds. */
    private fun buildGrassData(style: PlantStyle, species: Int): MeshData {
        val builder = MeshBuilder()
        val heightScale = if (style == PlantStyle.DESERT) 0.50f else 1f

        if (style == PlantStyle.COAST && species == 0) {
            // Reeds: tall narrow blades at a coastal waterline.
            for (i in 0 until 4) {
                val offset = (i - 1.5f) * 0.12f
                val tall = 1.35f + (i % 2) * 0.15f
                builder.addQuad(
                    Vec3(-0.07f + offset, 0f, offset),
                    Vec3(0.07f + offset, 0f, offset),
                    Vec3(0.07f + offset, tall, offset),
                    Vec3(-0.07f + offset, tall, offset)
                )
            }
            return builder.build()
        }

        when (species) {
            1 -> {
                for (i in 0 until 3) {
                    val offset = (i - 1) * 0.14f
                    builder.addQuad(
                        Vec3(-0.10f + offset, 0f, offset),
                        Vec3(0.10f + offset, 0f, offset),
                        Vec3(0.10f + offset, heightScale, offset),
                        Vec3(-0.10f + offset, heightScale, offset)
                    )
                }
            }
            2 -> {
                val h = 0.55f * heightScale
                builder.addQuad(
                    Vec3(-0.24f, 0f, 0f), Vec3(0.24f, 0f, 0f),
                    Vec3(0.20f, h, 0f), Vec3(-0.20f, h, 0f)
                )
                builder.addQuad(
                    Vec3(0f, 0f, -0.24f), Vec3(0f, 0f, 0.24f),
                    Vec3(0f, h * 0.92f, 0.20f), Vec3(0f, h * 0.92f, -0.20f)
                )
            }
            else -> {
                builder.addQuad(
                    Vec3(-0.26f, 0f, 0f), Vec3(0.26f, 0f, 0f),
                    Vec3(0.26f, heightScale, 0f), Vec3(-0.26f, heightScale, 0f)
                )
                builder.addQuad(
                    Vec3(0f, 0f, -0.26f), Vec3(0f, 0f, 0.26f),
                    Vec3(0f, heightScale, 0.26f), Vec3(0f, heightScale, -0.26f)
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
