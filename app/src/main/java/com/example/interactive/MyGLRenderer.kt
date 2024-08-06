import android.opengl.EGLConfig
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

    fun addTouchPoint(x: Float, y: Float) {
        val glX = (x / screenWidth) * 2 - 1
        val glY = -((y / screenHeight) * 2 - 1)
        synchronized(particles) {
            for (i in 0 until 10) {  // Add 10 particles per touch
                particles.add(Particle(glX, glY))
            }
        }
        Log.d("GLRenderer", "Added particles, Total particles: ${particles.size}")
    }

    private fun updateParticles() {
        synchronized(particles) {
            val iterator = particles.iterator()

            while (iterator.hasNext()) {
                val particle = iterator.next()
                particle.x += particle.vx
                particle.y += particle.vy
                particle.size *= 0.99f  // Slowly decrease size

                // Bounce off edges
                if (particle.x > 1f || particle.x < -1f) particle.vx *= -1
                if (particle.y > 1f || particle.y < -1f) particle.vy *= -1

                if (particle.size < 1f) {
                    iterator.remove()
                }
            }
        }
    }

    private fun updateVertexBuffer() {
        synchronized(particles) {
            if (particles.isEmpty()) {
                vertexBuffer = null
                return
            }

            val pointsArray = FloatArray(particles.size * 3)
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
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        synchronized(particles) {
            if (particles.isNotEmpty()) {
                GLES20.glUseProgram(program)

                updateParticles()
                updateVertexBuffer()

                val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
                GLES20.glEnableVertexAttribArray(positionHandle)
                vertexBuffer?.let {
                    GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 12, it)
                }

                val pointSizeHandle = GLES20.glGetAttribLocation(program, "pointSize")
                GLES20.glEnableVertexAttribArray(pointSizeHandle)
                vertexBuffer?.let {
                    it.position(2)
                    GLES20.glVertexAttribPointer(pointSizeHandle, 1, GLES20.GL_FLOAT, false, 12, it)
                }

                GLES20.glDrawArrays(GLES20.GL_POINTS, 0, particles.size)

                GLES20.glDisableVertexAttribArray(positionHandle)
                GLES20.glDisableVertexAttribArray(pointSizeHandle)
            }

        }

        // Calculate FPS
        val currentTime = System.nanoTime()
        frameCount++
        if (currentTime - lastFrameTime >= 1000000000) {
            fps = frameCount.toFloat()
            frameCount = 0
            lastFrameTime = currentTime
            Log.d("GLRenderer", "FPS: $fps")
        }
    }


    override fun onSurfaceCreated(p0: GL10?, p1: javax.microedition.khronos.egl.EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

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
                Log.e("GLRenderer", "Shader compilation error: ${GLES20.glGetShaderInfoLog(shader)}")
                throw RuntimeException("Shader compilation failed")
            }
        }
    }
}
