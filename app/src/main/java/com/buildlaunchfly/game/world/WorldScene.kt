package com.buildlaunchfly.game.world

import android.content.res.AssetManager
import com.buildlaunchfly.game.gfx.AnimalLoader
import com.buildlaunchfly.game.gfx.AssetTextures
import com.buildlaunchfly.game.gfx.Camera
import com.buildlaunchfly.game.gfx.GlbLoader
import com.buildlaunchfly.game.gfx.InstancedMesh
import com.buildlaunchfly.game.gfx.InstanceSet
import com.buildlaunchfly.game.gfx.Material
import com.buildlaunchfly.game.gfx.Mesh
import com.buildlaunchfly.game.gfx.MeshBuilder
import com.buildlaunchfly.game.gfx.MeshData
import com.buildlaunchfly.game.gfx.ObjLoader
import com.buildlaunchfly.game.gfx.ParticleRenderer
import com.buildlaunchfly.game.gfx.PropLoader
import com.buildlaunchfly.game.gfx.RtsModelLoader
import com.buildlaunchfly.game.gfx.ZipKit
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
    private val detailLevel: Int = 2,
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

    /** Ship moorings, declared before `init` so the scene build can use them. */
    private val shipSpecs = listOf(
        ShipSpec("VikingBoat", 4.5f, -120f, -230f, 0.7f),
        ShipSpec("BoatWSail", 6.5f, 150f, -330f, 2.1f),
        ShipSpec("Lifeboat", 2.0f, -55f, -150f, 4.0f),
        ShipSpec("SailShip", 18f, -260f, -470f, 1.2f),
        ShipSpec("CruiseShip", 22f, 330f, -600f, 3.4f)
    )

    private val shipTransforms = FloatArray(maxOf(shipSpecs.size, 1) * 16)
    private val shipMatrix = Mat4()

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

    /**
     * Hand-authored animal meshes, one per material group. When present they replace the
     * procedural animal mesh; each species drives its own instance buffer through [herds].
     */
    val animalParts: List<InstancedPart>

    /**
     * One imported species per herd. Each species walks its own grazing ring, so a realm can
     * hold a deer and a fox at the same time instead of copies of one animal.
     */
    private var herds: List<Herd> = emptyList()

    /** Static ships anchored offshore: only the ocean realm has water for them. */
    val shipParts: List<InstancedPart>

    /** Hand-authored buildings, props and mountains placed on the terrain. */
    val structures: List<InstancedPart>

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
        // The imported packs are the world's detail layer, so they load at every quality
        // level; only the number of copies follows the detail setting. Leaving them behind
        // the HIGH/ULTRA gate is why the worlds looked bare on a mid-range phone.
        val heroTreeBuilder = ArrayList<TreePart>(2)
        val heroPlantBuilder = ArrayList<InstancedPart>(5)
        if (assets != null) {
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

        // The imported parrot (glTF binary) is the flock when it is in the APK; the older
        // BufferGeometry JSON and the procedural triangles stay as fallbacks so the sky is
        // never empty if an art file is missing.
        val importedBird = if (assets != null) {
            GlbLoader.load(assets, "birds/parrot.glb", BIRD_HEIGHT)
                ?: RtsModelLoader.load(assets, "birds/bird.json", BIRD_HEIGHT)
        } else {
            null
        }
        birdsMesh = InstancedMesh("birds", importedBird ?: buildBirdData(), birdCount.coerceAtLeast(1))
        animalsMesh = InstancedMesh("animals", buildAnimalData(), animalCount.coerceAtLeast(1))
        animalParts = if (assets != null) {
            buildAnimalParts(assets, textures, animalCount)
        } else {
            emptyList()
        }

        shipParts = if (assets != null && definition.id == WorldId.OCEAN) {
            buildShipParts(assets, textures)
        } else {
            emptyList()
        }

        structures = if (assets != null) {
            buildStructures(assets, textures) +
                buildKitDressing(assets) +
                buildFieldBoundaries(assets, textures)
        } else {
            emptyList()
        }

        waterDeep = definition.waterDeep
        waterShallow = definition.waterShallow
    }

    fun update(dt: Float, time: Float, camera: Camera, particles: ParticleRenderer) {
        if (birds.instanceCount > 0) {
            birds.update(dt, time)
            birdsMesh.setInstances(birds.matrices(), birds.instanceCount)
        }
        if (herds.isNotEmpty()) {
            // Every species moves on its own ring and writes its own instance buffer.
            for (herd in herds) {
                if (herd.group.instanceCount <= 0) continue
                herd.group.update(dt, time, terrain)
                val matrices = herd.group.matrices()
                for (part in herd.parts) {
                    part.instanced.setInstances(matrices, herd.group.instanceCount)
                }
            }
        } else if (animals.instanceCount > 0) {
            animals.update(dt, time, terrain)
            animalsMesh.setInstances(animals.matrices(), animals.instanceCount)
        }
        updateShips(time)
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
        for (part in animalParts) {
            part.mesh.dispose()
            part.instanced.dispose()
        }
        for (part in shipParts) {
            part.mesh.dispose()
            part.instanced.dispose()
        }
        for (part in structures) {
            part.mesh.dispose()
            part.instanced.dispose()
        }
    }

    // ---- geometry ---------------------------------------------------------

    /** Palms and cacti carry their own stems; only the plain trees use the shared trunk mesh. */
    private fun showsTrunk(style: PlantStyle, species: Int): Boolean = when (style) {
        PlantStyle.DESERT -> species == 1
        PlantStyle.COAST -> species != 0
        else -> true
    }

    // ---- hand-authored near-field props -----------------------------------

    /** One asset file plus how many to place, how tall it grows, and its bark map. */
    private class HeroSpec(
        val file: String,
        val texture: String,
        val count: Int,
        val height: Float,
        val seedOffset: Int,
        val bark: String = ""
    )

    private fun heroTreeSpecs(): List<HeroSpec> = when (plantStyle) {
        PlantStyle.FOREST -> listOf(
            HeroSpec("BirchTree_1", "BirchTree_Leaves.png", 8, 7.0f, 1, "BirchTree_Bark.jpg"),
            HeroSpec("BirchTree_2", "BirchTree_Leaves.png", 7, 6.5f, 21, "BirchTree_Bark.jpg"),
            HeroSpec("MapleTree_1", "MapleTree_Leaves.png", 7, 7.5f, 2, "MapleTree_Bark.jpg"),
            HeroSpec("MapleTree_2", "MapleTree_Leaves.png", 6, 8.0f, 22, "MapleTree_Bark.jpg"),
            HeroSpec("MapleTree_4", "MapleTree_Leaves.png", 5, 7.2f, 57, "MapleTree_Bark.jpg"),
            HeroSpec("NormalTree_1", "NormalTree_Leaves.png", 6, 8.5f, 25, "NormalTree_Bark.jpg"),
            HeroSpec("NormalTree_5", "NormalTree_Leaves.png", 5, 9.0f, 58, "NormalTree_Bark.jpg")
        )
        PlantStyle.ALPINE -> listOf(
            HeroSpec("PineTree_1", "PineTree_Leaves.png", 10, 7.5f, 3, "PineTree_Bark.jpg"),
            HeroSpec("PineTree_2", "PineTree_Leaves.png", 8, 6.8f, 23, "PineTree_Bark.jpg"),
            HeroSpec("PineTree_3", "PineTree_Leaves.png", 7, 8.2f, 26, "PineTree_Bark.jpg"),
            HeroSpec("PineTree_5", "PineTree_Leaves.png", 6, 9.4f, 59, "PineTree_Bark.jpg")
        )
        PlantStyle.COAST -> listOf(
            HeroSpec("PalmTree_1", "PalmTree_Leaves.png", 6, 8.5f, 4, "PalmTree_Trunk.jpg"),
            HeroSpec("PalmTree_2", "PalmTree_Leaves.png", 6, 7.5f, 24, "PalmTree_Trunk.jpg"),
            HeroSpec("PalmTree_3", "PalmTree_Leaves.png", 5, 9.0f, 33, "PalmTree_Trunk.jpg"),
            HeroSpec("PalmTree_4", "PalmTree_Leaves.png", 5, 7.0f, 34, "PalmTree_Trunk.jpg"),
            HeroSpec("PalmTree_5", "PalmTree_Leaves.png", 4, 8.0f, 60, "PalmTree_Trunk.jpg")
        )
        PlantStyle.DESERT -> emptyList()
    }

    private fun heroPlantSpecs(): List<HeroSpec> {
        val plants = ArrayList<HeroSpec>(12)
        when (plantStyle) {
            PlantStyle.FOREST -> {
                plants.add(HeroSpec("Bush", "Bush_Leaves.png", 40, 1.5f, 11))
                plants.add(HeroSpec("Bush_Large", "Bush_Leaves.png", 24, 2.2f, 41))
                plants.add(HeroSpec("Bush_Small", "Bush_Leaves.png", 34, 0.9f, 27))
                plants.add(HeroSpec("Flower_1_Clump", "Flowers.png", 34, 0.7f, 12))
                plants.add(HeroSpec("Flower_2_Clump", "Flowers.png", 26, 0.6f, 28))
                plants.add(HeroSpec("Flower_3_Clump", "Flowers.png", 26, 0.65f, 42))
                plants.add(HeroSpec("Grass_Large", "Grass.png", 40, 1.0f, 13))
                plants.add(HeroSpec("Grass_Small", "Grass.png", 40, 0.6f, 43))
                plants.add(HeroSpec("Plant_1", "Flowers.png", 26, 0.9f, 44))
            }
            PlantStyle.ALPINE -> {
                plants.add(HeroSpec("Bush", "Bush_Leaves.png", 28, 1.2f, 14))
                plants.add(HeroSpec("Bush_Small", "Bush_Leaves.png", 24, 0.8f, 45))
                plants.add(HeroSpec("Grass_Small", "Grass.png", 30, 0.6f, 46))
                plants.add(HeroSpec("Plant_1", "Flowers.png", 18, 0.7f, 47))
            }
            PlantStyle.COAST -> {
                plants.add(HeroSpec("Bush_Large", "Bush_Leaves.png", 24, 2.0f, 16))
                plants.add(HeroSpec("Bush_Flowers", "Bush_Leaves.png", 22, 1.4f, 48))
                plants.add(HeroSpec("Grass_Large", "Grass.png", 34, 1.3f, 17))
                plants.add(HeroSpec("Grass_Small", "Grass.png", 30, 0.7f, 49))
                plants.add(HeroSpec("Plant_1", "Flowers.png", 20, 0.8f, 50))
            }
            PlantStyle.DESERT -> {
                plants.add(HeroSpec("Grass_Small", "Grass.png", 22, 0.5f, 51))
            }
        }
        plants.add(HeroSpec("Rock_1", "Rocks.jpg", 18, 2.0f, 18))
        plants.add(HeroSpec("Rock_2", "Rocks.jpg", 12, 1.6f, 29))
        plants.add(HeroSpec("Rock_3", "Rocks.jpg", 10, 2.4f, 52))
        plants.add(HeroSpec("Rock_4", "Rocks.jpg", 10, 1.9f, 53))
        return plants
    }

    private fun buildHeroProps(
        assets: AssetManager,
        textures: TextureFactory,
        outTrees: MutableList<TreePart>,
        outPlants: MutableList<InstancedPart>
    ) {
        val fallbackTrunkMaterial = Material(
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
            val barkTexture =
                if (spec.bark.isEmpty()) null
                else AssetTextures.load(assets, "quaternius/${spec.bark}")
            val trunkMaterial = if (barkTexture == null) {
                fallbackTrunkMaterial
            } else {
                Material(
                    albedo = barkTexture,
                    tint = Vec3(1f, 1f, 1f),
                    roughness = 0.92f,
                    metallic = 0f,
                    uvScale = 1f
                )
            }

            val set = heroScatter(heroCount(spec.count), spec.seedOffset, spec.height)
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

            val set = heroScatter(heroCount(spec.count), spec.seedOffset, spec.height)
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
            val deadBark = AssetTextures.load(assets, "quaternius/NormalTree_Bark.jpg")
            val deadMaterial = if (deadBark == null) {
                fallbackTrunkMaterial
            } else {
                Material(
                    albedo = deadBark,
                    tint = Vec3(0.92f, 0.88f, 0.82f),
                    roughness = 0.94f,
                    metallic = 0f,
                    uvScale = 1f
                )
            }
            for (spec in listOf(
                HeroSpec("DeadTree_1", "", 9, 5.5f, 31),
                HeroSpec("DeadTree_2", "", 7, 6.5f, 32),
                HeroSpec("DeadTree_3", "", 5, 5.0f, 37),
                HeroSpec("DeadTree_5", "", 6, 4.5f, 35),
                HeroSpec("DeadTree_9", "", 6, 4.0f, 36)
            )) {
                val model = ObjLoader.load(assets, "quaternius/${spec.file}.obj") ?: continue
                val data = model.bark ?: model.foliage ?: continue
                val set = heroScatter(heroCount(spec.count), spec.seedOffset, spec.height)
                if (set.count == 0) continue
                val mesh = Mesh("heroProp${spec.file}", data)
                val instanced = InstancedMesh("heroProps${spec.file}", data, set.count)
                instanced.setInstances(set.matrices, set.count)
                outPlants.add(InstancedPart(mesh, instanced, deadMaterial))
            }
        }

        // Whatever the Quaternius packs do not cover — desert cacti, coastal palms, meadow
        // grass — comes out of the Kenney Nature Kit.
        buildKenneyPlants(assets, textures, outPlants)
    }

    /** One Kenney Nature Kit model: exact file name, world height and how many per realm. */
    private class KenneySpec(
        val name: String,
        val height: Float,
        val count: Int,
        val seedOffset: Int
    )

    /** Which exact Kenney models this realm grows, on top of the Quaternius set. */
    private fun kenneySpecs(): List<KenneySpec> = when (plantStyle) {
        PlantStyle.DESERT -> listOf(
            KenneySpec("cactus_short", 1.9f, 12, 101),
            KenneySpec("cactus_tall", 3.6f, 8, 102),
            KenneySpec("rock_largeA", 1.2f, 10, 103)
        )
        PlantStyle.COAST -> listOf(
            KenneySpec("tree_palm", 6.5f, 6, 111),
            KenneySpec("plant_bushLarge", 1.6f, 14, 112),
            KenneySpec("grass_large", 0.9f, 18, 113)
        )
        PlantStyle.ALPINE -> listOf(
            KenneySpec("rock_largeA", 1.4f, 14, 121),
            KenneySpec("plant_bushSmall", 0.8f, 12, 122),
            KenneySpec("grass_large", 0.7f, 12, 123)
        )
        PlantStyle.FOREST -> listOf(
            KenneySpec("plant_bush", 1.2f, 14, 131),
            KenneySpec("plant_bushLarge", 1.7f, 10, 132),
            KenneySpec("grass_large", 0.9f, 16, 133)
        )
    }

    /**
     * The Kenney Nature Kit ships no texture: every model is a handful of flat material
     * colours in its MTL. [AnimalLoader] splits the OBJ by material and returns one mesh and
     * one colour per group, which is exactly the shape an [InstancedPart] needs, so each part
     * is drawn with the shared white texture tinted by its own colour.
     */
    private fun buildKenneyPlants(
        assets: AssetManager,
        textures: TextureFactory,
        out: MutableList<InstancedPart>
    ) {
        for (spec in kenneySpecs()) {
            val parts = AnimalLoader.load(
                assets,
                "models/nature/${spec.name}.obj",
                "models/nature/${spec.name}.mtl",
                1f
            )
            if (parts.isEmpty()) continue

            val set = heroScatter(heroCount(spec.count), spec.seedOffset, spec.height)
            if (set.count == 0) continue

            for ((index, part) in parts.withIndex()) {
                val mesh = Mesh("kenney${spec.name}$index", part.data)
                val instanced = InstancedMesh("kenney${spec.name}$index", part.data, set.count)
                instanced.setInstances(set.matrices, set.count)
                val material = Material(
                    albedo = textures.white,
                    tint = part.color,
                    roughness = 0.90f,
                    metallic = 0f,
                    uvScale = 1f
                )
                out.add(InstancedPart(mesh, instanced, material))
            }
        }
    }

    // ---- hand-authored buildings and mountains ----------------------------

    private class StructureSpec(
        val asset: String,
        val height: Float,
        val spots: List<Triple<Float, Float, Float>>
    )

    private fun structureSpecs(): List<StructureSpec> {
        val specs = ArrayList<StructureSpec>()
        when (definition.id) {
            WorldId.FOREST -> {
                specs.add(StructureSpec("Houses_FirstAge_1_Level2", 9f, listOf(
                    Triple(-44f, -70f, 0.5f),
                    Triple(48f, -104f, -0.7f),
                    Triple(24f, -46f, 1.2f),
                    Triple(-30f, -56f, -0.3f)
                )))
                specs.add(StructureSpec("Windmill_FirstAge", 15f, listOf(
                    Triple(78f, -140f, 0.2f),
                    Triple(-58f, -96f, 0.9f)
                )))
                specs.add(StructureSpec("WatchTower_FirstAge_Level2", 16f, listOf(
                    Triple(-88f, -175f, 0f),
                    Triple(34f, -80f, 0.4f)
                )))
                specs.add(StructureSpec("Mountain_Single", 95f, listOf(
                    Triple(-320f, -520f, 0.6f)
                )))
                specs.add(StructureSpec("MountainLarge_Single", 140f, listOf(
                    Triple(360f, -640f, 2.1f)
                )))
            }
            WorldId.MOUNTAIN -> {
                specs.add(StructureSpec("TownCenter_FirstAge_Level1", 13f, listOf(
                    Triple(52f, -150f, 0.0f)
                )))
                specs.add(StructureSpec("TowerHouse_FirstAge", 18f, listOf(
                    Triple(-64f, -190f, 0.9f),
                    Triple(30f, -66f, -0.6f),
                    Triple(-26f, -84f, 1.4f)
                )))
                specs.add(StructureSpec("Houses_FirstAge_1_Level2", 9f, listOf(
                    Triple(18f, -44f, 0.2f),
                    Triple(-48f, -60f, 1.8f)
                )))
                specs.add(StructureSpec("MountainLarge_Single", 160f, listOf(
                    Triple(-380f, -560f, 0.3f),
                    Triple(420f, -720f, 2.4f)
                )))
                specs.add(StructureSpec("Mountain_Single", 100f, listOf(
                    Triple(220f, -430f, 1.5f)
                )))
            }
            WorldId.SNOW -> {
                specs.add(StructureSpec("TownCenter_FirstAge_Level1", 12f, listOf(
                    Triple(-46f, -140f, 0.8f)
                )))
                specs.add(StructureSpec("TowerHouse_FirstAge", 17f, listOf(
                    Triple(68f, -185f, -0.5f),
                    Triple(22f, -58f, 0.6f),
                    Triple(-34f, -76f, -1.2f)
                )))
                specs.add(StructureSpec("Houses_FirstAge_1_Level2", 9f, listOf(
                    Triple(-18f, -42f, 1.1f)
                )))
                specs.add(StructureSpec("MountainLarge_Single", 150f, listOf(
                    Triple(-300f, -600f, 1.1f)
                )))
            }
            WorldId.OCEAN -> {
                specs.add(StructureSpec("Dock_FirstAge", 4f, listOf(
                    Triple(-34f, -78f, 1.57f),
                    Triple(30f, -64f, 1.57f)
                )))
                specs.add(StructureSpec("Houses_FirstAge_1_Level2", 8f, listOf(
                    Triple(58f, -118f, 0.4f),
                    Triple(-24f, -52f, 2.4f)
                )))
                specs.add(StructureSpec("WatchTower_FirstAge_Level2", 15f, listOf(
                    Triple(-130f, -240f, 0.0f),
                    Triple(44f, -88f, 0.2f)
                )))
            }
            WorldId.DESERT -> {
                specs.add(StructureSpec("Mine", 11f, listOf(
                    Triple(-70f, -145f, 0.7f),
                    Triple(26f, -62f, -0.4f)
                )))
                specs.add(StructureSpec("TowerHouse_FirstAge", 15f, listOf(
                    Triple(88f, -200f, -1.1f),
                    Triple(-30f, -74f, 0.8f)
                )))
                specs.add(StructureSpec("Houses_FirstAge_1_Level2", 8f, listOf(
                    Triple(16f, -40f, 1.6f)
                )))
            }
        }
        // Crates and barrels dress every realm's launch area.
        specs.add(StructureSpec("Crate", 1.2f, listOf(
            Triple(11f, -26f, 0.3f),
            Triple(13.5f, -28.5f, 1.1f)
        )))
        specs.add(StructureSpec("Barrel", 1.4f, listOf(
            Triple(-12.5f, -24f, 0f)
        )))
        return specs
    }

    private fun buildStructures(
        assets: AssetManager,
        textures: TextureFactory
    ): List<InstancedPart> {
        val out = ArrayList<InstancedPart>()
        val matrix = Mat4()
        for (spec in structureSpecs()) {
            val data = PropLoader.load(assets, "buildings/${spec.asset}.obj", spec.height)
                ?: continue
            val texture =
                AssetTextures.load(assets, "buildings/${spec.asset}.png") ?: continue
            if (spec.spots.isEmpty()) continue

            val matrices = FloatArray(spec.spots.size * 16)
            for (index in spec.spots.indices) {
                val spot = spec.spots[index]
                val y = terrain.height(spot.first, spot.second) - 0.15f
                matrix.setTrs(
                    spot.first,
                    y,
                    spot.second,
                    0f, spot.third, 0f,
                    1f, 1f, 1f
                )
                System.arraycopy(matrix.m, 0, matrices, index * 16, 16)
            }

            val mesh = Mesh("structure${spec.asset}", data)
            val instanced = InstancedMesh("structures${spec.asset}", data, spec.spots.size)
            instanced.setInstances(matrices, spec.spots.size)
            val material = Material(
                albedo = texture,
                tint = Vec3(1f, 1f, 1f),
                roughness = 0.90f,
                metallic = 0f,
                uvScale = 1f
            )
            out.add(InstancedPart(mesh, instanced, material))
        }
        // The itch.io aircraft kit arrives as a zip; pull out the pad-side props it holds.
        out.addAll(buildKitProps(assets, textures))
        // And the Kenney Castle Kit raises a ruin on every realm's skyline.
        out.addAll(buildCastle(assets))
        return out
    }

    /**
     * Windsock and traffic cone straight from the kit archive. Both are optional: if the
     * archive layout differs, nothing is added and the pad keeps its crates and barrel.
     */
    private fun buildKitProps(
        assets: AssetManager,
        textures: TextureFactory
    ): List<InstancedPart> {
        val entries = ZipKit.read(assets, "kits/aircraft-kit.zip")
        if (entries.isEmpty()) return emptyList()

        val out = ArrayList<InstancedPart>(2)
        val matrix = Mat4()
        val specs = listOf(
            Triple("windsock", 3.5f, Triple(-9.5f, -21f, 0f)),
            Triple("cone", 0.7f, Triple(7.5f, -24f, 0f))
        )
        for (spec in specs) {
            val obj = ZipKit.find(entries, ".obj", spec.first) ?: continue
            val data = PropLoader.loadFromText(
                String(obj.bytes, Charsets.ISO_8859_1),
                obj.name,
                spec.second
            ) ?: continue
            val png = ZipKit.textureFor(entries, obj.name) ?: continue
            val texture = AssetTextures.loadFromBytes(png.bytes, png.name) ?: continue

            val spot = spec.third
            val y = terrain.height(spot.first, spot.second) - 0.05f
            matrix.setTrs(spot.first, y, spot.second, 0f, spot.third, 0f, 1f, 1f, 1f)
            val matrices = FloatArray(16)
            System.arraycopy(matrix.m, 0, matrices, 0, 16)

            val mesh = Mesh("kit${spec.first}", data)
            val instanced = InstancedMesh("kit${spec.first}", data, 1)
            instanced.setInstances(matrices, 1)
            val material = Material(
                albedo = texture,
                tint = Vec3(1f, 1f, 1f),
                roughness = 0.88f,
                metallic = 0f,
                uvScale = 1f
            )
            out.add(InstancedPart(mesh, instanced, material))
        }
        return out
    }
    // ---- Kenney Castle Kit -------------------------------------------------

    /** Where a castle ruin stands: one tower, and a wall run heading off along +X. */
    private class CastleSite(
        val x: Float,
        val z: Float,
        val heading: Float,
        val wallModules: Int
    )

    private fun castleSites(): List<CastleSite> = when (definition.id) {
        WorldId.FOREST -> listOf(CastleSite(-96f, -156f, 0.35f, 4))
        WorldId.MOUNTAIN -> listOf(CastleSite(104f, -186f, -0.45f, 3))
        WorldId.SNOW -> listOf(CastleSite(-88f, -172f, 0.60f, 3))
        WorldId.OCEAN -> listOf(CastleSite(118f, -224f, 1.20f, 2))
        WorldId.DESERT -> listOf(CastleSite(-108f, -204f, -0.80f, 2))
    }

    /**
     * A ruined tower and its wall, built from the Kenney Castle Kit. The kit is authored on a
     * single 1-unit grid, so every piece is loaded at one shared scale, chosen so the finished
     * tower stands [TOWER_HEIGHT] metres tall; the walls then keep the artist's proportions.
     */
    private fun buildCastle(assets: AssetManager): List<InstancedPart> {
        val towerPieces = listOf("tower-square-base", "tower-square-mid", "tower-square-roof")
        val wallPieces = listOf("wall", "wall-doorway")

        val measured = LinkedHashMap<String, PropLoader.Extents>()
        for (name in towerPieces + wallPieces) {
            PropLoader.extents(assets, "models/castle/$name.obj")?.let { measured[name] = it }
        }
        if (measured.isEmpty()) return emptyList()

        val towerUnits = towerPieces.sumOf { (measured[it]?.height ?: 0f).toDouble() }.toFloat()
        if (towerUnits <= 0f) return emptyList()
        val scale = TOWER_HEIGHT / towerUnits

        val meshes = LinkedHashMap<String, MeshData>()
        for (name in measured.keys) {
            PropLoader.loadScaled(assets, "models/castle/$name.obj", scale)?.let {
                meshes[name] = it
            }
        }
        if (meshes.isEmpty()) return emptyList()

        val texture = AssetTextures.load(assets, "models/castle/colormap.png")
            ?: return emptyList()

        val placements = LinkedHashMap<String, ArrayList<Float>>()
        val slot = Mat4()

        fun place(name: String, x: Float, y: Float, z: Float, heading: Float) {
            if (name !in meshes) return
            slot.setTrs(x, y, z, 0f, heading, 0f, 1f)
            val list = placements.getOrPut(name) { ArrayList(48) }
            for (value in slot.m) list.add(value)
        }

        val towerHalf = (measured["tower-square-base"]?.size ?: 0f) * scale * 0.5f
        val wallWidth = (measured["wall"]?.size ?: 0f) * scale

        for (site in castleSites()) {
            if (terrain.height(site.x, site.z) < definition.waterLevel + 1f) continue

            var lift = terrain.height(site.x, site.z) - 0.25f
            for (name in towerPieces) {
                if (name !in meshes) continue
                place(name, site.x, lift, site.z, site.heading)
                lift += (measured[name]?.height ?: 0f) * scale
            }

            if (wallWidth <= 0f) continue
            for (index in 0 until site.wallModules) {
                val name = if (index == 0) "wall-doorway" else "wall"
                if (name !in meshes) continue
                val x = site.x + towerHalf + wallWidth * (index + 0.5f)
                place(name, x, terrain.height(x, site.z) - 0.25f, site.z, site.heading)
            }
        }

        val material = Material(
            albedo = texture,
            tint = Vec3(1f, 1f, 1f),
            roughness = 0.92f,
            metallic = 0f,
            uvScale = 1f
        )

        val out = ArrayList<InstancedPart>(placements.size)
        for ((name, values) in placements) {
            val data = meshes[name] ?: continue
            val count = values.size / 16
            if (count == 0) continue

            val matrices = FloatArray(count * 16)
            for (index in matrices.indices) matrices[index] = values[index]

            val mesh = Mesh("castle$name", data)
            val instanced = InstancedMesh("castle$name", data, count)
            instanced.setInstances(matrices, count)
            out.add(InstancedPart(mesh, instanced, material))
        }
        return out
    }

    /**
     * One piece of a Kenney modular kit placed in the world: which kit and model it comes
     * from, the height it should stand, and where it goes.
     */
    private class KitSpec(
        val kit: String,
        val model: String,
        val height: Float,
        val x: Float,
        val z: Float,
        val heading: Float
    )

    /**
     * Dresses the launch area and the first part of the flight with the industrial and town
     * kits: airfield buildings around the hangar, a few town props per realm, and castle
     * gates, flags and boulders standing in the flight corridor as obstacles.
     *
     * Every piece is normalised to its own height by [PropLoader.load], so a standalone prop
     * keeps its proportions without the modular grid the castle tower needs.
     */
    private fun buildKitDressing(assets: AssetManager): List<InstancedPart> {
        val textures = mapOf(
            "industrial" to "models/industrial/colormap.png",
            "town" to "models/town/colormap.png",
            "castle" to "models/castle/colormap.png"
        ).mapValues { (_, path) -> AssetTextures.load(assets, path) }

        val meshes = LinkedHashMap<String, MeshData>()
        val groups = LinkedHashMap<String, ArrayList<Float>>()
        val matrix = Mat4()

        for (spec in kitDressing()) {
            // The height is part of the key: the same model is placed at two sizes in a few
            // places, and each size needs its own mesh — the loader normalises to the height.
            val key = "${spec.kit}/${spec.model}@${spec.height}"
            val data = meshes.getOrPut(key) {
                PropLoader.load(
                    assets,
                    "models/${spec.kit}/${spec.model}.obj",
                    spec.height
                ) ?: return@getOrPut MeshData(FloatArray(0), IntArray(0))
            }
            if (data.vertices.isEmpty()) continue

            val ground = terrain.height(spec.x, spec.z)
            if (ground < definition.waterLevel + 0.5f) continue

            matrix.setTrs(spec.x, ground - 0.12f, spec.z, 0f, spec.heading, 0f, 1f)
            groups.getOrPut(key) { ArrayList(8) }.addAll(matrix.m.toList())
        }

        val out = ArrayList<InstancedPart>(groups.size)
        for ((key, values) in groups) {
            val data = meshes[key] ?: continue
            if (data.vertices.isEmpty()) continue
            val kit = key.substringBefore('/')
            val texture = textures[kit] ?: continue
            val count = values.size / 16
            if (count == 0) continue

            val matrices = FloatArray(count * 16)
            for (index in matrices.indices) matrices[index] = values[index]

            val name = "kit" + key.replace('/', '_')
            val mesh = Mesh(name, data)
            val instanced = InstancedMesh(name, data, count)
            instanced.setInstances(matrices, count)
            out.add(
                InstancedPart(
                    mesh,
                    instanced,
                    Material(
                        albedo = texture,
                        tint = Vec3(1f, 1f, 1f),
                        roughness = 0.9f,
                        metallic = 0f,
                        uvScale = 1f
                    )
                )
            )
        }
        return out
    }

    /**
     * One run of modular fencing along the ground: from ([startX], [startZ]) to
     * ([endX], [endZ]). Runs sit off to the sides of the corridor, so they mark a field
     * boundary and frame the launch without standing in the flight line.
     */
    private class FenceRun(
        val startX: Float,
        val startZ: Float,
        val endX: Float,
        val endZ: Float,
        val maxSections: Int
    )

    /** The field boundaries each realm gets, if any. */
    private fun fenceRuns(): List<FenceRun> = when (definition.id) {
        WorldId.FOREST -> listOf(
            FenceRun(-46f, -28f, -20f, -30f, 10),
            FenceRun(18f, -66f, 42f, -68f, 10)
        )
        WorldId.MOUNTAIN -> listOf(
            FenceRun(-48f, -36f, -18f, -38f, 11),
            FenceRun(16f, -54f, 44f, -56f, 11)
        )
        WorldId.DESERT -> listOf(
            FenceRun(-34f, -46f, -14f, -48f, 8)
        )
        else -> emptyList()
    }

    /**
     * Field boundaries from the Kenney Nature Kit: modular sections laid end to end at a
     * constant height, each one dropped onto the ground under its own posts. The step comes
     * from the model's own width-to-height ratio, so the sections meet edge to edge instead
     * of overlapping or leaving gaps, and a section that would stand in water is skipped.
     */
    private fun buildFieldBoundaries(
        assets: AssetManager,
        textures: TextureFactory
    ): List<InstancedPart> {
        val runs = fenceRuns()
        if (runs.isEmpty()) return emptyList()

        val model = "fence_simple"
        val parts = AnimalLoader.load(
            assets,
            "models/nature/$model.obj",
            "models/nature/$model.mtl",
            FENCE_HEIGHT
        )
        if (parts.isEmpty()) return emptyList()

        val extents = PropLoader.extents(assets, "models/nature/$model.obj") ?: return emptyList()
        if (extents.height <= 0f) return emptyList()
        val section = (extents.size / extents.height) * FENCE_HEIGHT * 0.98f
        if (section <= 0f) return emptyList()

        val matrices = ArrayList<Float>(runs.size * 11 * 16)
        val matrix = Mat4()

        for (run in runs) {
            val dx = run.endX - run.startX
            val dz = run.endZ - run.startZ
            val length = kotlin.math.sqrt(dx * dx + dz * dz)
            if (length <= 0f) continue

            // The section is authored along +X, so trade that axis for the run direction.
            val heading = kotlin.math.atan2(-dz, dx)
            val stepX = dx / length
            val stepZ = dz / length
            val count = (length / section).toInt().coerceIn(0, run.maxSections)

            for (index in 0..count) {
                val distance = index * section
                if (distance > length) break
                val x = run.startX + stepX * distance
                val z = run.startZ + stepZ * distance
                val ground = terrain.height(x, z)
                if (ground < definition.waterLevel + 0.4f) continue

                matrix.setTrs(x, ground - 0.05f, z, 0f, heading, 0f, 1f)
                for (value in matrix.m) matrices.add(value)
            }
        }

        val placed = matrices.size / 16
        if (placed == 0) return emptyList()

        val instanceArray = FloatArray(placed * 16)
        for (index in instanceArray.indices) instanceArray[index] = matrices[index]

        val out = ArrayList<InstancedPart>(parts.size)
        for ((index, part) in parts.withIndex()) {
            val name = "fence$index"
            val mesh = Mesh(name, part.data)
            val instanced = InstancedMesh(name, part.data, placed)
            instanced.setInstances(instanceArray, placed)
            out.add(
                InstancedPart(
                    mesh,
                    instanced,
                    Material(
                        albedo = textures.white,
                        tint = Vec3(part.color.x, part.color.y, part.color.z),
                        roughness = 0.9f,
                        metallic = 0f,
                        uvScale = 1f
                    )
                )
            )
        }
        return out
    }

    /** Every kit piece placed per realm, grouped by the kit it comes from. */
    private fun kitDressing(): List<KitSpec> {
        val out = ArrayList<KitSpec>(24)

        // The airfield behind and beside the hangar: industrial buildings, containers and a
        // water tower on the horizon the launch runs toward.
        out += KitSpec("industrial", "building-a", 12f, -64f, -128f, 0.35f)
        out += KitSpec("industrial", "building-c", 9f, 72f, -96f, -0.6f)
        out += KitSpec("industrial", "building-h", 7f, 46f, -196f, 0.1f)
        out += KitSpec("industrial", "shipping-container-a", 2.6f, 22f, -38f, 0.35f)
        out += KitSpec("industrial", "shipping-container-a", 2.4f, 25.5f, -40f, 0.35f)
        out += KitSpec("industrial", "water-tower", 9f, -42f, -74f, 0f)
        out += KitSpec("industrial", "windmill", 14f, 92f, -152f, 0.2f)

        // Realm dressing, placed close enough to read from the launch pad.
        when (definition.id) {
            WorldId.FOREST -> {
                out += KitSpec("town", "tree-high", 6.5f, 26f, -22f, 0.8f)
                out += KitSpec("town", "fence", 1.2f, 14f, -12f, 0f)
                out += KitSpec("town", "lantern", 3.2f, 9f, -16f, 0f)
                out += KitSpec("town", "stall", 2.6f, -18f, -30f, 1.1f)
            }
            WorldId.DESERT -> {
                out += KitSpec("town", "hedge", 1.6f, -26f, -26f, 0f)
                out += KitSpec("town", "pillar-stone", 3.5f, 22f, -34f, 0f)
                out += KitSpec("town", "banner-red", 4f, 16f, -40f, 0.5f)
            }
            WorldId.SNOW -> {
                out += KitSpec("town", "tree-high", 5.5f, -30f, -24f, 0.4f)
                out += KitSpec("town", "pillar-stone", 4f, 12f, -20f, 0f)
                out += KitSpec("town", "lantern", 3.2f, -8f, -18f, 0f)
            }
            WorldId.OCEAN -> {
                out += KitSpec("town", "cart", 2.2f, 24f, -18f, 1.9f)
                out += KitSpec("town", "stall", 2.6f, 15f, -28f, 0.7f)
                out += KitSpec("town", "banner-red", 4f, -22f, -36f, -0.4f)
            }
            WorldId.MOUNTAIN -> {
                out += KitSpec("town", "fence", 1.2f, 20f, -14f, 0f)
                out += KitSpec("town", "hedge", 1.6f, -24f, -30f, 0f)
                out += KitSpec("town", "tree-high", 6f, 30f, -40f, 0.2f)
            }
        }

        // Castle obstacles standing in the flight line, so the launch has something to steer
        // around beyond the trees and the ruin on the skyline.
        out += KitSpec("castle", "gate", 6.5f, 0f, -86f, 0f)
        out += KitSpec("castle", "rocks-small", 2.2f, 14f, -102f, 0f)
        out += KitSpec("castle", "flag-wide", 5f, -12f, -96f, 0f)
        out += KitSpec("castle", "rocks-small", 1.8f, -22f, -120f, 0.9f)
        out += KitSpec("castle", "metal-gate", 5.5f, 10f, -150f, 0f)

        return out
    }

    private class ShipSpec(
        val asset: String,
        val height: Float,
        val x: Float,
        val z: Float,
        val heading: Float
    )

    private fun buildShipParts(
        assets: AssetManager,
        textures: TextureFactory
    ): List<InstancedPart> {
        val out = ArrayList<InstancedPart>()
        for (index in shipSpecs.indices) {
            val spec = shipSpecs[index]
            val parts = AnimalLoader.load(
                assets,
                "ships/${spec.asset}.obj",
                "ships/${spec.asset}.mtl",
                spec.height
            )
            for ((partIndex, part) in parts.withIndex()) {
                val mesh = Mesh("ship${spec.asset}$partIndex", part.data)
                val instanced = InstancedMesh(
                    "ships${spec.asset}$partIndex",
                    part.data,
                    shipSpecs.size
                )
                val material = Material(
                    albedo = textures.white,
                    tint = Vec3(part.color.x, part.color.y, part.color.z),
                    roughness = 0.82f,
                    metallic = 0f,
                    uvScale = 1f
                )
                out.add(InstancedPart(mesh, instanced, material))
            }
        }
        return out
    }

    /** Ships sit at the waterline and rock slowly on the swell. */
    private fun updateShips(time: Float) {
        if (shipParts.isEmpty()) return
        for (index in shipSpecs.indices) {
            val spec = shipSpecs[index]
            val bob = kotlin.math.sin(time * 0.45f + index * 1.3f) * 0.18f
            val heading = spec.heading + kotlin.math.sin(time * 0.11f + index) * 0.025f
            shipMatrix.setTrs(
                spec.x,
                definition.waterLevel + bob,
                spec.z,
                0f, heading, 0f,
                1f, 1f, 1f
            )
            System.arraycopy(shipMatrix.m, 0, shipTransforms, index * 16, 16)
        }
        for (part in shipParts) {
            part.instanced.setInstances(shipTransforms, shipSpecs.size)
        }
    }

    /**
     * One imported species: the model, the height it is scaled to, and its share of the
     * realm's animal budget. Real ground has more than one animal living on it.
     */
    private class AnimalSpec(val file: String, val height: Float, val share: Float)

    /** One species in a realm: its own grazing group and the mesh groups that draw it. */
    private class Herd(
        val spec: AnimalSpec,
        val group: Wildlife.GroundAnimals,
        val parts: List<InstancedPart>
    )

    /** The species that live in each realm, in the order they are placed. */
    private fun animalSpecs(): List<AnimalSpec> = when (definition.id) {
        WorldId.FOREST -> listOf(
            AnimalSpec("Deer", 1.35f, 0.45f),
            AnimalSpec("Fox", 0.62f, 0.30f),
            AnimalSpec("ShibaInu", 0.52f, 0.25f)
        )
        WorldId.DESERT -> listOf(
            AnimalSpec("Donkey", 1.50f, 0.60f),
            AnimalSpec("Horse_White", 1.65f, 0.40f)
        )
        WorldId.SNOW -> listOf(
            AnimalSpec("Husky", 0.78f, 0.55f),
            AnimalSpec("Wolf", 0.90f, 0.45f)
        )
        WorldId.OCEAN -> listOf(
            AnimalSpec("Cow", 1.45f, 0.55f),
            AnimalSpec("Alpaca", 1.10f, 0.45f)
        )
        WorldId.MOUNTAIN -> listOf(
            AnimalSpec("Stag", 1.75f, 0.55f),
            AnimalSpec("Bull", 1.55f, 0.45f)
        )
    }

    private fun buildAnimalParts(
        assets: AssetManager,
        textures: TextureFactory,
        total: Int
    ): List<InstancedPart> {
        val out = ArrayList<InstancedPart>()
        val built = ArrayList<Herd>(2)
        var seedOffset = 0

        for (spec in animalSpecs()) {
            val count = (total * spec.share).toInt().coerceAtLeast(if (total > 0) 1 else 0)
            if (count > 0) {
                val parts = AnimalLoader.load(
                    assets,
                    "animals/${spec.file}.obj",
                    "animals/${spec.file}.mtl",
                    spec.height
                )
                if (parts.isNotEmpty()) {
                    val group = Wildlife.GroundAnimals(
                        count,
                        Vec3(0f, terrain.padHeight, -90f),
                        definition.seed + 331 + seedOffset
                    )
                    val meshes = ArrayList<InstancedPart>(parts.size)
                    for ((index, part) in parts.withIndex()) {
                        val name = "animal${spec.file}$index"
                        val mesh = Mesh(name, part.data)
                        val instanced = InstancedMesh(name, part.data, count)
                        val material = Material(
                            albedo = textures.white,
                            tint = Vec3(part.color.x, part.color.y, part.color.z),
                            roughness = 0.88f,
                            metallic = 0f,
                            uvScale = 1f
                        )
                        meshes.add(InstancedPart(mesh, instanced, material))
                    }
                    out.addAll(meshes)
                    built.add(Herd(spec, group, meshes))
                }
            }
            seedOffset += 97
        }

        herds = built
        return out
    }

    /** Props per species, scaled by the graphics setting rather than switched off entirely. */
    private fun heroCount(base: Int): Int {
        val factor = when {
            detailLevel <= 0 -> 0.35f
            detailLevel == 1 -> 0.7f
            detailLevel == 2 -> 1f
            else -> 1.35f
        }
        return (base * factor).toInt().coerceAtLeast(0)
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
            // 18–140 m: close enough to dress the hangar approach, far enough to line the
            // first half of every flight with trees, bushes and rocks to steer around.
            val radius = 18f + rng.nextFloat() * 122f
            val x = kotlin.math.cos(angle) * radius
            val z = -60f + kotlin.math.sin(angle) * radius
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

    private companion object {
        /** Height the imported castle tower is scaled to, in metres. */
        const val TOWER_HEIGHT = 15f

        /** Height an imported bird model is scaled to, in metres. */
        const val BIRD_HEIGHT = 0.9f

        /** Height a fence section is scaled to, in metres: about waist high on the player. */
        const val FENCE_HEIGHT = 0.9f
    }
}
