package com.dicoding.asclepius.view

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
        try {
            val result = imageClassifierHelper.classifyStaticImage(imageUri)
            val resultParts = result.split("\n")
            predictionResult = resultParts[0].replace("Prediksi: ", "")
            confidenceScore = resultParts[1].replace("Confidence Score: ", "")
            binding.resultText.text = result // Tampilkan hasil prediksi dan confidence score
        } catch (e: Exception) {
            e.printStackTrace()
            binding.resultText.text = "Error during image analysis: ${e.message}"
        }
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
