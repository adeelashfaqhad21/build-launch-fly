package com.buildlaunchfly.game.game

import android.content.Context
import android.util.Log
import com.buildlaunchfly.game.aircraft.AircraftBuild
import com.buildlaunchfly.game.aircraft.AircraftCatalog
import com.buildlaunchfly.game.aircraft.AircraftMesh
import com.buildlaunchfly.game.aircraft.FlightInput
import com.buildlaunchfly.game.aircraft.FlightPhysics
import com.buildlaunchfly.game.aircraft.FlightState
import com.buildlaunchfly.game.aircraft.PartSlot
import com.buildlaunchfly.game.audio.AudioEngine
import com.buildlaunchfly.game.gfx.Camera
import com.buildlaunchfly.game.gfx.SceneRenderer
import com.buildlaunchfly.game.gfx.TextureFactory
import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3
import com.buildlaunchfly.game.platform.InputState
import com.buildlaunchfly.game.slingshot.SlingUpgrade
import com.buildlaunchfly.game.slingshot.SlingshotLayout
import com.buildlaunchfly.game.slingshot.SlingshotMesh
import com.buildlaunchfly.game.slingshot.SlingshotState
import com.buildlaunchfly.game.world.WorldCatalog
import com.buildlaunchfly.game.world.WorldDefinition
import com.buildlaunchfly.game.world.WorldId
import com.buildlaunchfly.game.world.WorldScene

/**
 * Engine owner. Created on the Android side, but all GL resources are built and disposed on
 * the GL thread through [onSurfaceChanged] / [dispose].
 */
class GameApp(private val context: Context, private val input: InputState) {

    val audio = AudioEngine()

    /** The future UI plugs in here. */
    var hud: HudBridge? = null

    var quality: GraphicsQuality = GraphicsQuality.HIGH
        private set

    private val store = ProfileStore(context)
    private val profile = store.load()
    private val build = AircraftBuild(profile.partTiers)
    private val sling = SlingshotState(profile.slingshotLevels)

    private var textures: TextureFactory? = null
    private var renderer: SceneRenderer? = null
    private var scene: WorldScene? = null
    private var aircraft: AircraftMesh? = null
    private var slingMesh: SlingshotMesh? = null

    private val camera = Camera()
    private val cameraDirector = CameraDirector(camera)
    private val physics = FlightPhysics()

    private var state = GameState.SEATED
    private var initialized = false

    private var time = 0f
    private var pull = 0f
    private var stretchActive = false
    private var steerPitch = 0f
    private var steerRoll = 0f
    private var flightTime = 0f
    private var resultTimer = 0f
    private var lastResult: FlightResult? = null

    private val aircraftModel = Mat4()
    private val propellerModel = Mat4()
    private val slingshotModel = Mat4()
    private val bandLocal = Mat4()
    private val leftBand = Mat4()
    private val rightBand = Mat4()
    private val seatPosition = Vec3()
    private val padPosition = Vec3()
    private val focusPoint = Vec3()
    private val dragScratch = FloatArray(2)
    private val flightInput = FlightInput()
    private val particleColorSnow = Vec3(1f, 1f, 1f)
    private val particleColorDust = Vec3(0.95f, 0.84f, 0.66f)

    val playerProfile: PlayerProfile get() = profile
    val aircraftBuild: AircraftBuild get() = build
    val slingshotState: SlingshotState get() = sling
    val currentState: GameState get() = state
    val lastFlightResult: FlightResult? get() = lastResult

    fun onContextCreated() {
        if (initialized) {
            Log.i(TAG, "GL context recreated, rebuilding resources")
            dispose()
        }
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        camera.setViewport(width, height)
        if (!initialized) {
            createResources(width, height)
            initialized = true
        } else {
            renderer?.resize(width, height)
        }
    }

    // ---- setup ------------------------------------------------------------

    private fun createResources(width: Int, height: Int) {
        val textureFactory = TextureFactory()
        textures = textureFactory

        val sceneRenderer = SceneRenderer(width, height, textureFactory.white)
        renderer = sceneRenderer

        aircraft = AircraftMesh(textureFactory).also { it.updateFor(build) }
        slingMesh = SlingshotMesh(textureFactory)

        rebuildScene(profile.selectedWorld)

        val padY = scene?.terrain?.padHeight ?: 6f
        padPosition.set(0f, padY, 0f)
        seatPosition.set(
            padPosition.x + SlingshotLayout.HOLD_POINT.x,
            padPosition.y + SlingshotLayout.HOLD_POINT.y,
            padPosition.z + SlingshotLayout.HOLD_POINT.z
        )
        physics.reset(seatPosition)

        cameraDirector.snapTo(
            Vec3(seatPosition.x + 6.4f, seatPosition.y + 2f, seatPosition.z + 9f),
            seatPosition
        )

        audio.start()
        audio.setAmbience(scene?.definition?.ambience ?: com.buildlaunchfly.game.world.AmbienceStyle.FOREST)
        hud?.onCoinsChanged(profile.coins)
        hud?.onStateChanged(state)
        Log.i(TAG, "Engine ready\n${build.summary()}")
    }

    private fun rebuildScene(id: WorldId) {
        val sceneRenderer = renderer ?: return
        val textureFactory = textures ?: return
        val definition = WorldCatalog.byId(id)

        scene?.dispose()
        val newScene = WorldScene(definition, textureFactory, quality.detailLevel)
        scene = newScene

        applyEnvironment(definition, sceneRenderer)
        padPosition.set(0f, newScene.terrain.padHeight, 0f)
        slingshotModel.identity().translate(padPosition.x, padPosition.y, padPosition.z)
        audio.setAmbience(definition.ambience)
        hud?.onWorldChanged(definition)
        Log.i(TAG, "World ready: ${definition.displayName} (pad ${newScene.terrain.padHeight})")
    }

    private fun applyEnvironment(definition: WorldDefinition, sceneRenderer: SceneRenderer) {
        val env = sceneRenderer.env
        env.sunDir.set(definition.sunDirection).normalize()
        env.sunColor.set(definition.sunColor)
        env.skyColor.set(definition.ambientSky)
        env.groundColor.set(definition.ambientGround)
        env.fogColor.set(definition.fogColor)
        env.zenithColor.set(definition.zenithColor)
        env.horizonColor.set(definition.horizonColor)
        env.fogDensity = definition.fogDensity
        env.shadowStrength = if (quality == GraphicsQuality.LOW) 0.5f else 0.72f
    }

    // ---- simulation -------------------------------------------------------

    fun update(dtSeconds: Float) {
        if (!initialized) return
        val sceneRenderer = renderer ?: return
        val currentScene = scene ?: return

        val dt = dtSeconds.coerceIn(0f, 0.05f)
        time += dt
        sceneRenderer.env.time = time

        input.consumeDrag(dragScratch)
        val released = input.consumeRelease()
        steerRoll = (steerRoll + dragScratch[0] * 0.06f).coerceIn(-1f, 1f)
        steerPitch = (steerPitch - dragScratch[1] * 0.06f).coerceIn(-1f, 1f)
        val decay = (1f - 1.6f * dt).coerceAtLeast(0f)
        steerRoll *= decay
        steerPitch *= decay

        when (state) {
            GameState.SEATED -> updateSeated(dt, released)
            GameState.FLIGHT -> updateFlight(dt, currentScene)
            GameState.RESULTS -> {
                resultTimer += dt
                if (resultTimer > 2.4f) resetToSeated()
            }
        }

        when (state) {
            GameState.SEATED -> cameraDirector.updateSeated(padPosition, seatPosition, pull, dt)
            GameState.FLIGHT -> cameraDirector.updateFlight(
                physics.position, physics.velocity, physics.speed(), dt
            )
            GameState.RESULTS -> cameraDirector.updateResults(focusPoint, time, dt)
        }
        camera.update()

        sceneRenderer.particles.begin()
        currentScene.update(dt, time, camera, sceneRenderer.particles)
    }

    private fun updateSeated(dt: Float, released: Boolean) {
        if (input.pointerDown) {
            stretchActive = true
            pull = (pull + dt * 0.9f).coerceIn(0f, 1f)
            audio.setStretch(pull)
        } else if (stretchActive || released) {
            stretchActive = false
            audio.setStretch(0f)
            if (pull > 0.08f) {
                launch(pull)
            } else {
                pull = 0f
            }
        }
    }

    private fun updateFlight(dt: Float, currentScene: WorldScene) {
        val stats = build.stats()
        val ground = currentScene.terrain.height(physics.position.x, physics.position.z)
        flightInput.pitch = steerPitch
        flightInput.roll = steerRoll
        physics.update(dt, stats, ground, flightInput)
        flightTime += dt

        audio.setFlight(
            physics.speedFactor(),
            (physics.propellerRpm / 2400f).coerceIn(0f, 1f),
            true
        )
        hud?.onDistance(physics.distance, physics.speed())

        if (physics.state != FlightState.FLYING) finishFlight(currentScene)
    }

    /** Fires the aircraft. Exposed so menus can replay or auto-launch later. */
    fun launch(stretch: Float) {
        val stats = build.stats()
        val speed = sling.launchSpeed(stretch, stats.mass)
        seatPosition.set(
            padPosition.x + SlingshotLayout.HOLD_POINT.x,
            padPosition.y + SlingshotLayout.HOLD_POINT.y,
            padPosition.z + SlingshotLayout.HOLD_POINT.z
        )
        physics.reset(seatPosition)
        physics.launch(speed, LAUNCH_PITCH_DEGREES, 0f)

        state = GameState.FLIGHT
        flightTime = 0f
        pull = 0f
        audio.playRelease(stretch)
        hud?.onLaunched(stretch, speed)
        hud?.onStateChanged(state)
        Log.i(TAG, "Launch: stretch=${"%.2f".format(stretch)} speed=${"%.2f".format(speed)}")
    }

    private fun finishFlight(currentScene: WorldScene) {
        val result = Economy.evaluate(
            physics.distance,
            physics.maxSpeed,
            flightTime,
            physics.state == FlightState.LANDED,
            currentScene.definition,
            profile.bestDistance,
            sling.powerRating()
        )
        profile.recordFlight(result.distanceMeters, result.coins, result.isRecord)
        store.save(profile)

        lastResult = result
        focusPoint.set(physics.position)
        state = GameState.RESULTS
        resultTimer = 0f
        audio.setFlight(0f, 0f, false)
        audio.playUi(AudioEngine.Cue.RESULT)
        hud?.onFlightFinished(result)
        hud?.onCoinsChanged(profile.coins)
        hud?.onStateChanged(state)
        Log.i(
            TAG,
            "Flight finished: ${"%.1f".format(result.distanceMeters)} m, " +
                "coins=${result.coins}, record=${result.isRecord}"
        )
    }

    private fun resetToSeated() {
        physics.reset(seatPosition)
        pull = 0f
        stretchActive = false
        resultTimer = 0f
        state = GameState.SEATED
        audio.setFlight(0f, 0f, false)
        cameraDirector.snapTo(
            Vec3(seatPosition.x + 6.4f, seatPosition.y + 2f, seatPosition.z + 9f),
            seatPosition
        )
        hud?.onStateChanged(state)
    }

    // ---- rendering --------------------------------------------------------

    fun render() {
        if (!initialized) return
        val sceneRenderer = renderer ?: return
        val currentScene = scene ?: return
        val textureFactory = textures ?: return
        val aircraftMesh = aircraft ?: return
        val rig = slingMesh ?: return
        val definition = currentScene.definition

        if (state == GameState.SEATED) {
            val hold = rig.holdPoint(pull)
            aircraftModel.setTrs(
                padPosition.x + hold.x,
                padPosition.y + hold.y,
                padPosition.z + hold.z,
                0.10f * pull, 0f, 0f,
                1f
            )
            propellerModel.set(aircraftModel)
            propellerModel.translate(0f, 0f, -1.62f)
        } else {
            aircraftModel.set(physics.orientation)
            physics.propellerMatrix(propellerModel)
        }

        camera.update()
        sceneRenderer.beginFrame(camera)

        val shadowFocus = if (state == GameState.SEATED) seatPosition else physics.position
        sceneRenderer.beginShadowPass(shadowFocus, quality.shadowRadius)
        aircraftMesh.bodyMesh?.let { sceneRenderer.shadowMesh(it, aircraftModel) }
        aircraftMesh.propellerMesh?.let { sceneRenderer.shadowMesh(it, propellerModel) }
        if (state == GameState.SEATED) sceneRenderer.shadowMesh(rig.frameMesh, slingshotModel)
        sceneRenderer.shadowInstanced(currentScene.treeTrunks)
        sceneRenderer.shadowInstanced(currentScene.rocks)
        sceneRenderer.endShadowPass()

        sceneRenderer.drawSky()
        sceneRenderer.drawTerrain(
            currentScene.terrainMesh,
            textureFactory.surface(definition.surfaceA),
            textureFactory.surface(definition.surfaceB),
            textureFactory.surface(definition.surfaceC),
            definition.tintA,
            definition.tintB,
            definition.tintC,
            definition.rockThreshold,
            definition.snowLine
        )

        sceneRenderer.drawInstanced(currentScene.treeTrunks, currentScene.treeMaterial)
        sceneRenderer.drawInstanced(currentScene.treeCanopies, currentScene.canopyMaterial)
        sceneRenderer.drawInstanced(currentScene.rocks, currentScene.rockMaterial)
        currentScene.grass?.let { sceneRenderer.drawInstanced(it, currentScene.grassMaterial) }
        sceneRenderer.drawInstanced(currentScene.birdsMesh, currentScene.birdMaterial)
        sceneRenderer.drawInstanced(currentScene.animalsMesh, currentScene.animalMaterial)

        currentScene.waterMesh?.let { water ->
            sceneRenderer.drawWater(
                water, textureFactory.waterNormals, currentScene.waterDeep, currentScene.waterShallow
            )
        }

        if (state == GameState.SEATED) {
            sceneRenderer.drawMesh(rig.frameMesh, rig.frameMaterial, slingshotModel)

            rig.thicknessBoost = slingStateBandBoost()
            rig.leftBandMatrix(pull, bandLocal)
            leftBand.setProduct(slingshotModel, bandLocal)
            sceneRenderer.drawMesh(rig.bandMesh, rig.bandMaterial, leftBand)

            rig.rightBandMatrix(pull, bandLocal)
            rightBand.setProduct(slingshotModel, bandLocal)
            sceneRenderer.drawMesh(rig.bandMesh, rig.bandMaterial, rightBand)
        }

        aircraftMesh.bodyMesh?.let { sceneRenderer.drawMesh(it, aircraftMesh.bodyMaterial, aircraftModel) }
        aircraftMesh.propellerMesh?.let {
            sceneRenderer.drawMesh(it, aircraftMesh.propellerMaterial, propellerModel)
        }

        val particleColor = if (definition.snowfall > 0f) particleColorSnow else particleColorDust
        sceneRenderer.flushParticles(particleColor)
    }

    private fun slingStateBandBoost(): Float = (sling.bandThickness() - 1f).coerceIn(0f, 1f)

    // ---- actions reserved for the UI --------------------------------------

    fun selectWorld(id: WorldId): Boolean {
        profile.refreshUnlocks()
        if (!profile.unlockedWorlds.contains(id)) return false
        profile.selectWorld(id)
        store.save(profile)
        rebuildScene(id)
        physics.reset(seatPosition)
        resetToSeated()
        return true
    }

    /** Buys and installs the next tier of a slot if it is affordable. */
    fun installNextPart(slot: PartSlot): Boolean {
        val current = build.tierOf(slot)
        val next = AircraftCatalog.nextUpgrade(slot, current) ?: return false
        if (profile.coins < next.price) return false

        profile.coins -= next.price
        build.install(slot, next.tier)
        profile.partTiers[slot] = next.tier
        aircraft?.updateFor(build)
        store.save(profile)

        audio.playUi(AudioEngine.Cue.UPGRADE)
        hud?.onCoinsChanged(profile.coins)
        Log.i(TAG, "Installed ${next.displayName}\n${build.summary()}")
        return true
    }

    fun upgradeSlingshot(type: SlingUpgrade): Boolean {
        if (sling.isMaxed(type)) return false
        val cost = sling.costToUpgrade(type)
        if (profile.coins < cost) return false

        profile.coins -= cost
        sling.applyUpgrade(type)
        profile.slingshotLevels[type] = sling.levelOf(type)
        store.save(profile)

        audio.playUi(AudioEngine.Cue.UPGRADE)
        hud?.onCoinsChanged(profile.coins)
        return true
    }

    fun setGraphicsQuality(next: GraphicsQuality) {
        if (next == quality) return
        quality = next
        rebuildScene(profile.selectedWorld)
        physics.reset(seatPosition)
        resetToSeated()
    }

    fun dispose() {
        audio.stop()
        scene?.dispose()
        scene = null
        aircraft?.dispose()
        aircraft = null
        slingMesh?.dispose()
        slingMesh = null
        renderer?.dispose()
        renderer = null
        textures?.disposeAll()
        textures = null
        initialized = false
    }

    companion object {
        private const val TAG = "BLF-GAME"
        private const val LAUNCH_PITCH_DEGREES = 14f
    }
}
