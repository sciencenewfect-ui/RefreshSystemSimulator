
package com.refreshsystem.simulator

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D Human-Inspired Open System.
 * Conceptual research model, not medical anatomy.
 */
class HumanOpenSystem3DView(context: Context) : GLSurfaceView(context) {

    private val modelRenderer = HumanSystemRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(modelRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun updateSignals(
        camera: Boolean,
        microphone: Boolean,
        audio: Float,
        stability: Float
    ) {
        modelRenderer.updateSignals(camera, microphone, audio, stability)
    }

    private var lastX = 0f
    private var lastY = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY

                modelRenderer.rotateBy(dx * 0.55f, dy * 0.35f)

                lastX = event.x
                lastY = event.y
                return true
            }
        }
        return true
    }
}

private class HumanSystemRenderer : GLSurfaceView.Renderer {

    @Volatile private var cameraOn = false
    @Volatile private var micOn = false
    @Volatile private var soundLevel = 0f
    @Volatile private var stable = 0.5f
    @Volatile private var yaw = -12f
    @Volatile private var pitch = 4f

    private var program = 0
    private var vertexBuffer: FloatBuffer? = null
    private var vertexCount = 0

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)

    private var uMvp = 0
    private var uColor = 0
    private var uPulse = 0

    private var startTime = System.nanoTime()

    fun updateSignals(
        camera: Boolean,
        microphone: Boolean,
        audio: Float,
        stability: Float
    ) {
        cameraOn = camera
        micOn = microphone
        soundLevel = audio.coerceIn(0f, 1f)
        stable = stability.coerceIn(0f, 1f)
    }

    fun rotateBy(dx: Float, dy: Float) {
        yaw = (yaw + dx) % 360f
        pitch = (pitch + dy).coerceIn(-35f, 35f)
    }

    override fun onSurfaceCreated(
        gl: javax.microedition.khronos.opengles.GL10?,
        config: javax.microedition.khronos.egl.EGLConfig?
    ) {
        GLES20.glClearColor(0.035f, 0.055f, 0.09f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(
            GLES20.GL_SRC_ALPHA,
            GLES20.GL_ONE_MINUS_SRC_ALPHA
        )

        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)

        uMvp = GLES20.glGetUniformLocation(program, "uMVP")
        uColor = GLES20.glGetUniformLocation(program, "uColor")
        uPulse = GLES20.glGetUniformLocation(program, "uPulse")

        createSphere()
        startTime = System.nanoTime()
    }

    override fun onSurfaceChanged(
        gl: javax.microedition.khronos.opengles.GL10?,
        width: Int,
        height: Int
    ) {
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.coerceAtLeast(1)

        Matrix.frustumM(
            projection, 0,
            -ratio, ratio,
            -1f, 1f,
            2f, 18f
        )
    }

    override fun onDrawFrame(
        gl: javax.microedition.khronos.opengles.GL10?
    ) {
        GLES20.glClear(
            GLES20.GL_COLOR_BUFFER_BIT or
                GLES20.GL_DEPTH_BUFFER_BIT
        )

        GLES20.glUseProgram(program)

        Matrix.setLookAtM(
            view, 0,
            0f, 0f, 8.2f,
            0f, 0f, 0f,
            0f, 1f, 0f
        )

        val elapsed =
            (System.nanoTime() - startTime) / 1_000_000_000f

        val pulse = 0.55f + 0.45f * sin(elapsed * 2.4f)

        GLES20.glUniform1f(uPulse, pulse)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        val positionHandle =
            GLES20.glGetAttribLocation(program, "aPosition")

        GLES20.glEnableVertexAttribArray(positionHandle)

        val vertices = vertexBuffer ?: return

        vertices.position(0)

        GLES20.glVertexAttribPointer(
            positionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            12,
            vertices
        )

        // Head
        sphere(
            0f, 2.05f, 0f,
            0.48f, 0.58f, 0.43f,
            0.24f, 0.39f, 0.56f
        )

        // Neck
        sphere(
            0f, 1.48f, 0f,
            0.22f, 0.32f, 0.22f,
            0.24f, 0.39f, 0.56f
        )

        // Torso
        sphere(
            0f, 0.55f, 0f,
            0.82f, 1.05f, 0.43f,
            0.13f, 0.27f, 0.42f
        )

        // Pelvis
        sphere(
            0f, -0.55f, 0f,
            0.62f, 0.42f, 0.38f,
            0.16f, 0.31f, 0.47f
        )

        // Arms
        limb(-0.72f, 1.05f, 0f, -1.22f, 0.05f, 0f,
            0.17f, 0.56f, 0.20f, 0.32f, 0.48f)

        limb(0.72f, 1.05f, 0f, 1.22f, 0.05f, 0f,
            0.17f, 0.56f, 0.20f, 0.32f, 0.48f)

        // Legs
        limb(-0.36f, -0.72f, 0f, -0.48f, -2.0f, 0f,
            0.22f, 0.75f, 0.20f, 0.31f, 0.46f)

        limb(0.36f, -0.72f, 0f, 0.48f, -2.0f, 0f,
            0.22f, 0.75f, 0.20f, 0.31f, 0.46f)

        // Brain / Processing Core
        sphere(
            0f, 2.12f, 0.30f,
            0.27f, 0.22f, 0.18f,
            if (cameraOn || micOn) 1f else 0.38f,
            0.35f, 0.78f
        )

        // Camera / Eyes
        sphere(
            -0.22f, 2.08f, 0.36f,
            0.07f, 0.06f, 0.06f,
            if (cameraOn) 1f else 0.25f,
            0.88f, 0.96f
        )

        sphere(
            0.22f, 2.08f, 0.36f,
            0.07f, 0.06f, 0.06f,
            if (cameraOn) 1f else 0.25f,
            0.88f, 0.96f
        )

        // Microphone / Ears
        sphere(
            -0.48f, 2.02f, 0f,
            0.09f, 0.12f, 0.09f,
            if (micOn) 1f else 0.25f,
            0.88f, 0.96f
        )

        sphere(
            0.48f, 2.02f, 0f,
            0.09f, 0.12f, 0.09f,
            if (micOn) 1f else 0.25f,
            0.88f, 0.96f
        )

        // Lungs
        sphere(
            -0.30f, 0.63f, 0.39f,
            0.27f, 0.43f, 0.13f,
            0.12f, 0.72f, 0.84f
        )

        sphere(
            0.30f, 0.63f, 0.39f,
            0.27f, 0.43f, 0.13f,
            0.12f, 0.72f, 0.84f
        )

        // Heart: pulse changes with microphone signal
        sphere(
            -0.04f, 0.36f, 0.50f,
            0.19f + soundLevel * 0.05f,
            0.22f, 0.16f,
            0.98f, 0.25f, 0.34f
        )

        // Memory Region
        sphere(
            0.12f, -0.28f, 0.38f,
            0.28f, 0.22f, 0.15f,
            0.68f, 0.43f, 0.98f
        )

        // Neural pathway / spine
        limb(
            0f, 1.68f, 0.25f,
            0f, -0.45f, 0.28f,
            0.045f, 1.08f, 0.045f,
            0.22f, 0.82f
        )

        // Moving Refresh Particles
        for (i in 0..5) {

            val t = (elapsed * 0.42f + i / 6f) % 1f

            sphere(
                0f, 1.58f - t * 2.0f, 0.34f,
                0.075f, 0.075f, 0.075f,
                0.25f, 0.88f, 0.96f
            )

            val sideT = (elapsed * 0.32f + i / 6f) % 1f

            sphere(
                -0.68f + sideT * 0.58f,
                1.0f - sideT * 0.62f,
                0.25f,
                0.045f, 0.045f, 0.045f,
                0.98f, 0.36f, 0.43f
            )
        }

        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun limb(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        radius: Float, length: Float, depth: Float,
        r: Float, g: Float
    ) {
        val cx = (x1 + x2) / 2f
        val cy = (y1 + y2) / 2f
        val cz = (z1 + z2) / 2f

        val dx = x2 - x1
        val dy = y2 - y1

        val angle = Math.toDegrees(
            kotlin.math.atan2(dx, dy).toDouble()
        ).toFloat()

        sphereTransform(
            cx, cy, cz,
            radius, length, depth,
            angle, r, g, 0.48f
        )
    }

    private fun sphere(
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float
    ) {
        sphereTransform(
            x, y, z,
            sx, sy, sz,
            0f, r, g, b
        )
    }

    private fun sphereTransform(
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        rotation: Float,
        red: Float, green: Float, blue: Float
    ) {
        drawEllipsoid(
            x, y, z,
            sx, sy, sz,
            rotation,
            red, green, blue
        )
    }

    private fun drawEllipsoid(
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        rotation: Float,
        red: Float, green: Float, blue: Float
    ) {
        Matrix.setIdentityM(model, 0)

        Matrix.rotateM(model, 0, yaw, 0f, 1f, 0f)
        Matrix.rotateM(model, 0, pitch, 1f, 0f, 0f)

        Matrix.translateM(model, 0, x, y, z)

        if (rotation != 0f) {
            Matrix.rotateM(model, 0, rotation, 0f, 0f, 1f)
        }

        Matrix.scaleM(model, 0, sx, sy, sz)

        val mv = FloatArray(16)

        Matrix.multiplyMM(mv, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, projection, 0, mv, 0)

        GLES20.glUniformMatrix4fv(
            uMvp, 1, false, mvp, 0
        )

        GLES20.glUniform4f(
            uColor, red, green, blue, 1f
        )

        GLES20.glDrawArrays(
            GLES20.GL_TRIANGLES, 0, vertexCount
        )
    }

    private fun createSphere() {

        val data = ArrayList<Float>()

        val lat = 16
        val lon = 20

        for (i in 0 until lat) {

            val a0 = Math.PI * (-0.5 + i.toDouble() / lat)
            val a1 = Math.PI * (-0.5 + (i + 1).toDouble() / lat)

            for (j in 0 until lon) {

                val b0 = 2.0 * Math.PI * j / lon
                val b1 = 2.0 * Math.PI * (j + 1) / lon

                val p0 = point(a0, b0)
                val p1 = point(a1, b0)
                val p2 = point(a1, b1)
                val p3 = point(a0, b1)

                data.addAll(p0)
                data.addAll(p1)
                data.addAll(p2)

                data.addAll(p0)
                data.addAll(p2)
                data.addAll(p3)
            }
        }

        vertexCount = data.size / 3

        val bytes = ByteBuffer
            .allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())

        vertexBuffer = bytes.asFloatBuffer().apply {
            put(data.toFloatArray())
            position(0)
        }
    }

    private fun point(a: Double, b: Double) = listOf(
        (cos(a) * cos(b)).toFloat(),
        sin(a).toFloat(),
        (cos(a) * sin(b)).toFloat()
    )

    private fun createProgram(
        vertex: String,
        fragment: String
    ): Int {

        fun compile(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)

            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)

            return shader
        }

        val vs = compile(GLES20.GL_VERTEX_SHADER, vertex)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, fragment)

        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs)
            GLES20.glAttachShader(it, fs)
            GLES20.glLinkProgram(it)
        }
    }

    companion object {

        private const val VERTEX_SHADER = """
            uniform mat4 uMVP;
            attribute vec3 aPosition;
            varying vec3 vNormal;

            void main() {
                vNormal = normalize(aPosition);
                gl_Position = uMVP * vec4(aPosition, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;

            uniform vec4 uColor;
            uniform float uPulse;
            varying vec3 vNormal;

            void main() {
                float light = 0.42 + 0.58 *
                    max(
                        dot(
                            normalize(vNormal),
                            normalize(vec3(0.25,0.65,1.0))
                        ),
                        0.0
                    );

                vec3 color = uColor.rgb * light *
                    (0.92 + 0.08 * uPulse);

                gl_FragColor = vec4(color, uColor.a);
            }
        """
    }
}
