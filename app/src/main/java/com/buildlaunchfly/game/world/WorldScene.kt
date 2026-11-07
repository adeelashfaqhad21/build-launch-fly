package com.buildlaunchfly.game.world

import com.buildlaunchfly.game.gfx.Camera
import com.buildlaunchfly.game.gfx.InstancedMesh
import com.buildlaunchfly.game.gfx.Material
import com.buildlaunchfly.game.gfx.Mesh
import com.buildlaunchfly.game.gfx.MeshBuilder
import com.buildlaunchfly.game.gfx.MeshData
import com.buildlaunchfly.game.gfx.ParticleRenderer
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

    val treeTrunkMesh: Mesh
    val treeCanopyMesh: Mesh
    val rockMesh: Mesh
    val grassMesh: Mesh?
    val bushMesh: Mesh

    val treeMaterial: Material
    val canopyMaterial: Material
    val rockMaterial: Material
    val grassMaterial: Material
    val bushMaterial: Material
    val birdMaterial: Material
    val animalMaterial: Material

    val treeTrunks: InstancedMesh
    val treeCanopies: InstancedMesh
    val rocks: InstancedMesh
    val grass: InstancedMesh?
    val bushes: InstancedMesh
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
        val canopyData = buildCanopyData()
        val rockData = buildRockData()
        val bushData = buildBushData()

        treeTrunkMesh = Mesh("treeTrunk", trunkData)
        treeCanopyMesh = Mesh("treeCanopy", canopyData)
        rockMesh = Mesh("rock", rockData)
        bushMesh = Mesh("bush", bushData)

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
        treeTrunks = InstancedMesh("treeTrunks", trunkData, treeCapacity)
        treeCanopies = InstancedMesh("treeCanopies", canopyData, treeCapacity)
        if (treeSet.count > 0) {
            treeTrunks.setInstances(treeSet.matrices, treeSet.count)
            treeCanopies.setInstances(treeSet.matrices, treeSet.count)
        }

        val rockCount = (definition.rockCount * scale).toInt()
        val bushBaseCount = when (definition.id) {
            WorldId.FOREST -> 620
            WorldId.DESERT -> 110
            WorldId.SNOW -> 90
            WorldId.OCEAN -> 180
            WorldId.MOUNTAIN -> 440
        }
        val bushSet = Vegetation.scatterBushes(terrain, definition, (bushBaseCount * scale).toInt(), definition.seed)
        bushes = InstancedMesh("bushes", bushData, bushSet.count.coerceAtLeast(1))
        if (bushSet.count > 0) bushes.setInstances(bushSet.matrices, bushSet.count)
        val rockSet = Vegetation.scatterRocks(terrain, definition, rockCount, definition.seed)
        rocks = InstancedMesh("rocks", rockData, rockSet.count.coerceAtLeast(1))
        if (rockSet.count > 0) rocks.setInstances(rockSet.matrices, rockSet.count)

        val grassCount = (definition.grassCount * scale).toInt()
        if (grassCount > 0) {
            val grassData = buildGrassData()
            grassMesh = Mesh("grass", grassData)
            val grassSet = Vegetation.scatterGrass(
                terrain, definition, grassCount, definition.seed, 0f, -40f, 170f
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
        treeTrunkMesh.dispose()
        treeCanopyMesh.dispose()
        rockMesh.dispose()
        bushMesh.dispose()
        grassMesh?.dispose()
        treeTrunks.dispose()
        treeCanopies.dispose()
        rocks.dispose()
        bushes.dispose()
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

    private fun buildCanopyData(): MeshData {
        val builder = MeshBuilder()
        when {
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
                builder.addSphere(0f, 1.02f, 0f, 0.50f, 6, 9, 1.4f)
                builder.addSphere(0.22f, 0.86f, 0.12f, 0.34f, 5, 8, 1.4f)
                builder.addSphere(-0.20f, 0.92f, -0.14f, 0.31f, 5, 8, 1.4f)
            }
        }
        return builder.build()
    }

    private fun buildBushData(): MeshData {
        val builder = MeshBuilder()
        when (definition.id) {
            WorldId.DESERT -> {
                builder.addCylinder(0f, 0.30f, 0f, 0.045f, 0.035f, 0.58f, 6, false, false, 1f)
                builder.addCylinder(-0.28f, 0.30f, 0f, 0.025f, 0.025f, 0.48f, 5, false, false, 1f)
                builder.addCylinder(0.27f, 0.28f, 0.04f, 0.025f, 0.025f, 0.46f, 5, false, false, 1f)
                builder.addEllipsoid(0f, 0.40f, 0f, 0.38f, 0.22f, 0.34f, 7, 10)
                builder.addEllipsoid(-0.30f, 0.34f, 0.08f, 0.23f, 0.18f, 0.22f, 6, 8)
                builder.addEllipsoid(0.28f, 0.36f, -0.08f, 0.25f, 0.19f, 0.23f, 6, 8)
            }
            WorldId.SNOW -> {
                builder.addCylinder(0f, 0.24f, 0f, 0.045f, 0.035f, 0.42f, 6, false, false, 1f)
                builder.addCone(0f, 0.35f, 0f, 0.42f, 0.48f, 8, 1.25f)
                builder.addCone(-0.20f, 0.28f, 0.05f, 0.30f, 0.36f, 7, 1.2f)
                builder.addCone(0.20f, 0.30f, -0.04f, 0.30f, 0.38f, 7, 1.2f)
            }
            WorldId.OCEAN -> {
                builder.addCylinder(0f, 0.22f, 0f, 0.025f, 0.02f, 0.40f, 5, false, false, 1f)
                builder.addEllipsoid(0f, 0.32f, 0f, 0.14f, 0.42f, 0.14f, 5, 8)
                builder.addEllipsoid(-0.20f, 0.26f, 0.08f, 0.12f, 0.32f, 0.12f, 5, 8)
                builder.addEllipsoid(0.19f, 0.29f, -0.06f, 0.12f, 0.36f, 0.12f, 5, 8)
            }
            WorldId.FOREST, WorldId.MOUNTAIN -> {
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
