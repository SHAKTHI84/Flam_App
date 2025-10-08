package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.Matrix
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer : GLSurfaceView.Renderer {

    private var program = 0
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var textureUniformHandle = 0
    private var textureId = 0

    private var viewWidth = 0
    private var viewHeight = 0

    @Volatile private var isFrontFacing = false

    // For thread-safe texture updates
    @Volatile private var texturePixels: ByteBuffer? = null
    @Volatile private var textureWidth: Int = 0
    @Volatile private var textureHeight: Int = 0
    @Volatile private var newTextureAvailable = false

    // For saving a frame
    private var saveFrameCallback: ((Bitmap) -> Unit)? = null

    private val vertices = floatArrayOf(
        -1.0f, -1.0f,  // bottom left
         1.0f, -1.0f,  // bottom right
        -1.0f,  1.0f,  // top left
         1.0f,  1.0f   // top right
    )

    private val texCoords = floatArrayOf(
        1.0f, 0.0f,  // for Bottom-Left vertex
        1.0f, 1.0f,  // for Bottom-Right vertex
        0.0f, 0.0f,  // for Top-Left vertex
        0.0f, 1.0f,  // for Top-Right vertex
    )

    private val texCoordsFront = floatArrayOf(
        0.0f, 0.0f,  // for Bottom-Left vertex
        0.0f, 1.0f,  // for Bottom-Right vertex
        1.0f, 0.0f,  // for Top-Left vertex
        1.0f, 1.0f,  // for Top-Right vertex
    )

    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run {
        order(ByteOrder.nativeOrder()).asFloatBuffer().also { it.put(vertices); it.position(0) }
    }

    private val texCoordBuffer: FloatBuffer = ByteBuffer.allocateDirect(texCoords.size * 4).run {
        order(ByteOrder.nativeOrder()).asFloatBuffer().also { it.put(texCoords); it.position(0) }
    }

    private val texCoordBufferFront: FloatBuffer = ByteBuffer.allocateDirect(texCoordsFront.size * 4).run {
        order(ByteOrder.nativeOrder()).asFloatBuffer().also { it.put(texCoordsFront); it.position(0) }
    }

    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        void main() {
            gl_Position = aPosition;
            vTexCoord = aTexCoord;
        }
    """

    private val fragmentShaderCode = """
        precision mediump float;
        uniform sampler2D uTexture;
        varying vec2 vTexCoord;
        void main() {
            gl_FragColor = texture2D(uTexture, vTexCoord);
        }
    """

    fun setFrontFacing(isFront: Boolean) {
        isFrontFacing = isFront
    }

    @Synchronized
    fun updateTexture(pixels: ByteBuffer, width: Int, height: Int) {
        texturePixels = pixels
        textureWidth = width
        textureHeight = height
        newTextureAvailable = true
    }

    fun requestSaveFrame(callback: (Bitmap) -> Unit) {
        saveFrameCallback = callback
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        textureUniformHandle = GLES20.glGetUniformLocation(program, "uTexture")

        textureId = createTexture()
    }

    override fun onDrawFrame(gl: GL10?) {
        synchronized(this) {
            if (newTextureAvailable) {
                newTextureAvailable = false
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, textureWidth, textureHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, texturePixels)
            }
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        val currentTexCoordBuffer = if (isFrontFacing) texCoordBufferFront else texCoordBuffer
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, currentTexCoordBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureUniformHandle, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        saveFrameCallback?.let {
            saveFrame(it)
            saveFrameCallback = null
        }

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun saveFrame(callback: (Bitmap) -> Unit) {
        val bmp = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
        val buffer = ByteBuffer.allocate(viewWidth * viewHeight * 4)
        GLES20.glReadPixels(0, 0, viewWidth, viewHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)
        buffer.rewind()
        bmp.copyPixelsFromBuffer(buffer)

        val matrix = Matrix().apply { preScale(1.0f, -1.0f) }
        val flippedBitmap = Bitmap.createBitmap(bmp, 0, 0, viewWidth, viewHeight, matrix, false)
        bmp.recycle()

        callback(flippedBitmap)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        GLES20.glViewport(0, 0, width, height)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    private fun createTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return textures[0]
    }
}
