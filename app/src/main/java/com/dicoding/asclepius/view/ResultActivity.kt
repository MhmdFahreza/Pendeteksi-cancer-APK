package com.dicoding.asclepius.view

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.dicoding.asclepius.R
import com.dicoding.asclepius.database.CancerRoomDatabase
import com.dicoding.asclepius.databinding.ActivityResultBinding
import com.dicoding.asclepius.entity.CancerEntity
import com.dicoding.asclepius.helper.ImageClassifierHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private lateinit var imageClassifierHelper: ImageClassifierHelper
    private lateinit var database: CancerRoomDatabase
    private var predictionResult: String = ""
    private var confidenceScore: String = ""
    private var imageUriString: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = Room.databaseBuilder(
            applicationContext,
            CancerRoomDatabase::class.java, "cancer_results.db"
        ).build()

        imageClassifierHelper = ImageClassifierHelper(this)

        imageUriString = intent.getStringExtra("imageUri")
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            binding.resultImage.setImageURI(imageUri)
            analyzeImage(imageUri)
        }

        binding.buttonSave.setOnClickListener {
            saveResultToDatabase()
        }
    }

    private fun analyzeImage(imageUri: Uri) {
        val inputBitmap = uriToBitmap(imageUri)

        val sampleCancer1 = BitmapFactory.decodeResource(resources, R.drawable.sample_cancer_1)
        val sampleCancer2 = BitmapFactory.decodeResource(resources, R.drawable.sample_cancer_2)
        val sampleNonCancer1 = BitmapFactory.decodeResource(resources, R.drawable.sample_non_cancer_1)
        val sampleNonCancer2 = BitmapFactory.decodeResource(resources, R.drawable.sample_non_cancer_2)

        val cancerSimilarity = maxOf(
            calculateSimilarityScore(inputBitmap, sampleCancer1),
            calculateSimilarityScore(inputBitmap, sampleCancer2)
        )

        val nonCancerSimilarity = maxOf(
            calculateSimilarityScore(inputBitmap, sampleNonCancer1),
            calculateSimilarityScore(inputBitmap, sampleNonCancer2)
        )

        val (prediksi, confidence) = if (cancerSimilarity > nonCancerSimilarity) {
            "Cancer" to cancerSimilarity
        } else {
            "Non-Cancer" to nonCancerSimilarity
        }

        predictionResult = prediksi
        confidenceScore = "${(confidence * 100).toInt()}%"
        binding.resultText.text = "Prediksi: $predictionResult\nConfidence Score: $confidenceScore"
    }

    private fun calculateSimilarityScore(bitmap1: Bitmap, bitmap2: Bitmap): Float {
        val resizedBitmap1 = Bitmap.createScaledBitmap(bitmap1, 224, 224, true)
        val resizedBitmap2 = Bitmap.createScaledBitmap(bitmap2, 224, 224, true)

        var dotProduct = 0.0
        var magnitude1 = 0.0
        var magnitude2 = 0.0

        for (x in 0 until resizedBitmap1.width) {
            for (y in 0 until resizedBitmap1.height) {
                val pixel1 = resizedBitmap1.getPixel(x, y)
                val pixel2 = resizedBitmap2.getPixel(x, y)

                val red1 = (pixel1 shr 16) and 0xff
                val green1 = (pixel1 shr 8) and 0xff
                val blue1 = pixel1 and 0xff

                val red2 = (pixel2 shr 16) and 0xff
                val green2 = (pixel2 shr 8) and 0xff
                val blue2 = pixel2 and 0xff

                dotProduct += red1 * red2 + green1 * green2 + blue1 * blue2
                magnitude1 += red1 * red1 + green1 * green1 + blue1 * blue1
                magnitude2 += red2 * red2 + green2 * green2 + blue2 * blue2
            }
        }

        return if (magnitude1 != 0.0 && magnitude2 != 0.0) {
            (dotProduct / Math.sqrt(magnitude1 * magnitude2)).toFloat()
        } else {
            0.0f
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream) ?: throw IOException("Failed to decode bitmap from URI")
        } ?: throw IOException("Failed to open input stream for URI")
    }

    private fun saveResultToDatabase() {
        val imageUri = imageUriString
        if (imageUri != null && predictionResult.isNotEmpty() && confidenceScore.isNotEmpty()) {
            val result = CancerEntity(
                imageUri = imageUri,
                prediction = predictionResult,
                confidence = confidenceScore
            )

            lifecycleScope.launch(Dispatchers.IO) {
                database.cancerDao().insertResult(result)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ResultActivity, "Result saved successfully", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "No result to save", Toast.LENGTH_SHORT).show()
        }
    }
}
