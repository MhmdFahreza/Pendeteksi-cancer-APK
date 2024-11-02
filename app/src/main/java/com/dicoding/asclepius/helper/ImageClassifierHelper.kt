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

            // Gunakan inputShape untuk menentukan ukuran gambar input
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputShape[1], inputShape[2], true)

            // Siapkan TensorImage dan sesuaikan dengan inputShape
            val tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(resizedBitmap)

            // Output Buffer untuk probabilitas dua kelas
            val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 2), DataType.FLOAT32)

            // Menjalankan inferensi
            interpreter.run(tensorImage.buffer, outputBuffer.buffer.rewind())

            // Mengambil hasil prediksi dari model
            val outputArray = outputBuffer.floatArray
            val confidenceCancer = outputArray[0]
            val confidenceNonCancer = outputArray[1]

            // Menentukan prediksi berdasarkan confidence score tertinggi
            val (prediksi, confidence) = if (confidenceCancer < confidenceNonCancer) {
                "Cancer" to confidenceNonCancer
            } else {
                "Non-Cancer" to confidenceCancer
            }

            // Mengembalikan hasil prediksi dan confidence score dalam format baru
            return "Prediksi: $prediksi\nConfidence Score: ${String.format("%.2f", confidence * 100)}%"

        } catch (e: Exception) {
            e.printStackTrace()
            // Mengembalikan pesan kesalahan jika terjadi kegagalan dalam proses klasifikasi
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
