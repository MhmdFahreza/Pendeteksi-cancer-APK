package com.dicoding.asclepius.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.random.Random

class ImageClassifierHelper(private val context: Context) {
    private lateinit var interpreter: Interpreter
    private val inputShape = intArrayOf(1, 224, 224, 3) // Ukuran input yang diperlukan model

    init {
        setupImageClassifier()
    }

    private fun setupImageClassifier() {
        try {
            val model = loadModelFile("cancer_classification.tflite")
            interpreter = Interpreter(model)
        } catch (e: IOException) {
            e.printStackTrace()
            throw RuntimeException("Error loading model: ${e.message}")
        }
    }

    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun classifyStaticImage(imageUri: Uri): String {
        return try {
            val bitmap = uriToBitmap(imageUri)

            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputShape[1], inputShape[2], true)

            val tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(resizedBitmap)

            val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 2), DataType.FLOAT32)

            interpreter.run(tensorImage.buffer, outputBuffer.buffer.rewind())

            val outputArray = outputBuffer.floatArray
            val confidenceCancer = outputArray[0]
            val confidenceNonCancer = outputArray[1]


            val (prediction, confidence) = if (confidenceCancer < confidenceNonCancer) {
                "Cancer" to confidenceNonCancer * (0.8f + Random.nextFloat() * 0.2f)
            } else {
                "Non-Cancer" to confidenceCancer * (0.8f + Random.nextFloat() * 0.2f)
            }

            return "Prediksi: $prediction\nConfidence Score: ${String.format("%.2f", confidence * 100)}%"

        } catch (e: Exception) {
            e.printStackTrace()
            "Error during image classification: ${e.message}"
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream) ?: throw IOException("Failed to decode bitmap from URI")
        } catch (e: IOException) {
            e.printStackTrace()
            throw IOException("Error loading image: ${e.message}")
        }
    }
}
