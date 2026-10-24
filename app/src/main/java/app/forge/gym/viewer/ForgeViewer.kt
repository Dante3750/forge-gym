/*
 * Based on Filament's ModelViewer (com.google.android.filament.utils.ModelViewer),
 * Copyright (C) 2020 The Android Open Source Project, licensed under the Apache License, Version 2.0.
 * Changed for Forge: a shared, cached human model is driven by a separate small motion file,
 * with studio lighting, soft shadows and camera presets.
 */
package app.forge.gym.viewer

import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import com.google.android.filament.Colors
import com.google.android.filament.ColorGrading
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Fence
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Skybox
import com.google.android.filament.SwapChain
import com.google.android.filament.ToneMapper
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.GestureDetector
import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.Manipulator
import java.nio.Buffer

class ForgeViewer(private val textureView: TextureView, bg: Int, lite: Boolean = false) : android.view.View.OnTouchListener {
    val engine: Engine = Engine.create()
    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
    private val renderer: Renderer = engine.createRenderer()
    private val scene = engine.createScene()
    private val camera = engine.createCamera(engine.entityManager.create()).apply { setExposure(16f, 1f / 125f, 100f) }
    private val view: View = engine.createView()
    private val materials = UbershaderProvider(engine)
    private val assetLoader = AssetLoader(engine, materials, EntityManager.get())
    private val resourceLoader = ResourceLoader(engine, true)
    private val displayHelper = DisplayHelper(textureView.context)
    private var swapChain: SwapChain? = null
    private val lights = IntArray(3)
    private val readyRenderables = IntArray(128)

    private var curPreset = Preset.ANGLE
    private var focus = floatArrayOf(0f, 0.8f, 0f)
    private var dist = 4.1f

    private var manipulator: Manipulator = buildManipulator(Preset.FRONT, 1, 1)
    private var gestures = GestureDetector(textureView, manipulator)
    private var width = 1
    private var height = 1

    var model: FilamentAsset? = null; private set
    var motion: FilamentAsset? = null; private set
    private var jointPairs = IntArray(0)
    private val tmp = FloatArray(16)
    private var modelReady = false
    // The model's animator only exists once its resources have loaded; asking earlier caches an empty one.
    private var modelAnimator: com.google.android.filament.gltfio.Animator? = null
    private var readyFrames = 0

    enum class Preset { FRONT, ANGLE, SIDE, BACK }

    init {
        view.scene = scene
        view.camera = camera
        view.antiAliasing = View.AntiAliasing.FXAA
        view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply { enabled = true; sampleCount = 4 }
        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply { enabled = !lite; radius = 0.35f; intensity = 1.2f }
        view.setShadowType(View.ShadowType.DPCF)
        view.softShadowOptions = view.softShadowOptions.apply { penumbraScale = 2.5f; penumbraRatioScale = 1.5f }
        view.colorGrading = ColorGrading.Builder().toneMapper(ToneMapper.Filmic()).build(engine)
        val r = ((bg shr 16) and 0xFF) / 255f; val g = ((bg shr 8) and 0xFF) / 255f; val b = (bg and 0xFF) / 255f
        scene.skybox = Skybox.Builder().color(r * r, g * g, b * b, 1f).build(engine)
        // soft studio ambient
        scene.indirectLight = IndirectLight.Builder().irradiance(1, floatArrayOf(0.55f, 0.56f, 0.6f)).intensity(28_000f).build(engine)
        // key, fill and rim lights
        val dirs = arrayOf(floatArrayOf(0.5f, -0.75f, -0.45f), floatArrayOf(0.7f, -0.35f, -0.45f), floatArrayOf(0.25f, -0.4f, 0.9f))
        val power = floatArrayOf(95_000f, 32_000f, 70_000f)
        for (i in 0 until 3) {
            lights[i] = EntityManager.get().create()
            val (cr, cg, cb) = Colors.cct(if (i == 1) 7_200f else 5_800f)
            LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(cr, cg, cb).intensity(power[i]).direction(dirs[i][0], dirs[i][1], dirs[i][2])
                .castShadows(i == 0)
                .shadowOptions(LightManager.ShadowOptions().apply {
                    mapSize = if (lite) 1024 else 2048; shadowFar = 8f
                    screenSpaceContactShadows = true; stepCount = 8; maxShadowDistance = 0.25f
                })
                .build(engine, lights[i])
            scene.addEntity(lights[i])
        }
        uiHelper.renderCallback = SurfaceCallback()
        uiHelper.attachTo(textureView)
        textureView.addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: android.view.View) {}
            override fun onViewDetachedFromWindow(v: android.view.View) = destroy()
        })
    }

    /** Body and equipment cast soft shadows; the floor only receives them. */
    private fun shadows(a: FilamentAsset, cast: Boolean) {
        val rm = engine.renderableManager
        for (e in a.entities) {
            if (!rm.hasComponent(e)) continue
            val i = rm.getInstance(e)
            val floor = a.getName(e) == "floor"
            rm.setCastShadows(i, cast && !floor); rm.setReceiveShadows(i, true)
        }
    }

    /** Swaps the flat ambient for the studio light environment (soft light from every side + reflections). */
    fun setEnvironment(buffer: Buffer) {
        val ibl = runCatching { KTX1Loader.createIndirectLight(engine, buffer) }.getOrNull() ?: return
        ibl.intensity = 12_500f
        scene.indirectLight?.let { old -> scene.indirectLight = null; engine.destroyIndirectLight(old) }
        scene.indirectLight = ibl
        // the environment now does the fill: keep a softer key (for the floor shadow) and the rim
        val lm = engine.lightManager
        val power = floatArrayOf(110_000f, 0f, 55_000f)
        for (i in 0 until 3) if (lm.hasComponent(lights[i])) lm.setIntensity(lm.getInstance(lights[i]), power[i])
    }

    private fun buildManipulator(p: Preset, w: Int, h: Int): Manipulator {
        val d = when (p) {
            Preset.FRONT -> floatArrayOf(0f, 0.35f, 4.1f)
            Preset.ANGLE -> floatArrayOf(2.8f, 0.5f, 3.0f)
            Preset.SIDE -> floatArrayOf(4.1f, 0.3f, 0f)
            Preset.BACK -> floatArrayOf(0f, 0.4f, -4.1f)
        }
        val n = kotlin.math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2])
        // a tall portrait view needs more distance for a wide (lying) body, a wide view for a tall one
        val aspect = if (h > 0) w.toFloat() / h else 1f
        val k = dist * (if (aspect < 1f) 1f / aspect.coerceAtLeast(0.5f) * 0.85f + 0.15f else 1f)
        return Manipulator.Builder()
            .targetPosition(focus[0], focus[1], focus[2])
            .orbitHomePosition(focus[0] + d[0] / n * k, focus[1] + d[1] / n * k, focus[2] + d[2] / n * k)
            .zoomSpeed(0.06f)
            .orbitSpeed(0.006f, 0.006f)
            .viewport(w, h)
            .build(Manipulator.Mode.ORBIT)
    }

    fun setPreset(p: Preset) {
        curPreset = p
        manipulator = buildManipulator(p, width, height)
        gestures = GestureDetector(textureView, manipulator)
    }

    /** Frames the camera on the space the body moves through over the whole loop. */
    private fun frameMotion(a: FilamentAsset) {
        val an = a.instance.animator
        val tm = engine.transformManager
        val names = listOf("root", "head", "foot.L", "foot.R", "wrist.L", "wrist.R", "lowerleg01.L", "lowerleg01.R")
        val ents = names.map { a.getFirstEntityByName(it) }.filter { it != 0 }
        if (ents.isEmpty() || an.animationCount == 0) return
        val lo = floatArrayOf(1e9f, 1e9f, 1e9f); val hi = floatArrayOf(-1e9f, -1e9f, -1e9f)
        val m = FloatArray(16)
        var dur = 0f
        for (i in 0 until an.animationCount) dur = maxOf(dur, an.getAnimationDuration(i))
        for (step in 0 until 12) {
            val t = dur * step / 12f
            for (i in 0 until an.animationCount) an.applyAnimation(i, t)
            for (e in ents) {
                tm.getWorldTransform(tm.getInstance(e), m)
                for (c in 0..2) { lo[c] = minOf(lo[c], m[12 + c]); hi[c] = maxOf(hi[c], m[12 + c]) }
            }
        }
        lo[1] = minOf(lo[1], 0f); hi[1] += 0.2f // floor and the top of the head
        focus = floatArrayOf((lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2, (lo[2] + hi[2]) / 2)
        val ext = maxOf(hi[1] - lo[1], maxOf(hi[0] - lo[0], hi[2] - lo[2]) * 0.9f)
        dist = (ext * 2.05f + 0.45f).coerceIn(2.2f, 5.5f)
        setPreset(curPreset)
    }

    /** The shared human (rest pose, skinned). */
    fun setModel(buffer: Buffer) {
        model?.let { scene.removeEntities(it.entities); assetLoader.destroyAsset(it) }
        modelReady = false
        modelAnimator = null
        drawn = 0
        model = assetLoader.createAsset(buffer)?.also { a ->
            resourceLoader.asyncBeginLoad(a)
            a.releaseSourceData()
        }
        pairJoints()
    }

    /** One exercise: skeleton motion and equipment. */
    fun setMotion(buffer: Buffer) {
        motion?.let { scene.removeEntities(it.entities); assetLoader.destroyAsset(it) }
        motion = assetLoader.createAsset(buffer)?.also { a ->
            resourceLoader.loadResources(a)
            a.releaseSourceData()
            scene.addEntities(a.entities)
            shadows(a, cast = true)
            frameMotion(a)
        }
        pairJoints()
    }

    private fun pairJoints() {
        val m = model ?: return
        val a = motion ?: return
        val tm = engine.transformManager
        val out = ArrayList<Int>()
        for (e in m.entities) {
            val n = m.getName(e) ?: continue
            val src = a.getFirstEntityByName(n)
            if (src != 0 && tm.hasComponent(src) && tm.hasComponent(e)) { out += tm.getInstance(src); out += tm.getInstance(e) }
        }
        jointPairs = out.toIntArray()
        android.util.Log.i("ForgeViewer", "pairJoints model=${m.entities.size} motion=${a.entities.size} pairs=${out.size / 2} rootM=${m.getFirstEntityByName("root")} rootA=${a.getFirstEntityByName("root")} anims=${a.instance.animator.animationCount}")
    }
    private var dbg = 0

    /** True once the human is on screen and moving. */
    val showing: Boolean get() = drawn > 2
    private var drawn = 0

    val loadProgress: Float get() = if (model == null) 0f else resourceLoader.asyncGetLoadProgress()

    /** Plays the motion at [seconds] and draws a frame. */
    fun render(frameTimeNanos: Long, seconds: Double) {
        if (!uiHelper.isReadyToRender) return
        resourceLoader.asyncUpdateLoad()
        model?.let { m ->
            var count: Int
            while (m.popRenderables(readyRenderables).also { count = it } != 0) scene.addEntities(readyRenderables.copyOf(count))
            if (!modelReady && resourceLoader.asyncGetLoadProgress() >= 1f) {
                resourceLoader.asyncUpdateLoad()
                modelReady = true; scene.addEntities(m.entities); readyFrames = 0
                shadows(m, cast = true)
            }
            // a few frames after loading finished, the animator is guaranteed to exist
            if (modelReady && modelAnimator == null && ++readyFrames > 3) modelAnimator = m.instance.animator
        }
        motion?.let { a ->
            val an = a.instance.animator
            for (i in 0 until an.animationCount) {
                val d = an.getAnimationDuration(i).toDouble().coerceAtLeast(0.01)
                an.applyAnimation(i, (seconds % d).toFloat())
            }
            val tm = engine.transformManager
            var k = 0
            while (k < jointPairs.size) { tm.getTransform(jointPairs[k], tmp); tm.setTransform(jointPairs[k + 1], tmp); k += 2 }
            modelAnimator?.updateBoneMatrices()
            if (dbg++ % 90 == 0) {
                val m = model
                val ra = a.getFirstEntityByName("lowerleg01.L"); val rm = m?.getFirstEntityByName("lowerleg01.L") ?: 0
                val ta = FloatArray(16); val tb = FloatArray(16)
                if (ra != 0) tm.getTransform(tm.getInstance(ra), ta)
                if (rm != 0) tm.getTransform(tm.getInstance(rm), tb)
                android.util.Log.i("ForgeViewer", "t=%.2f pairs=%d kneeA=%.3f,%.3f,%.3f kneeM=%.3f,%.3f,%.3f ready=%s".format(seconds, jointPairs.size / 2, ta[4], ta[5], ta[6], tb[4], tb[5], tb[6], modelReady))
            }
        }
        val eye = DoubleArray(3); val tgt = DoubleArray(3); val up = DoubleArray(3)
        manipulator.getLookAt(eye, tgt, up)
        camera.lookAt(eye[0], eye[1], eye[2], tgt[0], tgt[1], tgt[2], up[0], up[1], up[2])
        val sc = swapChain ?: return
        if (renderer.beginFrame(sc, frameTimeNanos)) {
            renderer.render(view); renderer.endFrame()
            if (modelAnimator != null && motion != null) drawn++
        }
    }

    override fun onTouch(v: android.view.View, event: MotionEvent): Boolean { gestures.onTouchEvent(event); return true }

    private var destroyed = false
    fun destroy() {
        if (destroyed) return
        destroyed = true
        uiHelper.detach()
        motion?.let { scene.removeEntities(it.entities); assetLoader.destroyAsset(it) }
        model?.let { scene.removeEntities(it.entities); assetLoader.destroyAsset(it) }
        resourceLoader.asyncCancelLoad(); resourceLoader.evictResourceData()
        assetLoader.destroy(); materials.destroyMaterials(); materials.destroy(); resourceLoader.destroy()
        for (l in lights) { engine.destroyEntity(l); EntityManager.get().destroy(l) }
        engine.destroyRenderer(renderer); engine.destroyView(view); engine.destroyScene(scene)
        engine.destroyCameraComponent(camera.entity); EntityManager.get().destroy(camera.entity)
        engine.destroy()
    }

    private fun updateProjection() {
        camera.setLensProjection(40.0, width.toDouble() / height.coerceAtLeast(1), 0.05, 100.0)
    }

    inner class SurfaceCallback : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
            swapChain?.let { engine.destroySwapChain(it) }
            swapChain = engine.createSwapChain(surface)
            textureView.display?.let { displayHelper.attach(renderer, it) }
        }
        override fun onDetachedFromSurface() {
            displayHelper.detach()
            swapChain?.let { engine.destroySwapChain(it); engine.flushAndWait(); swapChain = null }
        }
        override fun onResized(w: Int, h: Int) {
            width = w; height = h
            view.viewport = Viewport(0, 0, w, h)
            setPreset(curPreset)
            updateProjection()
            val fence = engine.createFence(); fence.wait(Fence.Mode.FLUSH, Fence.WAIT_FOR_EVER); engine.destroyFence(fence)
        }
    }
}
