package com.example.workout_track_final

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoFrameProcessor : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var tflite: Interpreter
    private lateinit var inputImageBuffer: ByteBuffer
    private lateinit var viewFinder: PreviewView // Define the viewFinder property
    private lateinit var textView: TextView // Define the textView property
    private val TAG = "VideoFrameProcessor"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_frame_processor)

        viewFinder = findViewById(R.id.viewFinder) // Initialize viewFinder
        textView = findViewById(R.id.textView) // Initialize textView

        // Initialize TensorFlow Lite model
        try {
            tflite = Interpreter(loadModelFile("tflite_model.tflite"))
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TensorFlow Lite model", e)
        }

        // Setup Camera
        startCamera()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_PERMISSIONS)
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider) // Set the surface provider
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(1280, 720)) // Adjust resolution as needed
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
                .build().also {
                    it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
                        processImageProxy(imageProxy) // Process each image frame
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

        }, ContextCompat.getMainExecutor(this))
    }

    private fun loadModelFile(modelFile: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelFile)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun resizeBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        // Convert ImageProxy to Bitmap
        val bitmap = imageProxy.image?.let { image ->
            val planes = image.planes
            val buffer = planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            BitmapFactory.decodeByteArray(data, 0, data.size)
        }

        bitmap?.let {
            // Process the bitmap using the TensorFlow Lite model
            processVideoFrame(it)
        } ?: Log.e(TAG, "Failed to convert ImageProxy to Bitmap")

        // Close the imageProxy to avoid memory leaks
        imageProxy.close()
    }

    private fun processVideoFrame(bitmap: Bitmap) {
        // Resize bitmap if necessary
        val resizedBitmap = resizeBitmap(bitmap, INPUT_SIZE, INPUT_SIZE)

        // Pre-process resized bitmap to ByteBuffer
        inputImageBuffer = convertBitmapToByteBuffer(resizedBitmap)

        // Prepare the output array
        val outputMap = Array(1) { FloatArray(NUM_CLASSES) }

        try {
            // Run inference
            tflite.run(inputImageBuffer, outputMap)

            // Process the output
            val detectedExercise = processModelOutput(outputMap)
            Log.d(TAG, "Detected Exercise: $detectedExercise")

            // Here you can handle additional processing, like counting reps
        } catch (e: Exception) {
            Log.e(TAG, "Error during model inference: ${e.message}")
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            Log.e(TAG, "Bitmap has invalid dimensions: ${bitmap.width}x${bitmap.height}")
            return ByteBuffer.allocate(0) // Return an empty ByteBuffer or handle this case appropriately
        }

        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val value = intValues[pixel++]
                byteBuffer.putFloat(((value shr 16 and 0xFF) / 255.0f)) // R
                byteBuffer.putFloat(((value shr 8 and 0xFF) / 255.0f))  // G
                byteBuffer.putFloat((value and 0xFF) / 255.0f)          // B
            }
        }
        return byteBuffer
    }

    private fun processModelOutput(outputMap: Array<FloatArray>): String {
        // Implement your logic to process the output and return detected exercise
        // For example:
        val detectedClassIndex = outputMap[0].indices.maxByOrNull { outputMap[0][it] } ?: -1
        return "Detected Class Index: $detectedClassIndex" // Return the detected class name or index
    }

    override fun onDestroy() {
        super.onDestroy()
        tflite.close()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val INPUT_SIZE = 224 // Change as per your model's input size
        private const val NUM_CLASSES = 10 // Change as per your model's output classes
        private const val REQUEST_CODE_PERMISSIONS = 101
    }
}
