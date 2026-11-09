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
import com.buildlaunchfly.game.audio.SoundBank
import com.buildlaunchfly.game.gfx.Camera
import com.buildlaunchfly.game.gfx.DesignTokens
import com.buildlaunchfly.game.gfx.ParticleRenderer
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
import com.buildlaunchfly.game.world.HangarMesh
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

    val audio = AudioEngine(SoundBank(context))

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
    private var hangar: HangarMesh? = null

    private val camera = Camera()
    private val cameraDirector = CameraDirector(camera)
    private val physics = FlightPhysics()

    private var state = GameState.SEATED
    private var initialized = false

    /**
     * Set when building the GL resources threw. The splash shows it instead of the app dying
     * silently on a device we cannot debug: the loading screen stays up with the failure on it.
     */
    @Volatile
    private var startupError: String? = null

    /** 0..1 start-up progress, shown by the splash screen while the engine warms up. */
    @Volatile
    private var loadProgressValue = 0f

    private var time = 0f
    private var pull = 0f
    private var stretchActive = false
    private var steerPitch = 0f
    private var steerRoll = 0f
    private var flightTime = 0f
    private var resultTimer = 0f
    private var lastResult: FlightResult? = null
    private var maxAltitudeThisFlight = 0f
    private var lastFlightMaxAltitude = 0f

    private var slingRecoilTimer = 0f
    private var slingRecoilStretch = 0f
    private var launchPuffTimer = 0f
    private var perfectFlashTimer = 0f
    private var hudAirspeedKmh = 0f

    private var viewportWidth = 1
    private var viewportHeight = 1

    private val actions = ConcurrentLinkedQueue<Runnable>()

    private val aircraftModel = Mat4()
    private val propellerModel = Mat4()
    private val slingshotModel = Mat4()

    /** One matrix per band segment: the sling is a single elastic drawn in pieces. */
    private val bandLocal = Array(SlingshotMesh.BAND_SEGMENTS) { Mat4() }
    private val bandWorld = Array(SlingshotMesh.BAND_SEGMENTS) { Mat4() }

    private val seatPosition = Vec3()
    private val padPosition = Vec3()
    private val focusPoint = Vec3()
    private val dragScratch = FloatArray(2)
    private val flightInput = FlightInput()
    private val particleColorSnow = Vec3(1f, 1f, 1f)
    private val particleColorDust = Vec3(0.95f, 0.84f, 0.66f)
    private val particleColorSmoke = Vec3(0.94f, 0.95f, 0.97f)
    private val bandTintScratch = Vec3(1f, 1f, 1f)

    /** Countdown for the one-shot first-flight "pull back" hint on the launch screen. */
    private var launchHintTimer = HINT_DURATION

    /** True while a UI modal is up; freezes simulation but keeps rendering. */
    @Volatile
    var paused: Boolean = false

    @Volatile
    var cameraMode: CameraMode = CameraMode.LAUNCH

    /** Extra yaw applied to workshop/menu orbits from UI drag. */
    @Volatile
    var orbitOffset: Float = 0f

    /** Camera orbit distance on the pad, adjusted by pinch zoom. */
    @Volatile
    var orbitRadius: Float = 9.5f

    /** Focus for the hangar view, at aircraft-on-the-floor height rather than in the band. */
    private val hangarFocus = Vec3()

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

    /** Non-null when the engine could not be built; shown on the splash screen. */
    fun startupError(): String? = startupError

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
            // Stay uninitialised when the build failed, so the splash keeps showing why.
            initialized = startupError == null
        } else {
            renderer?.resize(viewportWidth, viewportHeight)
        }
    }

    // ---- setup ------------------------------------------------------------

    /**
     * Builds every GL resource. If anything throws, the failure is reported on the splash
     * screen and [initialized] stays false, so [update] and [render] keep returning early
     * instead of the process dying without a trace.
     */
    private fun createResources(width: Int, height: Int) {
        startupError = null
        try {
            buildResources(width, height)
        } catch (error: Throwable) {
            Log.e(TAG, "Startup failed", error)
            startupError = error.javaClass.simpleName + ": " + (error.message ?: "no detail")
            initialized = false
        }
    }

    private fun buildResources(width: Int, height: Int) {
        val textureSize = if (settings.highResTextures) TextureFactory.SIZE else 128
        val textureFactory = TextureFactory(seed = 1337, size = textureSize, assets = context.assets)
        textures = textureFactory
        loadProgressValue = 0.35f

        val sceneRenderer = SceneRenderer(width, height, textureFactory.white)
        renderer = sceneRenderer
        val failedShaders = sceneRenderer.failedPrograms
        if (failedShaders.isNotEmpty()) {
            Log.e(TAG, "Shader programs failed to build: $failedShaders")
            // Losing the terrain program is survivable (it falls back to plain PBR); losing a
            // lit or depth program is not, so that is reported rather than drawn as garbage.
            if (failedShaders.any { it != "terrain" }) {
                startupError = "shaders: " + failedShaders.joinToString()
                return
            }
        }
        loadProgressValue = 0.45f

        aircraft = AircraftMesh(textureFactory, context.assets).also { it.updateFor(build) }
        slingMesh = SlingshotMesh(textureFactory)
        hangar = HangarMesh(textureFactory, context.assets)
        loadProgressValue = 0.55f

        rebuildScene(profile.selectedWorld)
        loadProgressValue = 0.85f

        val padY = scene?.terrain?.padHeight ?: 6f
        padPosition.set(0f, padY, 0f)
        seatPosition.set(
            padPosition.x + SlingshotLayout.HOLD_POINT.x,
            padPosition.y + SlingshotLayout.HOLD_POINT.y,
            padPosition.z + SlingshotLayout.HOLD_POINT.z
        )
        physics.reset(seatPosition)
        physics.visualScale = AIRCRAFT_VISUAL_SCALE

        cameraDirector.snapTo(
            Vec3(
                seatPosition.x,
                padPosition.y + CameraDirector.SEATED_HEIGHT,
                padPosition.z + CameraDirector.SEATED_DISTANCE
            ),
            seatPosition
        )

        applyAudioVolumes()
        audio.start()
        audio.setAmbience(scene?.definition?.ambience ?: AmbienceStyle.FOREST)
        hud?.onCoinsChanged(profile.coins)
        hud?.onStateChanged(state)
        loadProgressValue = 1f
        Log.i(TAG, "Engine ready\n${build.summary()}")
    }

    fun loadProgress(): Float = loadProgressValue

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
        scene = null
        val newScene = try {
            WorldScene(
                definition,
                textureFactory,
                quality.detailLevel,
                context.assets
            )
        } catch (error: Throwable) {
            Log.e(TAG, "Could not build the ${definition.id} world", error)
            startupError = "${definition.id}: " + (error.message ?: error.javaClass.simpleName)
            return
        }
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
            launchHintTimer = HINT_DURATION
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

    /** Pinch zoom: a larger finger spread (scale > 1) moves the orbit camera closer. */
    fun requestOrbitZoom(scale: Float) {
        if (scale <= 0.01f) return
        orbitRadius = (orbitRadius / scale).coerceIn(6f, 18f)
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

    /** Hangar INCOME card: buys the next income level. */
    fun requestUpgradeIncome() {
        post(Runnable {
            if (profile.upgradeIncome()) {
                store.save(profile)
                audio.playUi(AudioEngine.Cue.UPGRADE)
                hud?.onCoinsChanged(profile.coins)
            }
        })
    }

    /** Hangar chest: grants coins on a cooldown. */
    fun requestClaimChest() {
        post(Runnable {
            val reward = profile.claimChest(System.currentTimeMillis())
            if (reward > 0) {
                store.save(profile)
                audio.playUi(AudioEngine.Cue.REWARD)
                hud?.onCoinsChanged(profile.coins)
            }
        })
    }

    fun chestReady(): Boolean = profile.chestReady(System.currentTimeMillis())

    fun incomeMultiplier(): Float = profile.incomeMultiplier()

    /** Reference HUD shows a percentage toward a target distance; 4 000 m is the realm goal. */
    fun targetDistanceMeters(): Float = 4000f

    /** 0 when not a perfect launch, positive countdown just after one. */
    fun perfectFlash(): Float = perfectFlashTimer

    /** Seconds since the results debrief appeared, used for the reward count-up. */
    fun resultsElapsed(): Float = resultTimer

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
            stallWarning = state == GameState.FLIGHT && physics.stalled,
            verticalSpeed = physics.velocity.y,
            flightTimeSeconds = flightTime,
            stallAirspeedKmh = physics.stallSpeed(build.stats()) * 3.6f
        )
    }

    fun pullAmount(): Float = pull

    /** Smoothed airspeed the speedometer needle follows (about 200 ms of lag). */
    fun hudAirspeedKmh(): Float = hudAirspeedKmh

    /** Fade for the one-shot launch hint: 1 right after entering the rig, 0 once shown. */
    fun launchHintAlpha(): Float = (launchHintTimer / HINT_DURATION).coerceIn(0f, 1f)

    // ---- simulation -------------------------------------------------------

    fun update(dtSeconds: Float) {
        if (!initialized || startupError != null) return
        try {
            updateFrame(dtSeconds)
        } catch (error: Throwable) {
            Log.e(TAG, "Update failed", error)
            startupError = "update: " + (error.message ?: error.javaClass.simpleName)
        }
    }

    private fun updateFrame(dtSeconds: Float) {
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
        launchPuffTimer = (launchPuffTimer - dt).coerceAtLeast(0f)
        perfectFlashTimer = (perfectFlashTimer - dt).coerceAtLeast(0f)
        if (state == GameState.SEATED && pull < 0.05f) {
            launchHintTimer = (launchHintTimer - dt).coerceAtLeast(0f)
        }
        sceneRenderer.env.time = time

        input.stick(dragScratch)
        val released = input.consumeRelease()
        // Held virtual stick: pitch and roll follow the finger and recentre on release.
        val steerEase = 1f - kotlin.math.exp(-dt / 0.09f)
        steerRoll += (dragScratch[0] - steerRoll) * steerEase
        steerPitch += (-dragScratch[1] - steerPitch) * steerEase

        when (state) {
            GameState.SEATED -> updateSeated(dt, released)
            GameState.FLIGHT -> updateFlight(dt, currentScene)
            GameState.RESULTS -> {
                resultTimer += dt
            }
        }

        // The HUD needle lags the real airspeed by about 200 ms instead of snapping.
        val speedTarget = physics.speed() * 3.6f
        val speedAlpha = 1f - kotlin.math.exp(-dt / 0.20f)
        hudAirspeedKmh += (speedTarget - hudAirspeedKmh) * speedAlpha

        when (state) {
            GameState.SEATED -> when (cameraMode) {
                CameraMode.ORBIT_AIRCRAFT -> {
                    hangarFocus.set(
                        padPosition.x,
                        padPosition.y + HANGAR_CAMERA_HEIGHT,
                        padPosition.z
                    )
                    // Kept inside the shed: the plank wall is at half-width 5.3 m, so a wider
                    // orbit would put the camera outside and the wall between it and the plane.
                    cameraDirector.updateOrbitAircraft(
                        hangarFocus, time, dt, orbitRadius.coerceIn(3.5f, 5.0f), orbitOffset
                    )
                }
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
        if (launchPuffTimer > 0f) emitLaunchPuff(sceneRenderer.particles)
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
        flightInput.roll = steerRoll
        physics.update(dt, stats, ground, flightInput)
        flightTime += dt

        // The world's own solids — trunks, rooftops, ruins — end the flight the way the ground
        // does, so flying low over the timber is a real risk instead of a free ride.
        if (physics.state == FlightState.FLYING && currentScene.obstacles.blocks(
                physics.position.x,
                physics.position.y,
                physics.position.z,
                AIRCRAFT_COLLISION_RADIUS
            )
        ) {
            physics.impact()
            Log.i(
                TAG,
                "Flight ended on an obstacle at " +
                    "${"%.1f".format(physics.position.x)}, " +
                    "${"%.1f".format(physics.position.y)}, " +
                    "${"%.1f".format(physics.position.z)}"
            )
        }

        val altitude = (physics.position.y - ground).coerceAtLeast(0f)
        if (altitude > maxAltitudeThisFlight) maxAltitudeThisFlight = altitude

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
        seatPosition.set(
            padPosition.x + SlingshotLayout.HOLD_POINT.x,
            padPosition.y + SlingshotLayout.HOLD_POINT.y,
            padPosition.z + SlingshotLayout.HOLD_POINT.z
        )
        physics.reset(seatPosition)
        physics.visualScale = AIRCRAFT_VISUAL_SCALE
        physics.launch(speed, profile.launchAngleDegrees.coerceIn(20f, 55f), 0f)
        // The ramp lip adds a little upward push on top of the throw.
        physics.velocity.y += LAUNCH_RAMP_BOOST

        state = GameState.FLIGHT
        flightTime = 0f
        pull = 0f
        launchHintTimer = 0f
        maxAltitudeThisFlight = 0f
        lastLaunchStretch = stretch
        slingRecoilStretch = stretch.coerceIn(0f, 1f)
        slingRecoilTimer = SLING_RECOIL_DURATION
        launchPuffTimer = PUFF_DURATION
        perfectFlashTimer = if (stretch >= 0.55f && stretch <= 0.85f) 1.4f else 0f
        cameraDirector.addShake(0.45f + stretch * 0.5f)

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
            sling.powerRating(),
            profile.incomeMultiplier()
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
        resultTimer = 0f
        state = GameState.SEATED
        paused = false
        // Every retry is a fresh launch: back to the aim camera, rig slack, and any release the
        // player left on the results screen swallowed so a stray tap cannot fire the aircraft.
        cameraMode = CameraMode.LAUNCH
        orbitOffset = 0f
        slingRecoilTimer = 0f
        input.consumeRelease()
        audio.setFlight(0f, 0f, false)
        cameraDirector.snapTo(
            Vec3(
                seatPosition.x,
                padPosition.y + CameraDirector.SEATED_HEIGHT,
                padPosition.z + CameraDirector.SEATED_DISTANCE
            ),
            seatPosition
        )
        hud?.onStateChanged(state)
    }

    // ---- rendering --------------------------------------------------------

    fun render() {
        if (!initialized || startupError != null) return
        try {
            renderFrame()
        } catch (error: Throwable) {
            Log.e(TAG, "Render failed", error)
            startupError = "render: " + (error.message ?: error.javaClass.simpleName)
        }
    }

    private fun renderFrame() {
        if (!initialized) return
        val sceneRenderer = renderer ?: return
        val currentScene = scene ?: return
        val textureFactory = textures ?: return
        val aircraftMesh = aircraft ?: return
        val rig = slingMesh ?: return
        val definition = currentScene.definition
        val showSling = (state == GameState.SEATED && cameraMode != CameraMode.ORBIT_AIRCRAFT) ||
            slingRecoilTimer > 0f
        val showHangar = state == GameState.SEATED && cameraMode == CameraMode.ORBIT_AIRCRAFT
        val showHangarRoof = state == GameState.SEATED && cameraMode == CameraMode.LAUNCH
        val slingDraw = if (state == GameState.SEATED) pull else recoilDraw()

        if (showSling) {
            rig.drawLength = sling.drawLengthMeters()
            rig.thicknessBoost = slingStateBandBoost()
            // Ref-2: the plane parked in the hangar breathes on its cradle (±2 cm). The band
            // geometry is derived from the same offset, so the sling moves with it.
            rig.idleBob = if (pull < 0.02f && cameraMode == CameraMode.ORBIT_AIRCRAFT) {
                kotlin.math.sin(time * 1.6f) * 0.02f
            } else {
                0f
            }

            // The band is one continuous elastic strung between the two tips through the tow
            // point, so it is drawn as a chain of stretched segments.
            val sag = SlingshotMesh.BAND_SAG * (1f - slingDraw)
            rig.bandMatrices(slingDraw, sag, bandLocal)
            for (i in bandLocal.indices) {
                bandWorld[i].setProduct(slingshotModel, bandLocal[i])
            }
            applyBandTension(rig, slingDraw)
        }

        if (state == GameState.SEATED) {
            val hold = rig.holdPoint(pull)
            // The hangar shows the aircraft standing on the plank floor on its cradle; only the
            // launch setup hangs it up in the band.
            val parked = cameraMode == CameraMode.ORBIT_AIRCRAFT
            aircraftModel.setTrs(
                padPosition.x + hold.x,
                padPosition.y + if (parked) HANGAR_PARK_HEIGHT else hold.y,
                padPosition.z + hold.z,
                0.10f * pull, 0f, 0f,
                AIRCRAFT_VISUAL_SCALE
            )
            propellerModel.set(aircraftModel)
            propellerModel.translate(0f, 0f, -1.62f * AIRCRAFT_VISUAL_SCALE)
        } else {
            aircraftModel.set(physics.orientation)
            physics.propellerMatrix(propellerModel)
        }

        camera.update()
        sceneRenderer.beginFrame(camera)

        val shadowFocus = if (state == GameState.SEATED) seatPosition else physics.position
        sceneRenderer.beginShadowPass(shadowFocus, quality.shadowRadius)
        if (aircraftMesh.hasImportedModel) {
            for (part in aircraftMesh.importedParts) {
                sceneRenderer.shadowMesh(part.mesh, aircraftModel)
            }
        } else {
            aircraftMesh.canvasMesh?.let { sceneRenderer.shadowMesh(it, aircraftModel) }
            aircraftMesh.woodMesh?.let { sceneRenderer.shadowMesh(it, aircraftModel) }
            aircraftMesh.metalMesh?.let { sceneRenderer.shadowMesh(it, aircraftModel) }
            aircraftMesh.propellerMesh?.let { sceneRenderer.shadowMesh(it, propellerModel) }
        }
        if (showSling) {
            sceneRenderer.shadowMesh(rig.frameMesh, slingshotModel)
            for (band in bandWorld) sceneRenderer.shadowMesh(rig.bandMesh, band)
        }
        if (showHangar) {
            hangar?.let { set ->
                sceneRenderer.shadowMesh(set.wallMesh, slingshotModel)
                sceneRenderer.shadowMesh(set.roofMesh, slingshotModel)
                sceneRenderer.shadowMesh(set.floorMesh, slingshotModel)
                sceneRenderer.shadowMesh(set.metalMesh, slingshotModel)
                sceneRenderer.shadowMesh(set.rampMesh, slingshotModel)
                for (prop in set.props) sceneRenderer.shadowMesh(prop.mesh, prop.matrix)
            }
        }
        for (tree in currentScene.trees) {
            sceneRenderer.shadowInstanced(tree.trunk.instanced)
            sceneRenderer.shadowInstanced(tree.canopy.instanced)
        }
        for (part in currentScene.shrubs) sceneRenderer.shadowInstanced(part.instanced)
        for (part in currentScene.rockParts) sceneRenderer.shadowInstanced(part.instanced)
        for (tree in currentScene.heroTrees) {
            sceneRenderer.shadowInstanced(tree.trunk.instanced)
            sceneRenderer.shadowInstanced(tree.canopy.instanced)
        }
        for (part in currentScene.heroPlants) sceneRenderer.shadowInstanced(part.instanced)
        for (part in currentScene.animalParts) sceneRenderer.shadowInstanced(part.instanced)
        // The airfield buildings, town props and castle obstacles are solid objects standing
        // in the open; without this they floated over the ground with no shadow at all.
        for (part in currentScene.structures) sceneRenderer.shadowInstanced(part.instanced)
        sceneRenderer.endShadowPass()

        sceneRenderer.drawSky()
        sceneRenderer.drawTerrain(
            currentScene.terrainMesh,
            textureFactory.surface(definition.surfaceA),
            textureFactory.surface(definition.surfaceB),
            textureFactory.surface(definition.surfaceC),
            textureFactory.soil,
            definition.tintA,
            definition.tintB,
            definition.tintC,
            definition.rockThreshold,
            definition.snowLine
        )

        for (tree in currentScene.trees) {
            sceneRenderer.drawInstanced(tree.trunk.instanced, tree.trunk.material)
            sceneRenderer.drawInstanced(tree.canopy.instanced, tree.canopy.material)
        }
        for (part in currentScene.shrubs) {
            sceneRenderer.drawInstanced(part.instanced, part.material)
        }
        for (part in currentScene.rockParts) {
            sceneRenderer.drawInstanced(part.instanced, part.material)
        }
        for (part in currentScene.grassParts) {
            sceneRenderer.drawInstanced(part.instanced, part.material)
        }
        for (tree in currentScene.heroTrees) {
            sceneRenderer.drawInstanced(tree.trunk.instanced, tree.trunk.material)
            sceneRenderer.drawInstanced(tree.canopy.instanced, tree.canopy.material)
        }
        for (part in currentScene.heroPlants) {
            sceneRenderer.drawInstanced(part.instanced, part.material)
        }
        sceneRenderer.drawInstanced(currentScene.birdsMesh, currentScene.birdMaterial)
        if (currentScene.animalParts.isEmpty()) {
            sceneRenderer.drawInstanced(currentScene.animalsMesh, currentScene.animalMaterial)
        } else {
            for (part in currentScene.animalParts) {
                sceneRenderer.drawInstanced(part.instanced, part.material)
            }
        }

        if (showHangar) {
            hangar?.let { set ->
                sceneRenderer.drawMesh(set.wallMesh, set.wallMaterial, slingshotModel)
                sceneRenderer.drawMesh(set.roofMesh, set.roofMaterial, slingshotModel)
                sceneRenderer.drawMesh(set.floorMesh, set.floorMaterial, slingshotModel)
                sceneRenderer.drawMesh(set.metalMesh, set.metalMaterial, slingshotModel)
                sceneRenderer.drawMesh(set.rampMesh, set.rampMaterial, slingshotModel)
                for (prop in set.props) {
                    sceneRenderer.drawMesh(prop.mesh, prop.material, prop.matrix)
                }
            }
        } else if (showHangarRoof) {
            // The launch shot stands inside the shed: the plank wall, floor, roof, lamp and ramp
            // are all drawn, because the camera now sits under the eave (3.6 m) rather than on
            // top of the roof.
            hangar?.let { set ->
                sceneRenderer.drawMesh(set.wallMesh, set.wallMaterial, slingshotModel)
                sceneRenderer.drawMesh(set.roofMesh, set.roofMaterial, slingshotModel)
                sceneRenderer.drawMesh(set.floorMesh, set.floorMaterial, slingshotModel)
                sceneRenderer.drawMesh(set.metalMesh, set.metalMaterial, slingshotModel)
                sceneRenderer.drawMesh(set.rampMesh, set.floorMaterial, slingshotModel)
                for (prop in set.props) {
                    sceneRenderer.drawMesh(prop.mesh, prop.material, prop.matrix)
                }
            }
        }

        for (part in currentScene.structures) {
            sceneRenderer.drawInstanced(part.instanced, part.material)
        }

        for (part in currentScene.shipParts) {
            sceneRenderer.drawInstanced(part.instanced, part.material)
        }

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
            // The timber rig only. The elastic band's stretched strips are not drawn: they read
            // as a bright multi-coloured ribbon across the aircraft instead of a slingshot,
            // and the pull is shown by the aircraft itself moving back in the rig.
            sceneRenderer.drawMesh(rig.frameMesh, rig.frameMaterial, slingshotModel)
        }

        if (aircraftMesh.hasImportedModel) {
            // The imported biplane is one welded mesh with its own propeller and airframe,
            // so it replaces the built body, propeller included.
            for (part in aircraftMesh.importedParts) {
                sceneRenderer.drawMesh(part.mesh, part.material, aircraftModel)
            }
        } else {
            aircraftMesh.canvasMesh?.let {
                sceneRenderer.drawMesh(it, aircraftMesh.canvasMaterial, aircraftModel)
            }
            aircraftMesh.ribMesh?.let {
                sceneRenderer.drawMesh(it, aircraftMesh.ribMaterial, aircraftModel)
            }
            aircraftMesh.woodMesh?.let {
                sceneRenderer.drawMesh(it, aircraftMesh.woodMaterial, aircraftModel)
            }
            aircraftMesh.metalMesh?.let {
                sceneRenderer.drawMesh(it, aircraftMesh.metalMaterial, aircraftModel)
            }
            aircraftMesh.canopyMesh?.let {
                sceneRenderer.drawMesh(it, aircraftMesh.canopyMaterial, aircraftModel)
            }
            aircraftMesh.propellerMesh?.let {
                sceneRenderer.drawMesh(it, aircraftMesh.propellerMaterial, propellerModel)
            }
        }
        aircraftMesh.propellerBlurMesh?.let { blur ->
            val rpmFraction = (physics.propellerRpm / 2200f).coerceIn(0f, 1f)
            val alpha = ((rpmFraction - 0.30f) / 0.70f).coerceIn(0f, 1f) * 0.30f
            aircraftMesh.propellerBlurMaterial.alpha = alpha
            if (alpha > 0.01f) {
                sceneRenderer.drawMesh(blur, aircraftMesh.propellerBlurMaterial, propellerModel)
            }
        }
        if (showSling) {
            // Bands sit over the wing root in the launch plate, so draw them after the airframe.
            for (band in bandWorld) {
                sceneRenderer.drawMesh(rig.bandMesh, rig.bandMaterial, band)
            }
        }

        val particleColor = when {
            launchPuffTimer > 0f -> particleColorSmoke
            definition.snowfall > 0f -> particleColorSnow
            else -> particleColorDust
        }
        sceneRenderer.flushParticles(particleColor)
    }

    /** Damped elastic recoil that plays out on the bands after release. */
    private fun recoilDraw(): Float {
        if (slingRecoilTimer <= 0f) return 0f
        val t = (slingRecoilTimer / SLING_RECOIL_DURATION).coerceIn(0f, 1f)
        val wobble = 0.35f + 0.65f * kotlin.math.abs(kotlin.math.cos((1f - t) * 14f))
        return (slingRecoilStretch * t * wobble).coerceIn(0f, 1f)
    }

    /** Short-lived smoke puff at the cradle after release. */
    private fun emitLaunchPuff(particles: ParticleRenderer) {
        val remaining = (launchPuffTimer / PUFF_DURATION).coerceIn(0f, 1f)
        val spread = (1f - remaining) * 2.6f
        for (i in 0 until 5) {
            val angle = time * 6.5f + i * 1.7f
            val radius = spread * (0.3f + i * 0.14f)
            particles.emit(
                padPosition.x + kotlin.math.cos(angle) * radius,
                padPosition.y + SlingshotLayout.HOLD_POINT.y - 0.35f + (1f - remaining) * 2f,
                padPosition.z + kotlin.math.sin(angle) * radius,
                0.55f + (1f - remaining) * 0.5f
            )
        }
    }

    private fun slingStateBandBoost(): Float = (sling.bandThickness() - 1f).coerceIn(0f, 1f)

    /**
     * The bands keep their yellow/cream stripes; tension only lifts them from the shade of
     * slack rubber to full brightness.
     */
    private fun applyBandTension(rig: SlingshotMesh, stretch: Float) {
        val bright = 0.90f + 0.10f * stretch.coerceIn(0f, 1f)
        bandTintScratch.set(bright, bright, bright)
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
        hangar?.dispose()
        hangar = null
        renderer?.dispose()
        renderer = null
        textures?.disposeAll()
        textures = null
        initialized = false
    }

    companion object {
        private const val TAG = "BLF-GAME"
        private const val SLING_RECOIL_DURATION = 0.6f
        private const val PUFF_DURATION = 0.85f
        private const val HINT_DURATION = 3f

        /** The aircraft is drawn larger than life so it reads as the hero of the launch shot. */
        const val AIRCRAFT_VISUAL_SCALE = 1.5f

        /** Upward push from the launch ramp lip, in m/s. */
        const val LAUNCH_RAMP_BOOST = 1.0f

        /**
         * Radius of the aircraft's collision disc, in metres: the fuselage plus a little, so a
         * wingtip clipping a trunk counts but squeezing between two trees does not.
         */
        const val AIRCRAFT_COLLISION_RADIUS = 1.5f

        /** Height the aircraft stands at on the hangar floor, in metres above the pad. */
        const val HANGAR_PARK_HEIGHT = 0.95f

        /** Height the hangar camera is centred on: the parked aircraft, not the band. */
        const val HANGAR_CAMERA_HEIGHT = 1.7f

        const val DESIGN_REFERENCE_HEIGHT = DesignTokens.DESIGN_HEIGHT
    }
}
