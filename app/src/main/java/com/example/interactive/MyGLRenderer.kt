import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10
import kotlin.math.min
import kotlin.random.Random

class MyGLRenderer : GLSurfaceView.Renderer {
    private var program: Int = 0
    private var vertexBuffer: FloatBuffer? = null
    private val touchPoints: MutableList<Float> = mutableListOf()

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var lastFrameTime: Long = 0
    private var frameCount: Int = 0
    private var fps: Float = 0f

    private val particles = mutableListOf<Particle>()

    class Particle(var x: Float, var y: Float) {
        var vx: Float = Random.nextFloat() * 0.02f - 0.01f
        var vy: Float = Random.nextFloat() * 0.02f - 0.01f
        var size: Float = Random.nextFloat() * 15f + 5f
    }


    private val vertexShaderCode = """
        attribute vec4 vPosition;
        attribute float pointSize;
        varying float alpha;
        void main() {
            gl_Position = vPosition;
            gl_PointSize = pointSize;
            alpha = pointSize / 20.0;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying float alpha;
        void main() {
            gl_FragColor = vec4(1.0, 0.0, 0.0, alpha);
        }
    """.trimIndent()

    init {
        updateVertexBuffer()
    }

    fun addTouchPoint(x: Float, y: Float) {
        val glX = (x / screenWidth) * 2 - 1
        val glY = -((y / screenHeight) * 2 - 1)
        for (i in 0 until 10) {  // Add 10 particles per touch
            particles.add(Particle(glX, glY))
        }
        Log.d("GLRenderer", "Added particles, Total particles: ${particles.size}")
    }

    private fun updateParticles() {
        val particlesToDraw = ArrayList<Particle>()  // Create a temporary list

        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            particle.x += particle.vx
            particle.y += particle.vy
            particle.size *= 0.99f  // Slowly decrease size

            // Bounce off edges
            if (particle.x > 1f || particle.x < -1f) particle.vx *= -1
            if (particle.y > 1f || particle.y < -1f) particle.vy *= -1

            if (particle.size >= 1f) {
                particlesToDraw.add(particle)  // Add remaining particles to draw list
            } else {
                iterator.remove()  // Safely remove small particles using iterator
            }
        }

        particles.clear()  // Optional: Clear the original list for efficiency
        particles.addAll(particlesToDraw)  // Update original list after modification
    }

    private fun updateVertexBuffer() {
        val pointsArray = FloatArray(particles.size * 3) // 3 floats per particle: x, y, size
        particles.forEachIndexed { index, particle ->
            pointsArray[index * 3] = particle.x
            pointsArray[index * 3 + 1] = particle.y
            pointsArray[index * 3 + 2] = particle.size
        }
        vertexBuffer = ByteBuffer.allocateDirect(pointsArray.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(pointsArray)
                position(0)
            }
    }


    override fun onDrawFrame(gl: GL10?) {
        // Clear the screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Use the shader program
        GLES20.glUseProgram(program)

        // Set the vertex position attribute
        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        // Set the point size attribute
        val pointSizeHandle = GLES20.glGetAttribLocation(program, "pointSize")
        GLES20.glEnableVertexAttribArray(pointSizeHandle)
        GLES20.glVertexAttrib1f(pointSizeHandle, 20.0f) // Set a fixed point size for all particles
        // Update particle positions
        updateParticles()

        // Draw each particle
        particles.forEach { particle ->
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1)
        }


        // Calculate FPS
        val currentTime = System.nanoTime()
        frameCount++
        if (currentTime - lastFrameTime >= 1000000000) { // 1 second in nanoseconds
            fps = frameCount.toFloat()
            frameCount = 0
            lastFrameTime = currentTime
            Log.d("GLRenderer", "FPS: $fps")
        }
    }

    override fun onSurfaceCreated(p0: GL10?, p1: javax.microedition.khronos.egl.EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0f, 1.0f) // Dark blue background


        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)

            val linked = IntArray(1)
            GLES20.glGetProgramiv(it, GLES20.GL_LINK_STATUS, linked, 0)
            if (linked[0] == 0) {
                Log.e("GLRenderer", "Program linking error: ${GLES20.glGetProgramInfoLog(it)}")
                throw RuntimeException("Program linking failed")
            }
        }

        // Check OpenGL ES version and extensions
        Log.i("GLRenderer", "GL Version: ${GLES20.glGetString(GLES20.GL_VERSION)}")
        Log.i("GLRenderer", "GL Extensions: ${GLES20.glGetString(GLES20.GL_EXTENSIONS)}")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        screenWidth = width
        screenHeight = height
        Log.i("GLRenderer", "Surface changed: $width x $height")
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                Log.e(
                    "GLRenderer",
                    "Shader compilation error: ${GLES20.glGetShaderInfoLog(shader)}"
                )
                throw RuntimeException("Shader compilation failed")
            }
        }
    }
}
