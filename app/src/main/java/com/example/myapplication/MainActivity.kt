package com.example.myapplication

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.OutputStream
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: MyGLRenderer
    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private lateinit var imageReader: ImageReader

    private var backCameraId: String? = null
    private var frontCameraId: String? = null
    private var currentCameraId: String? = null

    // FPS Counter variables
    private var frameCount = 0
    private var lastFpsTimestamp = 0L

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
        val image = it.acquireLatestImage() ?: return@OnImageAvailableListener
        val plane = image.planes[0]
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // Log frame processing time
        val startTime = System.currentTimeMillis()
        val processedBytes = processFrame(image.width, image.height, bytes, plane.rowStride)
        val endTime = System.currentTimeMillis()
        Log.d("FrameProcessing", "JNI call took: ${endTime - startTime}ms")

        val byteBuffer = ByteBuffer.wrap(processedBytes)
        renderer.updateTexture(byteBuffer, image.width, image.height)
        glSurfaceView.requestRender()
        image.close()

        // Calculate and log FPS
        frameCount++
        val currentTime = System.currentTimeMillis()
        if (lastFpsTimestamp == 0L) {
            lastFpsTimestamp = currentTime
        }
        val elapsedTime = currentTime - lastFpsTimestamp
        if (elapsedTime >= 1000) {
            val fps = frameCount / (elapsedTime / 1000.0)
            Log.d("FPSCounter", "FPS: %.2f".format(fps))
            frameCount = 0
            lastFpsTimestamp = currentTime
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        glSurfaceView = findViewById(R.id.glSurfaceView)

        setupButtons()
        findCameraIds()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            setupAndStartCamera()
        }
    }

    private fun setupButtons() {
        val switchCameraButton: FloatingActionButton = findViewById(R.id.fab_switch_camera)
        switchCameraButton.setOnClickListener {
            switchCamera()
        }

        val captureButton: Button = findViewById(R.id.btn_capture)
        captureButton.setOnClickListener {
            captureFrame()
        }
    }

    private fun findCameraIds() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_BACK -> backCameraId = cameraId
                CameraCharacteristics.LENS_FACING_FRONT -> frontCameraId = cameraId
            }
        }
        currentCameraId = backCameraId // Default to back camera
    }

    private fun switchCamera() {
        currentCameraId = if (currentCameraId == backCameraId) {
            renderer.setFrontFacing(true)
            frontCameraId
        } else {
            renderer.setFrontFacing(false)
            backCameraId
        }
        closeCamera()
        openCamera()
    }

    private fun captureFrame() {
        renderer.requestSaveFrame { bitmap ->
            saveBitmap(bitmap)
        }
    }

    private fun saveBitmap(bitmap: Bitmap) {
        val displayName = "OpenCV_Capture_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                runOnUiThread {
                    Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                }
            } ?: runOnUiThread {
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAndStartCamera() {
        startBackgroundThread()
        setupGLSurfaceView()
        renderer.setFrontFacing(currentCameraId == frontCameraId)
        openCamera()
    }

    private fun setupGLSurfaceView() {
        glSurfaceView.setEGLContextClientVersion(2)
        renderer = MyGLRenderer()
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    private fun openCamera() {
        if (currentCameraId == null) return

        imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.YUV_420_888, 2)
        imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return

        cameraManager.openCamera(currentCameraId!!, cameraStateCallback, backgroundHandler)
    }

    private fun createCameraPreviewSession() {
        if (cameraDevice == null) return
        val previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        previewRequestBuilder.addTarget(imageReader.surface)

        cameraDevice?.createCaptureSession(listOf(imageReader.surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                cameraCaptureSession = session
                cameraCaptureSession?.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {}
        }, backgroundHandler)
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (!this::backgroundThread.isInitialized) {
                setupAndStartCamera()
            }
            glSurfaceView.onResume()
        }
    }

    override fun onPause() {
        glSurfaceView.onPause()
        if (this::backgroundThread.isInitialized) {
            closeCamera()
            stopBackgroundThread()
        }
        super.onPause()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun stopBackgroundThread() {
        try {
            backgroundThread.quitSafely()
            backgroundThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun closeCamera() {
        try {
        cameraCaptureSession?.close()
        cameraCaptureSession = null
        cameraDevice?.close()
        cameraDevice = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupAndStartCamera()
            }
        }
    }

    private external fun processFrame(width: Int, height: Int, grayBytes: ByteArray, rowStride: Int): ByteArray

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        init {
            System.loadLibrary("myapplication")
            System.loadLibrary("opencv_java4")
        }
    }
}
