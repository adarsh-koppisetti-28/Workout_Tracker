package com.example.workout_track_final

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.room.jarjarred.org.antlr.v4.gui.Interpreter
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs


import org.tensorflow.lite.support.image.ImageProcessor

import kotlin.math.abs

class TFLiteModelProcessor(private val context: Context) {

    private val interpreter: Interpreter by lazy {
        Interpreter(loadModelFile(context))
    }

    private fun loadModelFile(context: Context): Array<out String>? {
        val assetFileDescriptor = context.assets.openFd("tflite_model.tflite")
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun processVideoForReps(videoUri: Uri): Int {
        val mediaRetriever = MediaMetadataRetriever()
        mediaRetriever.setDataSource(context, videoUri)

        val frameRate = mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toInt() ?: 0
        val threshold = 0.05f // Adjusted for normalized motion detection
        var previousFrameDiff = 0.0f
        var repsCount = 0
        var inMotion = false

        // Create an image processor to resize the image
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(64, 64, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        for (i in 0 until frameRate step 5) {  // Sample every 5 frames
            val bitmap = mediaRetriever.getFrameAtIndex(i) ?: continue
            val tensorImage = TensorImage.fromBitmap(bitmap)

            // Resize the image
            val resizedImage = imageProcessor.process(tensorImage)

            // Create tensor buffer for model input
            val tensorBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 64, 64, 3), org.tensorflow.lite.DataType.FLOAT32)

            // Create a float array to hold pixel values
            val floatArray = FloatArray(64 * 64 * 3) // 64*64 pixels * 3 color channels
            val pixels = IntArray(64 * 64)
            resizedImage.getBitmap().getPixels(pixels, 0, 64, 0, 0, 64, 64)

            // Populate the float array with normalized pixel values
            for (j in pixels.indices) {
                val pixel = pixels[j]
                floatArray[j * 3] = ((pixel shr 16 and 0xFF) / 255.0f) // Red
                floatArray[j * 3 + 1] = ((pixel shr 8 and 0xFF) / 255.0f) // Green
                floatArray[j * 3 + 2] = (pixel and 0xFF) / 255.0f // Blue
            }

            // Load the float array into the tensor buffer
            tensorBuffer.loadArray(floatArray)

            // Run inference
            // Note: Make sure your interpreter is set up correctly and accepts input of shape [1, 64, 64, 3]
            interpreter.run(tensorBuffer.buffer.rewind(), tensorBuffer.buffer.rewind())

            val motionScore = tensorBuffer.floatArray.sum() / tensorBuffer.floatArray.size // Normalize motion score
            val frameDiff = abs(motionScore - previousFrameDiff)

            // Count reps based on motion detection
            if (frameDiff > threshold && !inMotion) {
                repsCount++
                inMotion = true
            } else if (frameDiff < threshold) {
                inMotion = false
            }
            previousFrameDiff = motionScore
        }
        mediaRetriever.release()
        return repsCount
    }
}
