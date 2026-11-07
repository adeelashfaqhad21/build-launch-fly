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

    val treeMaterial: Material
    val canopyMaterial: Material
    val rockMaterial: Material
    val grassMaterial: Material
    val birdMaterial: Material
    val animalMaterial: Material

    val treeTrunks: InstancedMesh
    val treeCanopies: InstancedMesh
    val rocks: InstancedMesh
    val grass: InstancedMesh?
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

        treeTrunkMesh = Mesh("treeTrunk", trunkData)
        treeCanopyMesh = Mesh("treeCanopy", canopyData)
        rockMesh = Mesh("rock", rockData)

        treeMaterial = Material(
            albedo = textures.bark,
            tint = Vec3(0.82f, 0.76f, 0.68f),
            roughness = 0.92f,
            metallic = 0f,
            uvScale = 2f
        )
        canopyMaterial = Material(
            albedo = textures.leaf,
            tint = Vec3(1f, 1f, 1f),
            roughness = 0.85f,
            metallic = 0f,
            uvScale = 2.5f
        )
        rockMaterial = Material(
            albedo = textures.surface(definition.surfaceB),
            tint = definition.tintB,
            roughness = 0.95f,
            metallic = 0f,
            uvScale = 1.6f
        )
        grassMaterial = Material(
            albedo = textures.leaf,
            tint = Vec3(0.92f, 1.08f, 0.72f),
            roughness = 0.90f,
            metallic = 0f,
            uvScale = 1f
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
        treeTrunkMesh.dispose()
        treeCanopyMesh.dispose()
        rockMesh.dispose()
        grassMesh?.dispose()
        treeTrunks.dispose()
        treeCanopies.dispose()
        rocks.dispose()
        grass?.dispose()
        birdsMesh.dispose()
        animalsMesh.dispose()
    }

    // ---- geometry ---------------------------------------------------------

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

    private fun buildCanopyData(): MeshData {
        val builder = MeshBuilder()
        if (definition.pineTrees) {
            builder.addCone(0f, 0.30f, 0f, 0.52f, 0.9f, 9, 1.5f)
            builder.addCone(0f, 0.72f, 0f, 0.40f, 0.75f, 9, 1.5f)
            builder.addCone(0f, 1.08f, 0f, 0.26f, 0.60f, 8, 1.5f)
        } else {
            builder.addSphere(0f, 1.02f, 0f, 0.50f, 6, 9, 1.4f)
            builder.addSphere(0.22f, 0.86f, 0.12f, 0.34f, 5, 8, 1.4f)
            builder.addSphere(-0.20f, 0.92f, -0.14f, 0.31f, 5, 8, 1.4f)
        }
        return builder.build()
    }

    private fun buildRockData(): MeshData {
        val builder = MeshBuilder()
        builder.addSphere(0f, 0.42f, 0f, 0.55f, 6, 9, 1.2f)
        return builder.build()
    }

    private fun buildGrassData(): MeshData {
        val builder = MeshBuilder()
        // Two crossed quads anchored at the ground plane.
        builder.addQuad(
            Vec3(-0.26f, 0f, 0f), Vec3(0.26f, 0f, 0f),
            Vec3(0.26f, 0.52f, 0f), Vec3(-0.26f, 0.52f, 0f)
        )
        builder.addQuad(
            Vec3(0f, 0f, -0.26f), Vec3(0f, 0f, 0.26f),
            Vec3(0f, 0.52f, 0.26f), Vec3(0f, 0.52f, -0.26f)
        )
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
