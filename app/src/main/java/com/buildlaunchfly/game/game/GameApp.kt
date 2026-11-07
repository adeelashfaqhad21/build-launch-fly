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
import com.buildlaunchfly.game.gfx.DesignTokens
import com.buildlaunchfly.game.gfx.SceneRenderer
import com.buildlaunchfly.game.gfx.TextureFactory
import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3
import com.buildlaunchfly.game.platform.InputState
import com.buildlaunchfly.game.slingshot.SlingUpgrade
import com.buildlaunchfly.game.slingshot.SlingshotLayout
import com.buildlaunchfly.game.slingshot.SlingshotMesh
import com.buildlaunchfly.game.slingshot.SlingshotState
import com.buildlaunchfly.game.world.AmbienceStyle
import com.buildlaunchfly.game.world.WorldCatalog
import com.buildlaunchfly.game.world.WorldDefinition
import com.buildlaunchfly.game.world.WorldId
import com.buildlaunchfly.game.world.WorldScene
import java.util.concurrent.ConcurrentLinkedQueue

/** Cameras the UI can request while the aircraft is on the pad. */
enum class CameraMode { LAUNCH, ORBIT_AIRCRAFT, ORBIT_SLINGSHOT }

/**
 * Engine owner. Created on the Android side; all GL resources are built and disposed on the
 * GL thread. The UI layer talks to it through the `request*` methods, which are queued and
 * applied at the start of the next frame so no GL call ever happens on the UI thread.
 */
class GameApp(private val context: Context, private val input: InputState) {

    val audio = AudioEngine()

    /** Optional non-UI bridge (kept for headless integrations). */
    var hud: HudBridge? = null

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
    private var slingRecoilTimer = 0f
    private var slingRecoilStretch = 0f
    private var steerPitch = 0f
    private var steerYaw = 0f
    private var steerRoll = 0f
    private var flightTime = 0f
    private var resultTimer = 0f
    private var lastResult: FlightResult? = null
    private var maxAltitudeThisFlight = 0f
    private var lastFlightMaxAltitude = 0f

    private var viewportWidth = 1
    private var viewportHeight = 1

    private val actions = ConcurrentLinkedQueue<Runnable>()

    private val aircraftModel = Mat4()
    private val propellerModel = Mat4()
    private val propellerBladeModels = Array(3) { Mat4() }
    private val slingshotModel = Mat4()
    private val bandLocal = Mat4()
    private val cradleModel = Mat4()
    private val hydraulicLeftModel = Mat4()
    private val hydraulicRightModel = Mat4()
    private val leftBand = Mat4()
    private val rightBand = Mat4()
    private val seatPosition = Vec3()
    private val padPosition = Vec3()
    private val focusPoint = Vec3()
    private val dragScratch = FloatArray(2)
    private val flightInput = FlightInput()
    private val particleColorSnow = Vec3(1f, 1f, 1f)
    private val particleColorDust = Vec3(0.95f, 0.84f, 0.66f)
    private val particleColorSeaSpray = Vec3(0.72f, 0.86f, 0.92f)
    private val particleColorForest = Vec3(0.86f, 0.94f, 0.66f)
    private val particleColorMountain = Vec3(0.82f, 0.90f, 1.00f)
    private val bandTintAmber = Vec3(0.96f, 0.62f, 0.11f)
    private val bandTintCyan = Vec3(0.22f, 0.74f, 0.97f)
    private val bandTintScratch = Vec3(1f, 1f, 1f)

    /** True while a UI modal is up; freezes simulation but keeps rendering. */
    @Volatile
    var paused: Boolean = false

    @Volatile
    var cameraMode: CameraMode = CameraMode.ORBIT_AIRCRAFT

    /** Extra yaw applied to workshop/menu orbits from UI drag. */
    @Volatile
    var orbitOffset: Float = 0f

    var lastLaunchStretch: Float = 0.75f
        private set

    val playerProfile: PlayerProfile get() = profile
    val aircraftBuild: AircraftBuild get() = build
    val slingshotState: SlingshotState get() = sling
    val settings: GameSettings get() = profile.settings
    val quality: GraphicsQuality get() = profile.settings.quality
    val currentState: GameState get() = state
    val lastFlightResult: FlightResult? get() = lastResult
    val lastFlightAltitude: Float get() = lastFlightMaxAltitude
    val isInitialized: Boolean get() = initialized

    // ---- lifecycle --------------------------------------------------------

    fun onContextCreated() {
        if (initialized) {
            Log.i(TAG, "GL context recreated, rebuilding resources")
            dispose()
        }
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
        camera.setViewport(viewportWidth, viewportHeight)
        if (!initialized) {
            createResources(viewportWidth, viewportHeight)
            initialized = true
        } else {
            renderer?.resize(viewportWidth, viewportHeight)
        }
    }

    // ---- setup ------------------------------------------------------------

    private fun createResources(width: Int, height: Int) {
        val textureSize = if (settings.highResTextures) TextureFactory.SIZE else 128
        val textureFactory = TextureFactory(seed = 1337, size = textureSize)
        textures = textureFactory

        val sceneRenderer = SceneRenderer(width, height, textureFactory.white)
        renderer = sceneRenderer

        aircraft = AircraftMesh(textureFactory).also { it.updateFor(build) }
        slingMesh = SlingshotMesh(textureFactory)

        rebuildScene(profile.selectedWorld)

        val padY = scene?.terrain?.padHeight ?: 6f
        padPosition.set(0f, padY, 0f)
        val restingPoint = slingMesh?.holdPoint(0f) ?: SlingshotLayout.HOLD_POINT
        seatPosition.set(
            padPosition.x + restingPoint.x,
            padPosition.y + restingPoint.y,
            padPosition.z + restingPoint.z
        )
        physics.reset(seatPosition)

        cameraDirector.snapTo(
            Vec3(seatPosition.x + 6.4f, seatPosition.y + 2f, seatPosition.z + 9f),
            seatPosition
        )

        applyAudioVolumes()
        audio.start()
        audio.setAmbience(scene?.definition?.ambience ?: AmbienceStyle.FOREST)
        audio.setAmbienceEnabled(false)
        hud?.onCoinsChanged(profile.coins)
        hud?.onStateChanged(state)
        Log.i(TAG, "Engine ready\n${build.summary()}")
    }

    private fun applyAudioVolumes() {
        audio.applyVolumes(
            settings.masterVolume,
            settings.engineVolume,
            settings.ambienceVolume
        )
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
        env.fogDensity = if (settings.volumetricFog) {
            definition.fogDensity
        } else {
            definition.fogDensity * 0.28f
        }
        env.shadowStrength = when (quality) {
            GraphicsQuality.LOW -> 0.45f
            GraphicsQuality.MEDIUM -> 0.6f
            GraphicsQuality.HIGH -> 0.72f
            GraphicsQuality.ULTRA -> 0.8f
        }
    }

    // ---- queued UI actions ------------------------------------------------

    fun post(action: Runnable) {
        actions.add(action)
    }

    private fun drainActions() {
        while (true) {
            val action = actions.poll() ?: break
            try {
                action.run()
            } catch (error: Throwable) {
                Log.e(TAG, "Queued action failed", error)
            }
        }
    }

    fun requestEnterLaunch() {
        post(Runnable {
            returnToLaunch()
            cameraMode = CameraMode.LAUNCH
        })
    }

    fun requestReturnToMenu() {
        post(Runnable { returnToLaunch() })
    }

    fun requestLaunch(stretch: Float) {
        post(Runnable { launchInternal(stretch) })
    }

    fun requestInstallPart(slot: PartSlot) {
        post(Runnable {
            val next = AircraftCatalog.nextUpgrade(slot, build.tierOf(slot))
            if (next != null && profile.coins >= next.price) {
                profile.coins -= next.price
                build.install(slot, next.tier)
                profile.partTiers[slot] = next.tier
                aircraft?.updateFor(build)
                store.save(profile)
                audio.playUi(AudioEngine.Cue.UPGRADE)
                hud?.onCoinsChanged(profile.coins)
                Log.i(TAG, "Installed ${next.displayName}\n${build.summary()}")
            }
        })
    }

    fun requestUpgradeSlingshot(type: SlingUpgrade) {
        post(Runnable {
            if (!sling.isMaxed(type)) {
                val cost = sling.costToUpgrade(type)
                if (profile.coins >= cost) {
                    profile.coins -= cost
                    sling.applyUpgrade(type)
                    profile.slingshotLevels[type] = sling.levelOf(type)
                    store.save(profile)
                    audio.playUi(AudioEngine.Cue.UPGRADE)
                    hud?.onCoinsChanged(profile.coins)
                }
            }
        })
    }

    fun requestSelectWorld(id: WorldId) {
        post(Runnable {
            profile.refreshUnlocks()
            if (profile.unlockedWorlds.contains(id)) {
                profile.selectWorld(id)
                store.save(profile)
                rebuildScene(id)
                physics.reset(seatPosition)
                state = GameState.SEATED
                hud?.onStateChanged(state)
            }
        })
    }

    fun requestSetQuality(next: GraphicsQuality) {
        post(Runnable {
            if (settings.quality != next) {
                settings.quality = next
                rebuildScene(profile.selectedWorld)
                physics.reset(seatPosition)
                state = GameState.SEATED
                store.save(profile)
            }
        })
    }

    /**
     * Re-applies graphics/audio settings. [rebuildTextures] is required when the texture
     * resolution toggle changes, because every generated texture must be replaced.
     */
    fun requestReapplyGraphics(rebuildTextures: Boolean) {
        post(Runnable {
            applyAudioVolumes()
            store.save(profile)
            if (rebuildTextures) {
                val w = viewportWidth
                val h = viewportHeight
                dispose()
                if (w > 1 && h > 1) {
                    createResources(w, h)
                    initialized = true
                }
            } else {
                rebuildScene(profile.selectedWorld)
                physics.reset(seatPosition)
                state = GameState.SEATED
            }
        })
    }

    fun requestApplyAudio() {
        applyAudioVolumes()
    }

    fun requestSaveProfile() {
        post(Runnable { store.save(profile) })
    }

    fun requestOrbitDelta(deltaRadians: Float) {
        orbitOffset += deltaRadians
    }

    fun requestClaimAchievement(id: String) {
        post(Runnable {
            if (profile.claimAchievement(id)) {
                store.save(profile)
                audio.playUi(AudioEngine.Cue.REWARD)
                hud?.onCoinsChanged(profile.coins)
            }
        })
    }

    // ---- telemetry --------------------------------------------------------

    fun telemetry(): FlightTelemetry {
        val currentScene = scene ?: return FlightTelemetry()
        val groundY = currentScene.terrain.height(physics.position.x, physics.position.z)
        val altitude = (physics.position.y - groundY).coerceAtLeast(0f)
        val speed = physics.speed()
        return FlightTelemetry(
            distanceMeters = physics.distance,
            altitudeMeters = altitude,
            airspeedKmh = speed * 3.6f,
            pitchDegrees = physics.pitch * 57.29578f,
            rollDegrees = physics.roll * 57.29578f,
            yawDegrees = physics.yaw * 57.29578f,
            stallWarning = physics.pitch > 0.38f && speed < 17f && state == GameState.FLIGHT,
            verticalSpeed = physics.velocity.y,
            flightTimeSeconds = flightTime
        )
    }

    fun pullAmount(): Float = pull

    // ---- simulation -------------------------------------------------------

    fun update(dtSeconds: Float) {
        if (!initialized) return

        // Queued UI actions may rebuild scenes or textures, so they run before any
        // per-frame references are captured.
        drainActions()

        val sceneRenderer = renderer ?: return
        val currentScene = scene ?: return

        if (paused) return

        val dt = dtSeconds.coerceIn(0f, 0.05f)
        time += dt
        slingRecoilTimer = (slingRecoilTimer - dt).coerceAtLeast(0f)
        sceneRenderer.env.time = time

        input.consumeDrag(dragScratch)
        val released = input.consumeRelease()
        val cancelled = input.consumeCancel()
        if (state == GameState.FLIGHT) {
            // Horizontal drag commands yaw; vertical drag commands pitch.
            steerYaw = (steerYaw + dragScratch[0] * 3.8f).coerceIn(-1f, 1f)
            // Screen Y grows downward: a downward swipe commands a positive pitch
            // (nose up), while an upward swipe commands nose down.
            steerPitch = (steerPitch + dragScratch[1] * 3.8f).coerceIn(-1f, 1f)
        }
        val decay = (1f - 0.85f * dt).coerceAtLeast(0f)
        steerRoll *= decay
        steerPitch *= decay
        steerYaw *= decay

        when (state) {
            GameState.SEATED -> updateSeated(dt, released, cancelled)
            GameState.FLIGHT -> updateFlight(dt, currentScene)
            GameState.RESULTS -> {
                resultTimer += dt
            }
        }

        when (state) {
            GameState.SEATED -> when (cameraMode) {
                CameraMode.ORBIT_AIRCRAFT -> cameraDirector.updateOrbitAircraft(
                    seatPosition, time, dt, 9.5f, orbitOffset
                )
                CameraMode.ORBIT_SLINGSHOT -> cameraDirector.updateOrbitSlingshot(
                    padPosition, time, dt, orbitOffset
                )
                CameraMode.LAUNCH -> cameraDirector.updateSeated(padPosition, seatPosition, pull, dt)
            }
            GameState.FLIGHT -> cameraDirector.updateFlight(
                physics.position, physics.velocity, physics.speed(), dt
            )
            GameState.RESULTS -> cameraDirector.updateResults(focusPoint, time, dt)
        }
        camera.update()

        sceneRenderer.particles.begin()
        currentScene.update(dt, time, camera, sceneRenderer.particles)
    }

    private fun updateSeated(dt: Float, released: Boolean, cancelled: Boolean) {
        if (cancelled) {
            stretchActive = false
            pull = 0f
            audio.setStretch(0f)
            return
        }
        if (input.pointerDown) {
            stretchActive = true
            // Signed vertical drag lets the player increase or ease sling tension while held.
            pull = (pull + dragScratch[1] * 2.4f).coerceIn(0f, 1f)
            audio.setStretch(pull)
        } else if (stretchActive || released) {
            stretchActive = false
            audio.setStretch(0f)
            if (pull > 0.08f) {
                launchInternal(pull)
            } else {
                pull = 0f
            }
        }
    }

    private fun updateFlight(dt: Float, currentScene: WorldScene) {
        val stats = build.stats()
        val ground = currentScene.terrain.height(physics.position.x, physics.position.z)
        flightInput.pitch = steerPitch
        flightInput.yaw = steerYaw
        flightInput.roll = steerRoll
        physics.update(dt, stats, ground, flightInput)
        flightTime += dt

        val altitude = (physics.position.y - ground).coerceAtLeast(0f)
        if (altitude > maxAltitudeThisFlight) maxAltitudeThisFlight = altitude
        if (!profile.glacierCrevasseCrossed &&
            currentScene.terrain.isCrevasseZone(physics.position.x, physics.position.z)
        ) {
            profile.glacierCrevasseCrossed = true
        }

        audio.setAmbienceEnabled(true)
        audio.setFlight(
            physics.speedFactor(),
            (physics.propellerRpm / 2400f).coerceIn(0f, 1f),
            true
        )
        hud?.onDistance(physics.distance, physics.speed())

        if (physics.state != FlightState.FLYING) finishFlight(currentScene)
    }

    private fun launchInternal(stretch: Float) {
        val stats = build.stats()
        val speed = sling.launchSpeed(stretch, stats.mass)
        // Spawn the aircraft at the carriage's real drawn-back position so release
        // begins from the elastic's stored position rather than snapping forward.
        val releasePoint = slingMesh?.holdPoint(stretch) ?: SlingshotLayout.HOLD_POINT
        seatPosition.set(
            padPosition.x + releasePoint.x,
            padPosition.y + releasePoint.y,
            padPosition.z + releasePoint.z
        )
        physics.reset(seatPosition)
        val damperLevel = sling.levelOf(SlingUpgrade.HYDRAULIC_DAMPER)
        val damperFraction = damperLevel.toFloat() / SlingshotState.MAX_LEVEL
        val recoilTurbulence = 0.28f * (1f - damperFraction * 0.92f)
        physics.launch(
            speed,
            profile.launchAngleDegrees.coerceIn(20f, 55f),
            0f,
            recoilTurbulence
        )

        state = GameState.FLIGHT
        steerPitch = 0f
        steerYaw = 0f
        steerRoll = 0f
        flightTime = 0f
        pull = 0f
        maxAltitudeThisFlight = 0f
        lastLaunchStretch = stretch
        slingRecoilStretch = stretch.coerceIn(0f, 1f)
        slingRecoilTimer = SLING_RECOIL_DURATION
        val residualRecoil = 1f - damperFraction * 0.65f
        cameraDirector.addShake((0.45f + stretch * 0.5f) * residualRecoil)

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
        profile.recordFlight(
            result.distanceMeters,
            result.coins,
            maxAltitudeThisFlight,
            currentScene.definition.id
        )
        store.save(profile)

        lastResult = result
        lastFlightMaxAltitude = maxAltitudeThisFlight
        focusPoint.set(physics.position)
        state = GameState.RESULTS
        resultTimer = 0f
        audio.setFlight(0f, 0f, false)
        audio.setAmbienceEnabled(false)
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

    fun returnToLaunch() {
        physics.reset(seatPosition)
        pull = 0f
        stretchActive = false
        steerPitch = 0f
        steerYaw = 0f
        steerRoll = 0f
        resultTimer = 0f
        state = GameState.SEATED
        paused = false
        audio.setFlight(0f, 0f, false)
        audio.setAmbienceEnabled(false)
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
        val showSling = state == GameState.SEATED || slingRecoilTimer > 0f
        val slingDraw = if (state == GameState.SEATED) pull else recoilDraw()

        if (state == GameState.SEATED) {
            val hold = rig.holdPoint(pull)
            aircraftModel.setTrs(
                padPosition.x + hold.x,
                padPosition.y + hold.y,
                padPosition.z + hold.z,
                0.10f * pull, 0f, 0f,
                1.22f
            )
            propellerModel.set(aircraftModel)
            propellerModel.translate(0f, 0f, -1.62f)
        } else {
            aircraftModel.set(physics.orientation).scale(1.22f)
            physics.propellerMatrix(propellerModel)
            propellerModel.scale(1.22f)
        }
        updatePropellerBladeModels(physics.propellerBladePitch)

        camera.update()
        sceneRenderer.beginFrame(camera)

        val shadowFocus = if (state == GameState.SEATED) seatPosition else physics.position
        sceneRenderer.beginShadowPass(shadowFocus, quality.shadowRadius)
        aircraftMesh.bodyMesh?.let { sceneRenderer.shadowMesh(it, aircraftModel) }
        aircraftMesh.propellerMesh?.let { sceneRenderer.shadowMesh(it, propellerModel) }
        aircraftMesh.propellerBladeMesh?.let { blade ->
            propellerBladeModels.forEach { sceneRenderer.shadowMesh(blade, it) }
        }
        if (showSling) {
            sceneRenderer.shadowMesh(rig.frameMesh, slingshotModel)
            setHydraulicModels(rig, slingDraw)
            sceneRenderer.shadowMesh(rig.hydraulicRodMesh, hydraulicLeftModel)
            sceneRenderer.shadowMesh(rig.hydraulicRodMesh, hydraulicRightModel)
            val cradle = rig.holdPoint(slingDraw)
            cradleModel.identity().translate(padPosition.x + cradle.x, padPosition.y + cradle.y - 0.20f, padPosition.z + cradle.z)
            sceneRenderer.shadowMesh(rig.cradleMesh, cradleModel)
        }
        sceneRenderer.shadowInstanced(currentScene.treeTrunks)
        sceneRenderer.shadowInstanced(currentScene.bushes)
        sceneRenderer.shadowInstanced(currentScene.bushesVariantB)
        sceneRenderer.shadowInstanced(currentScene.bushesVariantC)
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
        sceneRenderer.drawInstanced(currentScene.bushes, currentScene.bushMaterial)
        sceneRenderer.drawInstanced(currentScene.bushesVariantB, currentScene.bushMaterial)
        sceneRenderer.drawInstanced(currentScene.bushesVariantC, currentScene.bushMaterial)
        currentScene.grass?.let { sceneRenderer.drawInstanced(it, currentScene.grassMaterial) }
        sceneRenderer.drawInstanced(currentScene.birdsMesh, currentScene.birdMaterial)
        sceneRenderer.drawInstanced(currentScene.animalsMesh, currentScene.animalMaterial)

        currentScene.waterMesh?.let { water ->
            sceneRenderer.drawWater(
                water,
                textureFactory.waterNormals,
                currentScene.waterDeep,
                currentScene.waterShallow,
                settings.waterReflections
            )
        }

        if (showSling) {
            sceneRenderer.drawMesh(rig.frameMesh, rig.frameMaterial, slingshotModel)
            setHydraulicModels(rig, slingDraw)
            sceneRenderer.drawMesh(rig.hydraulicRodMesh, rig.hydraulicMaterial, hydraulicLeftModel)
            sceneRenderer.drawMesh(rig.hydraulicRodMesh, rig.hydraulicMaterial, hydraulicRightModel)
            val cradle = rig.holdPoint(slingDraw)
            cradleModel.identity().translate(padPosition.x + cradle.x, padPosition.y + cradle.y - 0.20f, padPosition.z + cradle.z)
            sceneRenderer.drawMesh(rig.cradleMesh, rig.cradleMaterial, cradleModel)

            rig.thicknessBoost = slingStateBandBoost()
            applyBandTint(rig, slingDraw)
            rig.leftBandMatrix(slingDraw, bandLocal)
            leftBand.setProduct(slingshotModel, bandLocal)
            sceneRenderer.drawMesh(rig.bandMesh, rig.bandMaterial, leftBand)

            rig.rightBandMatrix(slingDraw, bandLocal)
            rightBand.setProduct(slingshotModel, bandLocal)
            sceneRenderer.drawMesh(rig.bandMesh, rig.bandMaterial, rightBand)
        }

        aircraftMesh.bodyMesh?.let {
            sceneRenderer.drawMesh(it, aircraftMesh.bodyMaterial, aircraftModel)
        }
        aircraftMesh.canopyMesh?.let {
            sceneRenderer.drawMesh(it, aircraftMesh.canopyMaterial, aircraftModel)
        }
        aircraftMesh.propellerMesh?.let {
            sceneRenderer.drawMesh(it, aircraftMesh.propellerMaterial, propellerModel)
        }
        aircraftMesh.propellerBladeMesh?.let { blade ->
            propellerBladeModels.forEach { sceneRenderer.drawMesh(blade, aircraftMesh.propellerMaterial, it) }
        }
        val propellerBlur = ((physics.propellerRpm - 850f) / 1700f).coerceIn(0f, 0.34f)
        if (state == GameState.FLIGHT && propellerBlur > 0f) {
            aircraftMesh.propellerBlurMesh?.let {
                sceneRenderer.drawPropellerBlur(it, propellerModel, propellerBlur)
            }
        }

        val particleColor = when (definition.id) {
            WorldId.FOREST -> particleColorForest
            WorldId.DESERT -> particleColorDust
            WorldId.SNOW -> particleColorSnow
            WorldId.OCEAN -> particleColorSeaSpray
            WorldId.MOUNTAIN -> particleColorMountain
        }
        sceneRenderer.flushParticles(particleColor)
    }


    /** Repositions each blade around the shaft and changes its pitch about its span axis. */
    private fun updatePropellerBladeModels(pitch: Float) {
        for (index in 0 until propellerBladeModels.size) {
            propellerBladeModels[index]
                .set(propellerModel)
                .rotateZ(index * 2.0943951f)
                .rotateY(pitch)
        }
    }

    /** Exposed piston length visibly retracts as the elastic carriage is drawn back. */
    private fun recoilDraw(): Float {
        if (slingRecoilTimer <= 0f) return 0f
        val remaining = (slingRecoilTimer / SLING_RECOIL_DURATION).coerceIn(0f, 1f)
        // The carriage returns quickly at first, then settles smoothly into its stop.
        return slingRecoilStretch * remaining * remaining
    }

    private fun setHydraulicModels(rig: SlingshotMesh, draw: Float = pull) {
        val exposed = 0.58f - draw.coerceIn(0f, 1f) * 0.34f
        val centerY = padPosition.y + 1.56f + exposed * 0.5f
        val z = padPosition.z + 0.80f
        hydraulicLeftModel.setTrs(padPosition.x - 1.48f, centerY, z, 0f, 0f, 0f, 1f, exposed, 1f)
        hydraulicRightModel.setTrs(padPosition.x + 1.48f, centerY, z, 0f, 0f, 0f, 1f, exposed, 1f)
    }

    private fun slingStateBandBoost(): Float = (sling.bandThickness() - 1f).coerceIn(0f, 1f)

    /** Amber at rest → neon cyan at full draw, exactly as specified for the draw state. */
    private fun applyBandTint(rig: SlingshotMesh, stretch: Float) {
        val t = stretch.coerceIn(0f, 1f)
        bandTintScratch.set(bandTintAmber)
        bandTintScratch.x += (bandTintCyan.x - bandTintAmber.x) * t
        bandTintScratch.y += (bandTintCyan.y - bandTintAmber.y) * t
        bandTintScratch.z += (bandTintCyan.z - bandTintAmber.z) * t
        rig.bandMaterial.tint.set(bandTintScratch)
    }

    // ---- disposal ---------------------------------------------------------

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
        const val DESIGN_REFERENCE_HEIGHT = DesignTokens.DESIGN_HEIGHT
        private const val SLING_RECOIL_DURATION = 0.42f
    }
}
