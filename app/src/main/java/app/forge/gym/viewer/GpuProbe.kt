package app.forge.gym.viewer

import android.opengl.EGL14
import android.opengl.GLES20

/** Reads the GPU name once, so the 3D viewer can step aside on software renderers that can't run it. */
object GpuProbe {
    val renderer: String by lazy {
        runCatching {
            val dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            EGL14.eglInitialize(dpy, IntArray(2), 0, IntArray(2), 1)
            val attrs = intArrayOf(EGL14.EGL_RENDERABLE_TYPE, 0x40, EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT, EGL14.EGL_NONE)
            val cfg = arrayOfNulls<android.opengl.EGLConfig>(1); val n = IntArray(1)
            EGL14.eglChooseConfig(dpy, attrs, 0, cfg, 0, 1, n, 0)
            val ctx = EGL14.eglCreateContext(dpy, cfg[0], EGL14.EGL_NO_CONTEXT, intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE), 0)
            val surf = EGL14.eglCreatePbufferSurface(dpy, cfg[0], intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0)
            EGL14.eglMakeCurrent(dpy, surf, surf, ctx)
            val r = (GLES20.glGetString(GLES20.GL_RENDERER) ?: "") + " | " + (GLES20.glGetString(GLES20.GL_VERSION) ?: "")
            EGL14.eglMakeCurrent(dpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(dpy, surf); EGL14.eglDestroyContext(dpy, ctx)
            r
        }.getOrDefault("")
    }

    /** The emulator's GLES translator flattens uniform blocks and cannot link skinned materials. */
    val supported: Boolean get() = !(renderer.contains("OpenGL ES Translator") && !renderer.contains("ANGLE")) && !renderer.contains("OpenGL ES 2.")

    /** Software renderers (emulators) can show the 3D screen, but not 3D inside scrolling pages. */
    val embedded: Boolean get() = supported && !renderer.contains("SwiftShader") && !renderer.contains("llvmpipe")
}
