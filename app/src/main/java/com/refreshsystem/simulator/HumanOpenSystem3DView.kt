
package com.refreshsystem.simulator

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Professional Human-Inspired Open System 3D Model.
 *
 * A conceptual research visualization.
 * It is not a medical or anatomical diagnostic model.
 */
class HumanOpenSystem3DView(context: Context) : GLSurfaceView(context) {

    private val humanRenderer = HumanSystemRenderer()

    private var previousX = 0f
    private var previousY = 0f

    init {
        setEGLContextClientVersion(2)
        setRenderer(humanRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun updateSignals(
        camera: Boolean,
        microphone: Boolean,
        audio: Float,
        stability: Float
    ) {
        humanRenderer.updateSignals(
            camera,
            microphone,
            audio,
            stability
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {

            MotionEvent.ACTION_DOWN -> {
                previousX = event.x
                previousY = event.y
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - previousX
                val dy = event.y - previousY

                humanRenderer.rotateBy(
                    dx * 0.45f,
                    dy * 0.35f
                )

                previousX = event.x
                previousY = event.y
                return true
            }
        }

        return true
    }
}

private class HumanSystemRenderer : GLSurfaceView.Renderer {

    @Volatile private var cameraOn = false
    @Volatile private var microphoneOn = false
    @Volatile private var audioLevel = 0f
    @Volatile private var dynamicStability = 0.70f

    @Volatile private var yaw = -8f
    @Volatile private var pitch = 3f

    private var shaderProgram = 0
    private var vertexBuffer: FloatBuffer? = null
    private var vertexCount = 0

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val modelView = FloatArray(16)
    private val mvp = FloatArray(16)

    private var uMvp = 0
    private var uColor = 0
    private var uPulse = 0

    private var startTime = System.nanoTime()

    // Human model color palette

    private val skin = floatArrayOf(0.72f, 0.46f, 0.31f)
    private val skinLight = floatArrayOf(0.88f, 0.64f, 0.47f)
    private val skinDark = floatArrayOf(0.48f, 0.28f, 0.20f)

    private val shirt = floatArrayOf(0.10f, 0.22f, 0.36f)
    private val shirtLight = floatArrayOf(0.16f, 0.34f, 0.52f)

    private val pants = floatArrayOf(0.10f, 0.15f, 0.23f)
    private val hair = floatArrayOf(0.055f, 0.045f, 0.04f)

    fun updateSignals(
        camera: Boolean,
        microphone: Boolean,
        audio: Float,
        stability: Float
    ) {
        cameraOn = camera
        microphoneOn = microphone
        audioLevel = audio.coerceIn(0f, 1f)
        dynamicStability = stability.coerceIn(0f, 1f)
    }

    fun rotateBy(dx: Float, dy: Float) {
        yaw = (yaw + dx) % 360f
        pitch = (pitch + dy).coerceIn(-20f, 20f)
    }

    override fun onSurfaceCreated(
        gl: javax.microedition.khronos.opengles.GL10?,
        config: javax.microedition.khronos.egl.EGLConfig?
    ) {
        GLES20.glClearColor(
            0.025f,
            0.045f,
            0.075f,
            1f
        )

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)

        GLES20.glBlendFunc(
            GLES20.GL_SRC_ALPHA,
            GLES20.GL_ONE_MINUS_SRC_ALPHA
        )

        shaderProgram = createProgram(
            VERTEX_SHADER,
            FRAGMENT_SHADER
        )

        uMvp = GLES20.glGetUniformLocation(
            shaderProgram,
            "uMVP"
        )

        uColor = GLES20.glGetUniformLocation(
            shaderProgram,
            "uColor"
        )

        uPulse = GLES20.glGetUniformLocation(
            shaderProgram,
            "uPulse"
        )

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
            projection,
            0,
            -ratio * 0.85f,
            ratio * 0.85f,
            -0.85f,
            0.85f,
            2f,
            20f
        )
    }

    override fun onDrawFrame(
        gl: javax.microedition.khronos.opengles.GL10?
    ) {
        GLES20.glClear(
            GLES20.GL_COLOR_BUFFER_BIT or
                GLES20.GL_DEPTH_BUFFER_BIT
        )

        GLES20.glUseProgram(shaderProgram)

        Matrix.setLookAtM(
            view,
            0,
            0f, 0f, 9f,
            0f, 0f, 0f,
            0f, 1f, 0f
        )

        val elapsed =
            (System.nanoTime() - startTime) / 1_000_000_000f

        val pulse =
            (0.25f + dynamicStability * 0.75f) *
                (0.75f + 0.25f * sin(elapsed * 2.4f))

        GLES20.glUniform1f(uPulse, pulse)

        val positionHandle =
            GLES20.glGetAttribLocation(
                shaderProgram,
                "aPosition"
            )

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

        // Rotate complete human model.

        drawHumanBody(elapsed)

        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    // ------------------------------------------------
    // COMPLETE HUMAN BODY
    // ------------------------------------------------

    private fun drawHumanBody(elapsed: Float) {

        drawHead()
        drawNeck()
        drawTorso()
        drawArms()
        drawHands()
        drawLegs()
        drawFeet()

        drawInternalSystems(elapsed)
        drawRefreshParticles(elapsed)
    }

    // ------------------------------------------------
    // HEAD AND FACE
    // ------------------------------------------------

    private fun drawHead() {

        // Main skull and face

        ellipsoid(
            0f, 2.18f, 0f,
            0.43f, 0.55f, 0.37f,
            skin
        )

        // Forehead

        ellipsoid(
            0f, 2.43f, 0.22f,
            0.34f, 0.25f, 0.20f,
            skinLight
        )

        // Jaw

        ellipsoid(
            0f, 1.91f, 0.10f,
            0.32f, 0.22f, 0.29f,
            skin
        )

        // Hair cap

        ellipsoid(
            0f, 2.57f, -0.01f,
            0.44f, 0.23f, 0.38f,
            hair
        )

        // Hair sides

        ellipsoid(
            -0.38f, 2.35f, -0.03f,
            0.09f, 0.28f, 0.27f,
            hair
        )

        ellipsoid(
            0.38f, 2.35f, -0.03f,
            0.09f, 0.28f, 0.27f,
            hair
        )

        // Ears

        ellipsoid(
            -0.44f, 2.17f, 0.02f,
            0.10f, 0.16f, 0.09f,
            skinDark
        )

        ellipsoid(
            0.44f, 2.17f, 0.02f,
            0.10f, 0.16f, 0.09f,
            skinDark
        )

        // Eyes

        ellipsoid(
            -0.17f, 2.29f, 0.325f,
            0.095f, 0.055f, 0.045f,
            floatArrayOf(0.96f, 0.94f, 0.88f)
        )

        ellipsoid(
            0.17f, 2.29f, 0.325f,
            0.095f, 0.055f, 0.045f,
            floatArrayOf(0.96f, 0.94f, 0.88f)
        )

        // Eye pupils

        ellipsoid(
            -0.17f, 2.29f, 0.367f,
            0.030f, 0.035f, 0.018f,
            floatArrayOf(0.10f, 0.12f, 0.14f)
        )

        ellipsoid(
            0.17f, 2.29f, 0.367f,
            0.030f, 0.035f, 0.018f,
            floatArrayOf(0.10f, 0.12f, 0.14f)
        )

        // Eyebrows

        ellipsoid(
            -0.17f, 2.38f, 0.32f,
            0.12f, 0.025f, 0.035f,
            hair
        )

        ellipsoid(
            0.17f, 2.38f, 0.32f,
            0.12f, 0.025f, 0.035f,
            hair
        )

        // Nose bridge

        ellipsoid(
            0f, 2.20f, 0.37f,
            0.065f, 0.14f, 0.075f,
            skinLight
        )

        // Nose tip

        ellipsoid(
            0f, 2.13f, 0.43f,
            0.09f, 0.055f, 0.065f,
            skin
        )

        // Mouth

        ellipsoid(
            0f, 2.00f, 0.375f,
            0.11f, 0.025f, 0.025f,
            floatArrayOf(0.43f, 0.13f, 0.14f)
        )

        // Chin

        ellipsoid(
            0f, 1.92f, 0.30f,
            0.16f, 0.09f, 0.08f,
            skinLight
        )

        // Camera indicators on eyes

        if (cameraOn) {
            ellipsoid(
                -0.17f, 2.29f, 0.39f,
                0.025f, 0.025f, 0.015f,
                floatArrayOf(0.15f, 0.95f, 1f)
            )

            ellipsoid(
                0.17f, 2.29f, 0.39f,
                0.025f, 0.025f, 0.015f,
                floatArrayOf(0.15f, 0.95f, 1f)
            )
        }
    }

    // ------------------------------------------------
    // NECK
    // ------------------------------------------------

    private fun drawNeck() {

        ellipsoid(
            0f, 1.66f, 0f,
            0.19f, 0.34f, 0.20f,
            skin
        )

        ellipsoid(
            0f, 1.60f, 0.08f,
            0.16f, 0.25f, 0.17f,
            skinLight
        )
    }

    // ------------------------------------------------
    // TORSO
    // ------------------------------------------------

    private fun drawTorso() {

        // Main upper body

        ellipsoid(
            0f, 0.92f, 0f,
            0.75f, 0.85f, 0.37f,
            shirt
        )

        // Chest

        ellipsoid(
            0f, 1.23f, 0.06f,
            0.76f, 0.42f, 0.38f,
            shirtLight
        )

        // Abdomen

        ellipsoid(
            0f, 0.55f, 0.04f,
            0.53f, 0.48f, 0.32f,
            shirt
        )

        // Shoulder joints

        ellipsoid(
            -0.72f, 1.38f, 0f,
            0.27f, 0.29f, 0.31f,
            shirtLight
        )

        ellipsoid(
            0.72f, 1.38f, 0f,
            0.27f, 0.29f, 0.31f,
            shirtLight
        )

        // Pelvis

        ellipsoid(
            0f, -0.05f, 0f,
            0.55f, 0.40f, 0.33f,
            pants
        )

        // Clothing waist

        ellipsoid(
            0f, 0.18f, 0.02f,
            0.54f, 0.15f, 0.33f,
            pants
        )
    }

    // ------------------------------------------------
    // ARMS
    // ------------------------------------------------

    private fun drawArms() {

        // Left upper arm

        capsule(
            -0.78f, 1.30f, 0f,
            -1.02f, 0.65f, 0.02f,
            0.19f,
            skin
        )

        // Right upper arm

        capsule(
            0.78f, 1.30f, 0f,
            1.02f, 0.65f, 0.02f,
            0.19f,
            skin
        )

        // Elbows

        ellipsoid(
            -1.02f, 0.65f, 0.02f,
            0.17f, 0.17f, 0.17f,
            skinLight
        )

        ellipsoid(
            1.02f, 0.65f, 0.02f,
            0.17f, 0.17f, 0.17f,
            skinLight
        )

        // Forearms

        capsule(
            -1.02f, 0.65f, 0.02f,
            -1.12f, -0.12f, 0.12f,
            0.14f,
            skin
        )

        capsule(
            1.02f, 0.65f, 0.02f,
            1.12f, -0.12f, 0.12f,
            0.14f,
            skin
        )

        // Wrists

        ellipsoid(
            -1.12f, -0.12f, 0.12f,
            0.12f, 0.10f, 0.12f,
            skinLight
        )

        ellipsoid(
            1.12f, -0.12f, 0.12f,
            0.12f, 0.10f, 0.12f,
            skinLight
        )
    }

    // ------------------------------------------------
    // HANDS AND FINGERS
    // ------------------------------------------------

    private fun drawHands() {

        for (side in listOf(-1f, 1f)) {

            val x = side * 1.12f

            // Palm

            ellipsoid(
                x, -0.29f, 0.13f,
                0.15f, 0.19f, 0.09f,
                skin
            )

            // Four fingers

            for (i in 0..3) {

                val fingerX = x + side * (i - 1.5f) * 0.065f

                capsule(
                    fingerX, -0.38f, 0.14f,
                    fingerX, -0.57f, 0.14f,
                    0.027f,
                    skinLight
                )
            }

            // Thumb

            capsule(
                x - side * 0.12f, -0.26f, 0.16f,
                x - side * 0.22f, -0.40f, 0.16f,
                0.045f,
                skin
            )
        }
    }

    // ------------------------------------------------
    // LEGS
    // ------------------------------------------------

    private fun drawLegs() {

        // Upper thighs

        capsule(
            -0.29f, -0.34f, 0f,
            -0.38f, -1.25f, 0f,
            0.26f,
            pants
        )

        capsule(
            0.29f, -0.34f, 0f,
            0.38f, -1.25f, 0f,
            0.26f,
            pants
        )

        // Knees

        ellipsoid(
            -0.38f, -1.28f, 0.05f,
            0.20f, 0.19f, 0.20f,
            pants
        )

        ellipsoid(
            0.38f, -1.28f, 0.05f,
            0.20f, 0.19f, 0.20f,
            pants
        )

        // Lower legs

        capsule(
            -0.38f, -1.32f, 0f,
            -0.39f, -2.20f, 0f,
            0.16f,
            pants
        )

        capsule(
            0.38f, -1.32f, 0f,
            0.39f, -2.20f, 0f,
            0.16f,
            pants
        )

        // Ankles

        ellipsoid(
            -0.39f, -2.20f, 0.02f,
            0.13f, 0.15f, 0.14f,
            skin
        )

        ellipsoid(
            0.39f, -2.20f, 0.02f,
            0.13f, 0.15f, 0.14f,
            skin
        )
    }

    // ------------------------------------------------
    // FEET
    // ------------------------------------------------

    private fun drawFeet() {

        ellipsoid(
            -0.39f, -2.35f, 0.16f,
            0.19f, 0.13f, 0.34f,
            floatArrayOf(0.12f, 0.13f, 0.15f)
        )

        ellipsoid(
            0.39f, -2.35f, 0.16f,
            0.19f, 0.13f, 0.34f,
            floatArrayOf(0.12f, 0.13f, 0.15f)
        )
    }

    // ------------------------------------------------
    // INTERNAL SYSTEM VISUALIZATION
    // ------------------------------------------------

    private fun drawInternalSystems(elapsed: Float) {

        // Brain / processing core

        ellipsoid(
            0f, 2.17f, -0.03f,
            0.22f, 0.17f, 0.20f,
            floatArrayOf(
                0.48f + dynamicStability * 0.30f,
                0.22f,
                0.86f
            )
        )

        // Lungs

        ellipsoid(
            -0.29f, 1.00f, 0.34f,
            0.22f, 0.36f, 0.10f,
            floatArrayOf(0.08f, 0.70f, 0.83f)
        )

        ellipsoid(
            0.29f, 1.00f, 0.34f,
            0.22f, 0.36f, 0.10f,
            floatArrayOf(0.08f, 0.70f, 0.83f)
        )

        // Heart

        val heartScale = 0.16f + audioLevel * 0.035f

        ellipsoid(
            -0.05f, 0.77f, 0.43f,
            heartScale, heartScale * 1.15f, 0.12f,
            floatArrayOf(0.96f, 0.16f, 0.27f)
        )

        // Memory region

        ellipsoid(
            0.10f, 0.15f, 0.35f,
            0.22f, 0.17f, 0.12f,
            floatArrayOf(0.65f, 0.30f, 0.96f)
        )

        // Neural pathway

        capsule(
            0f, 1.55f, 0.23f,
            0f, -0.10f, 0.32f,
            0.035f,
            floatArrayOf(0.12f, 0.84f, 0.96f)
        )

        // Internal sensory indicators

        if (microphoneOn) {
            ellipsoid(
                -0.47f, 2.17f, 0.03f,
                0.055f, 0.075f, 0.055f,
                floatArrayOf(0.15f, 0.95f, 0.55f)
            )

            ellipsoid(
                0.47f, 2.17f, 0.03f,
                0.055f, 0.075f, 0.055f,
                floatArrayOf(0.15f, 0.95f, 0.55f)
            )
        }
    }

    // ------------------------------------------------
    // REFRESH PARTICLES
    // ------------------------------------------------

    private fun drawRefreshParticles(elapsed: Float) {

        val refreshColor = floatArrayOf(
            0.15f,
            0.85f,
            1f
        )

        for (i in 0..7) {

            val t = (elapsed * 0.40f + i / 8f) % 1f

            val y = 1.60f - t * 2.0f

            val particleSize =
                0.035f + dynamicStability * 0.025f

            ellipsoid(
                0f, y, 0.38f,
                particleSize,
                particleSize,
                particleSize,
                refreshColor
            )
        }

        // External incoming information particles

        for (i in 0..3) {

            val t = (elapsed * 0.28f + i / 4f) % 1f

            val x = -1.15f + t * 0.85f

            ellipsoid(
                x,
                0.95f,
                0.25f,
                0.045f,
                0.045f,
                0.045f,
                floatArrayOf(1f, 0.65f, 0.20f)
            )
        }
    }

    // ------------------------------------------------
    // 3D GEOMETRY
    // ------------------------------------------------

    private fun capsule(
        x1: Float,
        y1: Float,
        z1: Float,
        x2: Float,
        y2: Float,
        z2: Float,
        radius: Float,
        color: FloatArray
    ) {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1

        val length = sqrt(dx * dx + dy * dy + dz * dz)
            .coerceAtLeast(0.001f)

        val angle = Math.toDegrees(
            atan2(dx.toDouble(), dy.toDouble())
        ).toFloat()

        ellipsoid(
            (x1 + x2) / 2f,
            (y1 + y2) / 2f,
            (z1 + z2) / 2f,
            radius,
            length / 2f + radius * 0.35f,
            radius,
            color,
            angle
        )
    }

    private fun ellipsoid(
        x: Float,
        y: Float,
        z: Float,
        sx: Float,
        sy: Float,
        sz: Float,
        color: FloatArray,
        rotation: Float = 0f
    ) {
        Matrix.setIdentityM(model, 0)

        Matrix.rotateM(model, 0, yaw, 0f, 1f, 0f)
        Matrix.rotateM(model, 0, pitch, 1f, 0f, 0f)

        Matrix.translateM(model, 0, x, y, z)

        if (rotation != 0f) {
            Matrix.rotateM(
                model,
                0,
                rotation,
                0f,
                0f,
                1f
            )
        }

        Matrix.scaleM(model, 0, sx, sy, sz)

        Matrix.multiplyMM(
            modelView,
            0,
            view,
            0,
            model,
            0
        )

        Matrix.multiplyMM(
            mvp,
            0,
            projection,
            0,
            modelView,
            0
        )

        GLES20.glUniformMatrix4fv(
            uMvp,
            1,
            false,
            mvp,
            0
        )

        GLES20.glUniform4f(
            uColor,
            color[0],
            color[1],
            color[2],
            1f
        )

        GLES20.glDrawArrays(
            GLES20.GL_TRIANGLES,
            0,
            vertexCount
        )
    }

    // ------------------------------------------------
    // SPHERE MESH
    // ------------------------------------------------

    private fun createSphere() {

        val data = ArrayList<Float>()

        val latitude = 20
        val longitude = 24

        for (i in 0 until latitude) {

            val a0 = Math.PI *
                (-0.5 + i.toDouble() / latitude)

            val a1 = Math.PI *
                (-0.5 + (i + 1).toDouble() / latitude)

            for (j in 0 until longitude) {

                val b0 = 2.0 * Math.PI * j / longitude
                val b1 = 2.0 * Math.PI * (j + 1) / longitude

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

    private fun point(
        latitude: Double,
        longitude: Double
    ): List<Float> {
        return listOf(
            (cos(latitude) * cos(longitude)).toFloat(),
            sin(latitude).toFloat(),
            (cos(latitude) * sin(longitude)).toFloat()
        )
    }

    // ------------------------------------------------
    // SHADER PROGRAM
    // ------------------------------------------------

    private fun createProgram(
        vertex: String,
        fragment: String
    ): Int {

        fun compile(
            type: Int,
            source: String
        ): Int {

            val shader = GLES20.glCreateShader(type)

            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)

            val status = IntArray(1)

            GLES20.glGetShaderiv(
                shader,
                GLES20.GL_COMPILE_STATUS,
                status,
                0
            )

            if (status[0] == 0) {
                val error = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                throw IllegalStateException("Shader compilation failed: $error")
            }

            return shader
        }

        val vertexShader = compile(
            GLES20.GL_VERTEX_SHADER,
            vertex
        )

        val fragmentShader = compile(
            GLES20.GL_FRAGMENT_SHADER,
            fragment
        )

        val program = GLES20.glCreateProgram()

        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)

        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)

        GLES20.glGetProgramiv(
            program,
            GLES20.GL_LINK_STATUS,
            linkStatus,
            0
        )

        if (linkStatus[0] == 0) {
            val error = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw IllegalStateException("Shader linking failed: $error")
        }

        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        return program
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

                vec3 normal = normalize(vNormal);

                vec3 lightDirection =
                    normalize(vec3(-0.35, 0.75, 1.0));

                float diffuse = max(
                    dot(normal, lightDirection),
                    0.0
                );

                float lighting = 0.38 + diffuse * 0.62;

                float pulseEffect =
                    0.88 + 0.12 * uPulse;

                vec3 finalColor =
                    uColor.rgb * lighting * pulseEffect;

                gl_FragColor = vec4(finalColor, uColor.a);
            }
        """
    }
}
