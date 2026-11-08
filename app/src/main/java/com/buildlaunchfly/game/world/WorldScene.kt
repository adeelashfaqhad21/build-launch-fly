package com.buildlaunchfly.game.world

import com.buildlaunchfly.game.gfx.Camera
import com.buildlaunchfly.game.gfx.InstancedMesh
import com.buildlaunchfly.game.gfx.Material
import com.buildlaunchfly.game.gfx.Mesh
import com.buildlaunchfly.game.gfx.MeshBuilder
import com.buildlaunchfly.game.gfx.MeshData
import com.buildlaunchfly.game.gfx.ParticleRenderer
import com.buildlaunchfly.game.gfx.SurfaceKind
import com.buildlaunchfly.game.gfx.TextureFactory
import com.buildlaunchfly.game.math.Vec3

/**
 * Everything that makes up one playable world: terrain, water, vegetation, wildlife and
 * weather. Built on the GL thread; the caller owns disposal.
 */
class WorldScene(
    val definition: WorldDefinition,
    textures: TextureFactory,
    detailLevel: Int = 2
) {

    val terrain = Terrain(definition)
    val terrainMesh: Mesh
    val waterMesh: Mesh?
    val settlementWallsMesh: Mesh
    val settlementRoofsMesh: Mesh
    val settlementDetailsMesh: Mesh

    val treeTrunkMesh: Mesh
    val treeCanopyMesh: Mesh
    val treeCanopyVariantMesh: Mesh
    val rockMesh: Mesh
    val grassMesh: Mesh?

    val treeMaterial: Material
    val canopyMaterial: Material
    val rockMaterial: Material
    val grassMaterial: Material
    val bushMaterial: Material
    val birdMaterial: Material
    val animalMaterial: Material
    val settlementWallMaterial: Material
    val settlementRoofMaterial: Material
    val settlementDetailMaterial: Material

    val treeTrunks: InstancedMesh
    val treeCanopies: InstancedMesh
    val treeCanopyVariants: InstancedMesh
    val rocks: InstancedMesh
    val grass: InstancedMesh?
    val bushes: InstancedMesh
    val bushesVariantB: InstancedMesh
    val bushesVariantC: InstancedMesh
    val birdsMesh: InstancedMesh
    val animalsMesh: InstancedMesh

    // Compact collision proxies for authored natural obstacles (trees and rocks).
    // Each record stores centre XYZ, horizontal radius and vertical half-extent.
    private var naturalObstacleProxies: FloatArray = FloatArray(0)
    // Spatial broadphase: test only nearby 64m cells instead of scanning every
    // tree, rock and house proxy on every rendered frame.
    private val obstacleBuckets: Array<MutableList<Int>?> =
        arrayOfNulls(OBSTACLE_GRID_WIDTH * OBSTACLE_GRID_DEPTH)

    val birds: Wildlife.Birds
    val animals: Wildlife.GroundAnimals
    val weather = WeatherSystem(definition)

    val waterDeep: Vec3
    val waterShallow: Vec3

    init {
        // Keep the default mobile preset lean; high and ultra progressively
        // restore habitat density for devices with more GPU headroom.
        val scale = when {
            detailLevel <= 0 -> 0.24f
            detailLevel == 1 -> 0.44f
            detailLevel == 2 -> 0.72f
            else -> 0.92f
        }

        terrainMesh = Mesh("terrain", TerrainMesh.build(terrain, detailLevel))
        waterMesh = if (definition.hasWater) Mesh("water", buildWaterData()) else null

        // Small, ecosystem-specific settlements are authored as three static meshes.
        // They add inhabited landmarks without introducing per-building draw calls.
        val settlementGeometry = SettlementMeshes.build(terrain, definition, detailLevel)
        settlementWallsMesh = Mesh("settlementWalls", settlementGeometry.walls)
        settlementRoofsMesh = Mesh("settlementRoofs", settlementGeometry.roofs)
        settlementDetailsMesh = Mesh("settlementDetails", settlementGeometry.details)
        settlementWallMaterial = Material(
            albedo = when (definition.id) {
                WorldId.FOREST, WorldId.MOUNTAIN -> textures.bark
                WorldId.DESERT, WorldId.OCEAN -> textures.surface(SurfaceKind.SAND)
                WorldId.SNOW -> textures.surface(SurfaceKind.SNOW)
            },
            tint = when (definition.id) {
                WorldId.FOREST -> Vec3(0.76f, 0.62f, 0.44f)
                WorldId.DESERT -> Vec3(0.91f, 0.70f, 0.48f)
                WorldId.SNOW -> Vec3(0.66f, 0.72f, 0.78f)
                WorldId.OCEAN -> Vec3(0.84f, 0.78f, 0.63f)
                WorldId.MOUNTAIN -> Vec3(0.67f, 0.62f, 0.54f)
            },
            roughness = 0.92f
        )
        settlementRoofMaterial = Material(
            albedo = when (definition.id) {
                WorldId.DESERT, WorldId.OCEAN -> textures.surface(SurfaceKind.ROCK)
                WorldId.SNOW -> textures.surface(SurfaceKind.SNOW)
                else -> textures.bark
            },
            tint = when (definition.id) {
                WorldId.FOREST -> Vec3(0.38f, 0.25f, 0.18f)
                WorldId.DESERT -> Vec3(0.66f, 0.40f, 0.25f)
                WorldId.SNOW -> Vec3(0.88f, 0.94f, 1.00f)
                WorldId.OCEAN -> Vec3(0.38f, 0.48f, 0.56f)
                WorldId.MOUNTAIN -> Vec3(0.34f, 0.28f, 0.22f)
            },
            roughness = 0.86f
        )
        settlementDetailMaterial = Material(
            // Doors, shutters and porch timbers should read as aged natural wood,
            // not industrial metal fittings on otherwise rural buildings.
            albedo = textures.bark,
            tint = when (definition.id) {
                WorldId.DESERT -> Vec3(0.30f, 0.19f, 0.11f)
                WorldId.SNOW -> Vec3(0.34f, 0.28f, 0.22f)
                WorldId.OCEAN -> Vec3(0.38f, 0.30f, 0.20f)
                else -> Vec3(0.28f, 0.18f, 0.12f)
            },
            roughness = 0.94f,
            metallic = 0f
        )

        val trunkData = buildTrunkData()
        val canopyData = buildCanopyData(0)
        val canopyVariantData = buildCanopyData(1)
        val rockData = buildRockData()
        val bushData = buildBushData(0)
        val bushDataB = buildBushData(1)
        val bushDataC = buildBushData(2)

        treeTrunkMesh = Mesh("treeTrunk", trunkData)
        treeCanopyMesh = Mesh("treeCanopy", canopyData)
        treeCanopyVariantMesh = Mesh("treeCanopyVariant", canopyVariantData)
        rockMesh = Mesh("rock", rockData)

        treeMaterial = Material(
            albedo = textures.bark,
            tint = when (definition.id) {
                WorldId.FOREST -> Vec3(0.76f, 0.66f, 0.52f) // damp temperate bark
                WorldId.DESERT -> Vec3(0.72f, 0.56f, 0.38f) // sun-bleached woody stems
                WorldId.SNOW -> Vec3(0.62f, 0.58f, 0.54f) // dark conifer trunks against snow
                WorldId.OCEAN -> Vec3(0.70f, 0.62f, 0.48f) // salt-weathered coastal wood
                WorldId.MOUNTAIN -> Vec3(0.68f, 0.58f, 0.44f) // alpine bark
            },
            roughness = 0.92f,
            metallic = 0f,
            uvScale = 2.6f
        )
        canopyMaterial = Material(
            albedo = textures.leaf,
            tint = when (definition.id) {
                WorldId.FOREST -> Vec3(0.82f, 1.00f, 0.66f) // layered temperate foliage
                WorldId.SNOW -> Vec3(0.48f, 0.66f, 0.58f) // dark evergreen needles
                WorldId.DESERT -> Vec3(0.70f, 0.82f, 0.48f) // muted drought-tolerant greens
                WorldId.OCEAN -> Vec3(0.72f, 0.92f, 0.66f) // wind-shaped coastal canopy
                WorldId.MOUNTAIN -> Vec3(0.66f, 0.86f, 0.58f) // highland conifer and meadow edge
            },
            roughness = 0.88f,
            metallic = 0f,
            uvScale = 2.8f
        )
        rockMaterial = Material(
            albedo = textures.surface(if (definition.id == WorldId.SNOW) definition.surfaceC else definition.surfaceB),
            tint = if (definition.id == WorldId.SNOW) definition.tintC else definition.tintB,
            roughness = if (definition.id == WorldId.SNOW) 0.28f else 0.95f,
            metallic = if (definition.id == WorldId.SNOW) 0.08f else 0f,
            uvScale = 1.6f
        )
        grassMaterial = Material(
            albedo = textures.leaf,
            tint = when (definition.id) {
                WorldId.DESERT -> Vec3(0.88f, 0.76f, 0.48f)
                WorldId.OCEAN -> Vec3(0.72f, 0.94f, 0.68f)
                WorldId.MOUNTAIN -> Vec3(0.78f, 1.02f, 0.70f)
                WorldId.SNOW -> Vec3(0.76f, 0.88f, 0.82f)
                WorldId.FOREST -> Vec3(0.64f, 0.94f, 0.48f)
            },
            roughness = 0.90f,
            metallic = 0f,
            uvScale = 1f
        )
        bushMaterial = Material(
            albedo = textures.leaf,
            tint = when (definition.id) {
                WorldId.DESERT -> Vec3(0.76f, 0.68f, 0.42f)
                WorldId.SNOW -> Vec3(0.64f, 0.76f, 0.70f)
                WorldId.OCEAN -> Vec3(0.76f, 0.94f, 0.72f)
                else -> Vec3(0.78f, 0.98f, 0.66f)
            },
            roughness = 0.92f,
            metallic = 0f,
            uvScale = 1.8f
        )
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

        val treeCount = (definition.treeCount * scale).toInt()
        val treeSet = Vegetation.scatterTrees(terrain, definition, treeCount, definition.seed)
        val treeCapacity = treeSet.count.coerceAtLeast(1)
        // Keep species in natural stands rather than alternating tree types one-by-one.
        // A low-frequency field creates broad groves and transition edges across each habitat.
        val primaryTreeMatrices = FloatArray(treeSet.count * 16)
        val variantTreeMatrices = FloatArray(treeSet.count * 16)
        var primaryTreeCount = 0
        var variantTreeCount = 0
        for (index in 0 until treeSet.count) {
            val source = index * 16
            val x = treeSet.matrices[source + 12]
            val z = treeSet.matrices[source + 14]
            val speciesField = com.buildlaunchfly.game.gfx.Noise.fbm01(
                x * 0.0018f, z * 0.0018f, 2, definition.seed + 401
            )
            val variantShare = when (definition.id) {
                WorldId.FOREST -> 0.49f // birch groves break up broadleaf woodland
                WorldId.DESERT -> 0.38f // scattered multi-arm cactus stands
                WorldId.SNOW -> 0.43f // compact spruce pockets below the tree line
                WorldId.OCEAN -> 0.48f // wind-shaped coastal tree clusters
                WorldId.MOUNTAIN -> 0.52f // fir stands among open alpine meadows
            }
            if (speciesField > 0.50f - (variantShare - 0.5f)) {
                System.arraycopy(treeSet.matrices, source, variantTreeMatrices, variantTreeCount * 16, 16)
                variantTreeCount++
            } else {
                System.arraycopy(treeSet.matrices, source, primaryTreeMatrices, primaryTreeCount * 16, 16)
                primaryTreeCount++
            }
        }
        treeTrunks = InstancedMesh("treeTrunks", trunkData, treeCapacity)
        treeCanopies = InstancedMesh("treeCanopies", canopyData, primaryTreeCount.coerceAtLeast(1))
        treeCanopyVariants = InstancedMesh("treeCanopyVariants", canopyVariantData, variantTreeCount.coerceAtLeast(1))
        if (treeSet.count > 0) treeTrunks.setInstances(treeSet.matrices, treeSet.count)
        if (primaryTreeCount > 0) treeCanopies.setInstances(primaryTreeMatrices, primaryTreeCount)
        if (variantTreeCount > 0) treeCanopyVariants.setInstances(variantTreeMatrices, variantTreeCount)

        val rockCount = (definition.rockCount * scale).toInt()
        val bushBaseCount = when (definition.id) {
            WorldId.FOREST -> 620
            WorldId.DESERT -> 110
            WorldId.SNOW -> 90
            WorldId.OCEAN -> 180
            WorldId.MOUNTAIN -> 440
        }
        val bushSet = Vegetation.scatterBushes(terrain, definition, (bushBaseCount * scale).toInt(), definition.seed)
        // Species are assigned by a low-frequency habitat field, not instance order.
        // Full-size buffers allow uneven natural stands without risking an array overflow.
        val bushMatricesA = FloatArray(bushSet.count * 16)
        val bushMatricesB = FloatArray(bushSet.count * 16)
        val bushMatricesC = FloatArray(bushSet.count * 16)
        var bushCountA = 0
        var bushCountB = 0
        var bushCountC = 0
        for (index in 0 until bushSet.count) {
            val source = index * 16
            val x = bushSet.matrices[source + 12]
            val z = bushSet.matrices[source + 14]
            val speciesField = com.buildlaunchfly.game.gfx.Noise.fbm01(
                x * 0.0038f, z * 0.0038f, 2, definition.seed + 613
            )
            when {
                speciesField < 0.34f -> {
                    System.arraycopy(bushSet.matrices, source, bushMatricesA, bushCountA * 16, 16)
                    bushCountA++
                }
                speciesField < 0.68f -> {
                    System.arraycopy(bushSet.matrices, source, bushMatricesB, bushCountB * 16, 16)
                    bushCountB++
                }
                else -> {
                    System.arraycopy(bushSet.matrices, source, bushMatricesC, bushCountC * 16, 16)
                    bushCountC++
                }
            }
        }
        bushes = InstancedMesh("bushesA", bushData, bushCountA.coerceAtLeast(1))
        bushesVariantB = InstancedMesh("bushesB", bushDataB, bushCountB.coerceAtLeast(1))
        bushesVariantC = InstancedMesh("bushesC", bushDataC, bushCountC.coerceAtLeast(1))
        if (bushCountA > 0) bushes.setInstances(bushMatricesA, bushCountA)
        if (bushCountB > 0) bushesVariantB.setInstances(bushMatricesB, bushCountB)
        if (bushCountC > 0) bushesVariantC.setInstances(bushMatricesC, bushCountC)
        val rockSet = Vegetation.scatterRocks(terrain, definition, rockCount, definition.seed)
        rocks = InstancedMesh("rocks", rockData, rockSet.count.coerceAtLeast(1))
        if (rockSet.count > 0) rocks.setInstances(rockSet.matrices, rockSet.count)
        naturalObstacleProxies = buildNaturalObstacleProxies(
            treeSet.matrices, treeSet.count, rockSet.matrices, rockSet.count,
            settlementGeometry.obstacles
        )
        buildObstacleBuckets()

        val grassCount = (definition.grassCount * scale).toInt()
        if (grassCount > 0) {
            val grassData = buildGrassData()
            grassMesh = Mesh("grass", grassData)
            val grassSet = Vegetation.scatterGrass(
                terrain, definition, grassCount, definition.seed
            )
            grass = InstancedMesh("grass", grassData, grassSet.count.coerceAtLeast(1))
            if (grassSet.count > 0) grass.setInstances(grassSet.matrices, grassSet.count)
        } else {
            grassMesh = null
            grass = null
        }

        val birdCount = (definition.birdCount * scale).toInt().coerceAtLeast(0)
        val animalCount = (definition.animalCount * scale).toInt().coerceAtLeast(0)

        birds = Wildlife.Birds(
            birdCount,
            terrain,
            definition
        )
        animals = Wildlife.GroundAnimals(
            animalCount,
            terrain,
            definition
        )

        birdsMesh = InstancedMesh("birds", buildBirdData(), birdCount.coerceAtLeast(1))
        animalsMesh = InstancedMesh("animals", buildAnimalData(), animalCount.coerceAtLeast(1))

        waterDeep = definition.waterDeep
        waterShallow = definition.waterShallow
    }

    /**
     * Tests the aircraft against the same deterministic tree and rock instances used
     * by the renderer. This makes the natural landscape itself the obstacle course.
     */
    fun collidesWithNaturalObstacle(x: Float, y: Float, z: Float, aircraftRadius: Float = 1.25f): Boolean {
        val cellX = ((x - OBSTACLE_GRID_MIN_X) / OBSTACLE_CELL_SIZE).toInt()
        val cellZ = ((z - OBSTACLE_GRID_MIN_Z) / OBSTACLE_CELL_SIZE).toInt()
        for (dz in -1..1) {
            val gz = cellZ + dz
            if (gz !in 0 until OBSTACLE_GRID_DEPTH) continue
            for (dx in -1..1) {
                val gx = cellX + dx
                if (gx !in 0 until OBSTACLE_GRID_WIDTH) continue
                val bucket = obstacleBuckets[gz * OBSTACLE_GRID_WIDTH + gx] ?: continue
                for (entry in bucket) {
                    val dxWorld = x - naturalObstacleProxies[entry]
                    val dyWorld = y - naturalObstacleProxies[entry + 1]
                    val dzWorld = z - naturalObstacleProxies[entry + 2]
                    val horizontal = naturalObstacleProxies[entry + 3] + aircraftRadius
                    val vertical = naturalObstacleProxies[entry + 4] + 0.9f
                    if (kotlin.math.abs(dyWorld) < vertical &&
                        dxWorld * dxWorld + dzWorld * dzWorld < horizontal * horizontal
                    ) return true
                }
            }
        }
        return false
    }

    private fun buildObstacleBuckets() {
        var offset = 0
        while (offset < naturalObstacleProxies.size) {
            val x = naturalObstacleProxies[offset]
            val z = naturalObstacleProxies[offset + 2]
            val gx = ((x - OBSTACLE_GRID_MIN_X) / OBSTACLE_CELL_SIZE).toInt()
            val gz = ((z - OBSTACLE_GRID_MIN_Z) / OBSTACLE_CELL_SIZE).toInt()
            if (gx in 0 until OBSTACLE_GRID_WIDTH && gz in 0 until OBSTACLE_GRID_DEPTH) {
                val index = gz * OBSTACLE_GRID_WIDTH + gx
                var bucket = obstacleBuckets[index]
                if (bucket == null) {
                    bucket = ArrayList()
                    obstacleBuckets[index] = bucket
                }
                bucket.add(offset)
            }
            offset += 5
        }
    }

    private fun buildNaturalObstacleProxies(
        trees: FloatArray,
        treeCount: Int,
        rocks: FloatArray,
        rockCount: Int,
        settlements: FloatArray
    ): FloatArray {
        val result = FloatArray((treeCount + rockCount) * 5 + settlements.size)
        var out = 0
        for (index in 0 until treeCount) {
            val i = index * 16
            val sx = kotlin.math.sqrt(trees[i] * trees[i] + trees[i + 1] * trees[i + 1] + trees[i + 2] * trees[i + 2])
            val sy = kotlin.math.sqrt(trees[i + 4] * trees[i + 4] + trees[i + 5] * trees[i + 5] + trees[i + 6] * trees[i + 6])
            val sz = kotlin.math.sqrt(trees[i + 8] * trees[i + 8] + trees[i + 9] * trees[i + 9] + trees[i + 10] * trees[i + 10])
            result[out++] = trees[i + 12]
            result[out++] = trees[i + 13] + sy * 0.55f
            result[out++] = trees[i + 14]
            result[out++] = maxOf(sx, sz) * 0.72f + 0.75f
            result[out++] = sy * 0.60f
        }
        for (index in 0 until rockCount) {
            val i = index * 16
            val sx = kotlin.math.sqrt(rocks[i] * rocks[i] + rocks[i + 1] * rocks[i + 1] + rocks[i + 2] * rocks[i + 2])
            val sy = kotlin.math.sqrt(rocks[i + 4] * rocks[i + 4] + rocks[i + 5] * rocks[i + 5] + rocks[i + 6] * rocks[i + 6])
            val sz = kotlin.math.sqrt(rocks[i + 8] * rocks[i + 8] + rocks[i + 9] * rocks[i + 9] + rocks[i + 10] * rocks[i + 10])
            result[out++] = rocks[i + 12]
            result[out++] = rocks[i + 13]
            result[out++] = rocks[i + 14]
            result[out++] = maxOf(sx, sz) * 0.78f + 0.35f
            result[out++] = sy * 0.82f + 0.35f
        }
        if (settlements.isNotEmpty()) {
            System.arraycopy(settlements, 0, result, out, settlements.size)
            out += settlements.size
        }
        return if (out == result.size) result else result.copyOf(out)
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

    private companion object {
        const val OBSTACLE_CELL_SIZE = 64f
        const val OBSTACLE_GRID_MIN_X = -1280f
        const val OBSTACLE_GRID_MIN_Z = -3200f
        const val OBSTACLE_GRID_WIDTH = 44
        const val OBSTACLE_GRID_DEPTH = 64
    }

    fun dispose() {
        terrainMesh.dispose()
        waterMesh?.dispose()
        settlementWallsMesh.dispose()
        settlementRoofsMesh.dispose()
        settlementDetailsMesh.dispose()
        treeTrunkMesh.dispose()
        treeCanopyMesh.dispose()
        treeCanopyVariantMesh.dispose()
        rockMesh.dispose()
        grassMesh?.dispose()
        treeTrunks.dispose()
        treeCanopies.dispose()
        treeCanopyVariants.dispose()
        rocks.dispose()
        bushes.dispose()
        bushesVariantB.dispose()
        bushesVariantC.dispose()
        grass?.dispose()
        birdsMesh.dispose()
        animalsMesh.dispose()
    }

    // ---- geometry ---------------------------------------------------------

    private fun buildWaterData(): MeshData {
        val y = definition.waterLevel + 0.035f
        val builder = MeshBuilder()
        if (definition.id == WorldId.FOREST || definition.id == WorldId.MOUNTAIN) {
            val segments = 112
            val halfWidth = if (definition.id == WorldId.FOREST) 8.5f else 6f
            var previousLeft: Vec3? = null
            var previousRight: Vec3? = null
            for (i in 0..segments) {
                val z = 280f - i * 52f
                val centerX = 150f * kotlin.math.sin(z * 0.00125f) +
                    54f * kotlin.math.sin(z * 0.0028f + 0.7f)
                val left = Vec3(centerX - halfWidth, y, z)
                val right = Vec3(centerX + halfWidth, y, z)
                val pl = previousLeft
                val pr = previousRight
                if (pl != null && pr != null) builder.addQuad(pl, pr, right, left, uvScale = 4f)
                previousLeft = left
                previousRight = right
            }
        } else {
            builder.addQuad(
                Vec3(-3600f, y, 1600f),
                Vec3(5200f, y, 1600f),
                Vec3(5200f, y, -5200f),
                Vec3(-3600f, y, -5200f),
                uvScale = 1f
            )
        }
        return builder.build()
    }

    private fun buildTrunkData(): MeshData {
        val builder = MeshBuilder()
        // Unit-height trunk with its base at y = 0 so instances scale cleanly.
        builder.addCylinder(0f, 0.5f, 0f, 0.085f, 0.055f, 1f, 7, false, false, 1f)
        return builder.build()
    }

    private fun buildCanopyData(variant: Int): MeshData {
        val builder = MeshBuilder()
        when {
            definition.id == WorldId.FOREST && variant == 1 -> {
                // Slender birch-like crown: upright, separated foliage clusters rather
                // than another copy of the broad, rounded woodland crown.
                builder.addEllipsoid(0f, 1.05f, 0f, 0.30f, 0.62f, 0.29f, 6, 9)
                builder.addEllipsoid(0.12f, 0.72f, 0.08f, 0.25f, 0.36f, 0.24f, 6, 8)
                builder.addEllipsoid(-0.10f, 1.43f, -0.06f, 0.25f, 0.35f, 0.23f, 6, 8)
            }
            definition.id == WorldId.MOUNTAIN && variant == 1 -> {
                // Alpine fir: broad lower boughs and a narrow pointed crown.
                builder.addCone(0f, 0.34f, 0f, 0.58f, 0.88f, 9, 1.25f)
                builder.addCone(0f, 0.90f, 0f, 0.43f, 0.82f, 9, 1.25f)
                builder.addCone(0f, 1.40f, 0f, 0.27f, 0.70f, 8, 1.2f)
            }
            definition.id == WorldId.SNOW && variant == 1 -> {
                // Compact subarctic spruce, distinctly shorter and fuller than the primary pine.
                builder.addCone(0f, 0.28f, 0f, 0.62f, 0.72f, 8, 1.3f)
                builder.addCone(0f, 0.76f, 0f, 0.46f, 0.70f, 8, 1.3f)
                builder.addCone(0f, 1.18f, 0f, 0.28f, 0.62f, 7, 1.2f)
            }
            definition.id == WorldId.OCEAN && variant == 1 -> {
                // Salt-wind sculpted coastal crown, biased to one side.
                builder.addEllipsoid(0.18f, 0.96f, 0f, 0.58f, 0.42f, 0.36f, 7, 9)
                builder.addEllipsoid(-0.18f, 0.72f, 0.08f, 0.34f, 0.30f, 0.29f, 6, 8)
                builder.addEllipsoid(0.48f, 1.20f, -0.08f, 0.30f, 0.27f, 0.28f, 6, 8)
            }
            definition.id == WorldId.DESERT && variant == 1 -> {
                // Multi-arm desert cactus rather than a second straight saguaro stamp.
                builder.addCylinder(0f, 0.78f, 0f, 0.13f, 0.12f, 1.55f, 8, false, false, 1f)
                builder.pushTrs(-0.16f, 0.54f, 0f, 0f, 0f, 1.5707963f, 1f)
                builder.addCylinder(0f, 0f, 0f, 0.10f, 0.10f, 0.48f, 7, false, false, 1f)
                builder.pop()
                builder.pushTrs(0.18f, 0.88f, 0f, 0f, 0f, 1.5707963f, 1f)
                builder.addCylinder(0f, 0f, 0f, 0.09f, 0.09f, 0.42f, 7, false, false, 1f)
                builder.pop()
            }
            definition.id == WorldId.DESERT -> {
                // Saguaro silhouette: two raised arms with elbows, not a generic tree crown.
                builder.addCylinder(0f, 0.72f, 0f, 0.14f, 0.12f, 1.45f, 10, false, false, 1f)
                builder.pushTrs(-0.22f, 0.72f, 0f, 0f, 0f, 1.5707963f, 1f)
                builder.addCylinder(0f, 0f, 0f, 0.10f, 0.10f, 0.46f, 8, false, false, 1f)
                builder.pop()
                builder.addCylinder(-0.48f, 0.90f, 0f, 0.095f, 0.09f, 0.62f, 8, false, false, 1f)
                builder.pushTrs(0.24f, 0.66f, 0f, 0f, 0f, 1.5707963f, 1f)
                builder.addCylinder(0f, 0f, 0f, 0.10f, 0.10f, 0.42f, 8, false, false, 1f)
                builder.pop()
                builder.addCylinder(0.45f, 0.85f, 0f, 0.09f, 0.085f, 0.55f, 8, false, false, 1f)
            }
            definition.pineTrees -> {
                builder.addCone(0f, 0.30f, 0f, 0.52f, 0.9f, 9, 1.5f)
                builder.addCone(0f, 0.72f, 0f, 0.40f, 0.75f, 9, 1.5f)
                builder.addCone(0f, 1.08f, 0f, 0.26f, 0.60f, 8, 1.5f)
            }
            else -> {
                // Temperate broadleaf canopy: offset foliage masses form a broken,
                // asymmetric crown instead of the familiar three-ball lollipop.
                builder.addEllipsoid(0f, 1.02f, 0f, 0.43f, 0.62f, 0.42f, 7, 10)
                builder.addEllipsoid(0.30f, 0.78f, 0.10f, 0.34f, 0.40f, 0.33f, 6, 9)
                builder.addEllipsoid(-0.31f, 0.83f, -0.16f, 0.32f, 0.46f, 0.30f, 6, 9)
                builder.addEllipsoid(0.06f, 1.38f, -0.20f, 0.29f, 0.36f, 0.31f, 6, 8)
                builder.addEllipsoid(-0.12f, 0.60f, 0.22f, 0.31f, 0.28f, 0.34f, 6, 8)
                // Small outer lobes break the silhouette at mid-distance while keeping
                // the canopy connected enough to read as one mature tree.
                builder.addEllipsoid(0.52f, 0.92f, -0.18f, 0.19f, 0.28f, 0.21f, 5, 7)
                builder.addEllipsoid(-0.50f, 0.70f, 0.18f, 0.20f, 0.27f, 0.22f, 5, 7)
            }
        }
        return builder.build()
    }

    private fun buildBushData(variant: Int): MeshData {
        val builder = MeshBuilder()
        when (definition.id) {
            WorldId.DESERT -> when (variant) {
                1 -> {
                    // Upright segmented prickly-pear-like cactus with paired pads.
                    builder.addCylinder(0f, 0.26f, 0f, 0.055f, 0.05f, 0.50f, 7, false, false, 1f)
                    builder.addEllipsoid(0f, 0.55f, 0f, 0.20f, 0.28f, 0.10f, 6, 8)
                    builder.addEllipsoid(-0.22f, 0.42f, 0f, 0.12f, 0.19f, 0.08f, 6, 8)
                    builder.addEllipsoid(0.22f, 0.50f, 0f, 0.12f, 0.18f, 0.08f, 6, 8)
                }
                2 -> {
                    // Low, thorny dry scrub with thin divergent woody stems.
                    for (stem in 0 until 5) {
                        val angle = stem * 1.256637f
                        builder.pushTrs(0f, 0.08f, 0f, 0f, angle, -0.48f, 1f)
                        builder.addCone(0f, 0.28f, 0f, 0.07f, 0.62f, 5)
                        builder.pop()
                    }
                    builder.addEllipsoid(0f, 0.25f, 0f, 0.34f, 0.16f, 0.30f, 6, 8)
                }
                else -> {
                builder.addCylinder(0f, 0.30f, 0f, 0.045f, 0.035f, 0.58f, 6, false, false, 1f)
                builder.addCylinder(-0.28f, 0.30f, 0f, 0.025f, 0.025f, 0.48f, 5, false, false, 1f)
                builder.addCylinder(0.27f, 0.28f, 0.04f, 0.025f, 0.025f, 0.46f, 5, false, false, 1f)
                builder.addEllipsoid(0f, 0.40f, 0f, 0.38f, 0.22f, 0.34f, 7, 10)
                builder.addEllipsoid(-0.30f, 0.34f, 0.08f, 0.23f, 0.18f, 0.22f, 6, 8)
                builder.addEllipsoid(0.28f, 0.36f, -0.08f, 0.25f, 0.19f, 0.23f, 6, 8)
                }
            }
            WorldId.SNOW -> when (variant) {
                1 -> {
                    // Dwarf alpine conifer, low branches spreading near the snow.
                    for (layer in 0 until 3) {
                        builder.addCone(0f, 0.18f + layer * 0.20f, 0f, 0.34f - layer * 0.07f, 0.48f, 7, 1.15f)
                    }
                }
                2 -> {
                    // Cushion plant: broad, low, irregular rounded tundra mound.
                    builder.addEllipsoid(0f, 0.18f, 0f, 0.42f, 0.18f, 0.36f, 7, 9)
                    builder.addEllipsoid(-0.22f, 0.22f, 0.10f, 0.24f, 0.16f, 0.22f, 6, 8)
                    builder.addEllipsoid(0.20f, 0.20f, -0.12f, 0.25f, 0.15f, 0.23f, 6, 8)
                }
                else -> {
                builder.addCylinder(0f, 0.24f, 0f, 0.045f, 0.035f, 0.42f, 6, false, false, 1f)
                builder.addCone(0f, 0.35f, 0f, 0.42f, 0.48f, 8, 1.25f)
                builder.addCone(-0.20f, 0.28f, 0.05f, 0.30f, 0.36f, 7, 1.2f)
                builder.addCone(0.20f, 0.30f, -0.04f, 0.30f, 0.38f, 7, 1.2f)
                }
            }
            WorldId.OCEAN -> when (variant) {
                1 -> {
                    // Taller wind-combed dune grass clump.
                    for (blade in 0 until 7) {
                        builder.pushTrs(0f, 0f, 0f, 0f, blade * 0.8976f, -0.52f, 1f)
                        builder.addCone(0f, 0.48f, 0f, 0.045f, 0.92f, 5)
                        builder.pop()
                    }
                }
                2 -> {
                    // Salt-tolerant coastal succulent with fleshy leaf rosettes.
                    builder.addCylinder(0f, 0.18f, 0f, 0.045f, 0.04f, 0.35f, 6, false, false, 1f)
                    for (leaf in 0 until 5) {
                        builder.pushTrs(0f, 0.12f, 0f, 0f, leaf * 1.2566f, -0.70f, 1f)
                        builder.addEllipsoid(0f, 0.30f, 0f, 0.10f, 0.32f, 0.08f, 5, 7)
                        builder.pop()
                    }
                }
                else -> {
                builder.addCylinder(0f, 0.22f, 0f, 0.025f, 0.02f, 0.40f, 5, false, false, 1f)
                builder.addEllipsoid(0f, 0.32f, 0f, 0.14f, 0.42f, 0.14f, 5, 8)
                builder.addEllipsoid(-0.20f, 0.26f, 0.08f, 0.12f, 0.32f, 0.12f, 5, 8)
                builder.addEllipsoid(0.19f, 0.29f, -0.06f, 0.12f, 0.36f, 0.12f, 5, 8)
                }
            }
            WorldId.FOREST, WorldId.MOUNTAIN -> when (variant) {
                1 -> {
                    // Open, spreading bramble with long lateral branches and sparse foliage.
                    for (branch in 0 until 5) {
                        builder.pushTrs(0f, 0.10f, 0f, 0f, branch * 1.2566f, -0.30f, 1f)
                        builder.addCylinder(0f, 0.28f, 0f, 0.035f, 0.025f, 0.56f, 5, false, false, 1f)
                        builder.addEllipsoid(0f, 0.58f, 0f, 0.24f, 0.16f, 0.18f, 6, 8)
                        builder.pop()
                    }
                }
                2 -> {
                    // Dense, low multi-stem leafy shrub with an asymmetric crown.
                    for (stem in 0 until 4) {
                        val x = (stem - 1.5f) * 0.14f
                        builder.addCylinder(x, 0.28f, 0f, 0.035f, 0.03f, 0.54f, 5, false, false, 1f)
                        builder.addEllipsoid(x, 0.56f + (stem % 2) * 0.12f, (stem % 2) * 0.12f,
                            0.24f, 0.28f, 0.23f, 6, 8)
                    }
                }
                else -> {
                builder.addCylinder(0f, 0.34f, 0f, 0.055f, 0.045f, 0.68f, 6, false, false, 1f)
                builder.addCylinder(-0.20f, 0.46f, 0.02f, 0.035f, 0.03f, 0.48f, 5, false, false, 1f)
                builder.addCylinder(0.19f, 0.48f, -0.04f, 0.035f, 0.03f, 0.50f, 5, false, false, 1f)
                // Main woody crown and uneven leaf masses form a broad, grounded shrub silhouette.
                builder.addEllipsoid(0f, 0.55f, 0f, 0.40f, 0.34f, 0.38f, 7, 10)
                builder.addEllipsoid(-0.27f, 0.40f, 0.04f, 0.27f, 0.25f, 0.29f, 6, 9)
                builder.addEllipsoid(0.25f, 0.43f, -0.06f, 0.29f, 0.27f, 0.27f, 6, 9)
                builder.addEllipsoid(0.03f, 0.78f, -0.12f, 0.25f, 0.23f, 0.24f, 6, 9)
                // Secondary foliage lobes break the smooth ball-like outline into a natural,
                // irregular branching crown. Their low placement keeps the plant shrub-like.
                builder.addEllipsoid(-0.48f, 0.36f, -0.16f, 0.19f, 0.18f, 0.21f, 5, 7)
                builder.addEllipsoid(0.43f, 0.34f, 0.18f, 0.20f, 0.17f, 0.22f, 5, 7)
                builder.addEllipsoid(-0.18f, 0.68f, 0.30f, 0.21f, 0.19f, 0.20f, 5, 7)
                builder.addEllipsoid(0.28f, 0.67f, -0.30f, 0.18f, 0.20f, 0.19f, 5, 7)
                builder.addEllipsoid(0.02f, 0.91f, 0.10f, 0.17f, 0.16f, 0.18f, 5, 7)
                builder.addEllipsoid(-0.36f, 0.55f, 0.25f, 0.16f, 0.17f, 0.18f, 5, 7)
                builder.addEllipsoid(0.39f, 0.52f, -0.22f, 0.17f, 0.16f, 0.18f, 5, 7)
                }
            }
        }
        return builder.build()
    }

    private fun buildRockData(): MeshData {
        val builder = MeshBuilder()
        when (definition.id) {
            WorldId.DESERT -> {
                // Layered sandstone outcrop with a broader cap and eroded lower pedestal.
                builder.addCone(0f, 0.56f, 0f, 0.48f, 1.12f, 7, 1.35f)
                builder.addEllipsoid(0f, 0.92f, 0f, 0.66f, 0.20f, 0.58f, 6, 8)
                builder.addEllipsoid(0.12f, 0.30f, 0.04f, 0.42f, 0.30f, 0.40f, 6, 8)
            }
            WorldId.SNOW -> {
                // Faceted ice pinnacles and fractured glacier blocks.
                builder.addCone(0f, 0.68f, 0f, 0.40f, 1.36f, 6, 0.78f)
                builder.addCone(-0.28f, 0.42f, 0.12f, 0.24f, 0.84f, 5, 0.82f)
                builder.addEllipsoid(0.20f, 0.22f, -0.10f, 0.46f, 0.24f, 0.42f, 5, 7)
            }
            WorldId.OCEAN -> {
                // Sea-stack silhouette: narrow eroded foot and irregular rock crown.
                builder.addCone(0f, 0.62f, 0f, 0.36f, 1.24f, 7, 0.78f)
                builder.addEllipsoid(0.04f, 1.02f, 0f, 0.43f, 0.30f, 0.38f, 6, 8)
            }
            WorldId.MOUNTAIN -> {
                builder.addCone(0f, 0.58f, 0f, 0.48f, 1.16f, 6, 0.92f)
                builder.addEllipsoid(0.10f, 0.28f, 0f, 0.52f, 0.28f, 0.44f, 6, 8)
            }
            WorldId.FOREST -> {
                builder.addSphere(0f, 0.42f, 0f, 0.55f, 6, 9, 1.2f)
                builder.addEllipsoid(0.24f, 0.24f, -0.08f, 0.30f, 0.20f, 0.26f, 5, 7)
            }
        }
        return builder.build()
    }

    private fun buildGrassData(): MeshData {
        val builder = MeshBuilder()
        when (definition.id) {
            WorldId.FOREST -> {
                // Fern rosette: a central rachis with paired, tapering leaflets on each frond.
                builder.addCylinder(0f, 0.22f, 0f, 0.025f, 0.018f, 0.44f, 5, false, false)
                for (frond in 0 until 7) {
                    val yaw = frond * 0.8975979f
                    builder.pushTrs(0f, 0.12f, 0f, 0f, yaw, -0.62f + (frond % 3) * 0.08f, 1f)
                    builder.addCylinder(0f, 0.34f, 0f, 0.018f, 0.010f, 0.68f, 5, false, false)
                    for (leaflet in 0 until 4) {
                        val side = if (leaflet % 2 == 0) -1f else 1f
                        val y = 0.12f + leaflet * 0.14f
                        builder.addEllipsoid(side * (0.10f + leaflet * 0.018f), y, 0f,
                            0.16f, 0.035f, 0.065f, 4, 6)
                    }
                    builder.pop()
                }
            }
            WorldId.DESERT -> {
                // Sparse dry grass tufts with narrow, wind-leaning blades.
                for (blade in 0 until 5) {
                    val yaw = blade * 1.256637f
                    builder.pushTrs(0f, 0f, 0f, 0f, yaw, -0.30f, 1f)
                    builder.addCone(0f, 0.32f, 0f, 0.055f, 0.68f, 5)
                    builder.pop()
                }
            }
            WorldId.OCEAN -> {
                // Coastal marram grass: long arching blades in a compact dune clump.
                for (blade in 0 until 8) {
                    val yaw = blade * 0.785398f
                    builder.pushTrs(0f, 0f, 0f, 0f, yaw, -0.42f, 1f)
                    builder.addCone(0f, 0.43f, 0f, 0.045f, 0.86f, 5)
                    builder.pop()
                }
            }
            WorldId.MOUNTAIN -> {
                // Alpine bunchgrass with a small, dense crown of upright blades.
                for (blade in 0 until 9) {
                    val yaw = blade * 0.698132f
                    builder.pushTrs(0f, 0f, 0f, 0f, yaw, -0.18f, 1f)
                    builder.addCone(0f, 0.30f, 0f, 0.045f, 0.60f + (blade % 3) * 0.10f, 5)
                    builder.pop()
                }
            }
            WorldId.SNOW -> Unit
        }
        return builder.build()
    }

    private fun buildBirdData(): MeshData {
        val builder = MeshBuilder()
        builder.addEllipsoid(0f, 0f, 0f, 0.075f, 0.065f, 0.22f, 5, 7)
        builder.addCone(0f, 0.015f, -0.25f, 0.035f, 0.16f, 5, 0.65f)
        when (definition.id) {
            WorldId.OCEAN -> {
                // Gull: long swept wings with slightly raised tips.
                builder.pushTrs(0f, 0f, 0f, 0f, 0f, 0.08f, 1f)
                builder.addBox(0.31f, 0.015f, 0f, 0.31f, 0.018f, 0.075f)
                builder.addBox(-0.31f, 0.015f, 0f, 0.31f, 0.018f, 0.075f)
                builder.pop()
            }
            WorldId.MOUNTAIN -> {
                // Alpine eagle: broad primary feathers and a fanned tail.
                builder.addBox(0.30f, 0f, 0f, 0.34f, 0.025f, 0.12f)
                builder.addBox(-0.30f, 0f, 0f, 0.34f, 0.025f, 0.12f)
                for (feather in 0 until 3) {
                    builder.addEllipsoid(0.42f + feather * 0.07f, -0.01f, 0.10f, 0.10f, 0.018f, 0.09f, 4, 5)
                    builder.addEllipsoid(-0.42f - feather * 0.07f, -0.01f, 0.10f, 0.10f, 0.018f, 0.09f, 4, 5)
                }
            }
            WorldId.SNOW -> {
                // Compact ptarmigan-like profile with rounded wings.
                builder.addEllipsoid(0.22f, -0.015f, 0f, 0.24f, 0.045f, 0.14f, 5, 7)
                builder.addEllipsoid(-0.22f, -0.015f, 0f, 0.24f, 0.045f, 0.14f, 5, 7)
            }
            WorldId.DESERT -> {
                // Broad soaring desert raptor wings.
                builder.addBox(0.32f, 0f, 0f, 0.36f, 0.018f, 0.14f)
                builder.addBox(-0.32f, 0f, 0f, 0.36f, 0.018f, 0.14f)
                builder.addCone(0f, 0f, 0.28f, 0.11f, 0.28f, 5, 0.65f)
            }
            WorldId.FOREST -> {
                // Swallow: tapered, swept wings and forked tail.
                builder.addBox(0.26f, 0f, -0.02f, 0.28f, 0.014f, 0.075f)
                builder.addBox(-0.26f, 0f, -0.02f, 0.28f, 0.014f, 0.075f)
                builder.addCone(0.07f, 0f, 0.28f, 0.025f, 0.22f, 4, 0.5f)
                builder.addCone(-0.07f, 0f, 0.28f, 0.025f, 0.22f, 4, 0.5f)
            }
        }
        return builder.build()
    }

    private fun buildAnimalData(): MeshData {
        val builder = MeshBuilder()
        when (definition.id) {
            WorldId.DESERT -> {
                // Dromedary-like desert grazer with a distinct single hump and long neck.
                builder.addEllipsoid(0f, 0.52f, 0f, 0.30f, 0.25f, 0.58f, 6, 9)
                builder.addEllipsoid(0.02f, 0.78f, 0.02f, 0.20f, 0.25f, 0.23f, 5, 7)
                builder.addCylinder(0f, 0.82f, -0.43f, 0.075f, 0.065f, 0.55f, 6, false, false)
                builder.addEllipsoid(0f, 1.12f, -0.68f, 0.12f, 0.13f, 0.16f, 5, 7)
            }
            WorldId.SNOW -> {
                // Arctic fox: low body, deep chest, short legs and bushy tail.
                builder.addEllipsoid(0f, 0.38f, 0f, 0.28f, 0.24f, 0.48f, 6, 9)
                builder.addEllipsoid(0f, 0.49f, -0.39f, 0.17f, 0.17f, 0.19f, 5, 7)
                builder.addCone(0f, 0.34f, 0.52f, 0.16f, 0.42f, 5, 0.75f)
                builder.addCone(-0.09f, 0.69f, -0.46f, 0.055f, 0.20f, 4, 0.7f)
                builder.addCone(0.09f, 0.69f, -0.46f, 0.055f, 0.20f, 4, 0.7f)
            }
            WorldId.OCEAN -> {
                // Resting seal silhouette on the coastal rocks.
                builder.addEllipsoid(0f, 0.36f, 0f, 0.30f, 0.24f, 0.60f, 7, 10)
                builder.addEllipsoid(0f, 0.49f, -0.48f, 0.18f, 0.18f, 0.22f, 6, 8)
                builder.addEllipsoid(0.20f, 0.20f, 0.48f, 0.12f, 0.08f, 0.28f, 5, 7)
                builder.addEllipsoid(-0.20f, 0.20f, 0.48f, 0.12f, 0.08f, 0.28f, 5, 7)
            }
            WorldId.MOUNTAIN -> {
                // Ibex-like alpine grazer with swept-back horns.
                builder.addEllipsoid(0f, 0.48f, 0f, 0.28f, 0.23f, 0.52f, 6, 9)
                builder.addEllipsoid(0f, 0.68f, -0.42f, 0.15f, 0.16f, 0.18f, 5, 7)
                builder.addCone(-0.10f, 0.92f, -0.43f, 0.055f, 0.38f, 5, 0.65f)
                builder.addCone(0.10f, 0.92f, -0.43f, 0.055f, 0.38f, 5, 0.65f)
            }
            WorldId.FOREST -> {
                // Roe deer-like body with neck and small antlers.
                builder.addEllipsoid(0f, 0.48f, 0f, 0.26f, 0.22f, 0.52f, 6, 9)
                builder.addEllipsoid(0f, 0.67f, -0.42f, 0.15f, 0.15f, 0.20f, 5, 7)
                builder.addCylinder(0f, 0.74f, -0.48f, 0.045f, 0.04f, 0.22f, 5, false, false)
                builder.addCone(-0.09f, 0.91f, -0.45f, 0.025f, 0.22f, 4, 0.55f)
                builder.addCone(0.09f, 0.91f, -0.45f, 0.025f, 0.22f, 4, 0.55f)
            }
        }
        // Shared four-leg stance.
        builder.addBox(0.16f, 0.20f, 0.30f, 0.05f, 0.20f, 0.05f)
        builder.addBox(-0.16f, 0.20f, 0.30f, 0.05f, 0.20f, 0.05f)
        builder.addBox(0.16f, 0.20f, -0.30f, 0.05f, 0.20f, 0.05f)
        builder.addBox(-0.16f, 0.20f, -0.30f, 0.05f, 0.20f, 0.05f)
        return builder.build()
    }
}
