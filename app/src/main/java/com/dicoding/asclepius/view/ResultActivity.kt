package com.dicoding.asclepius.view

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dicoding.asclepius.databinding.ActivityResultBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private lateinit var imageClassifierHelper: ImageClassifierHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi ImageClassifierHelper
        imageClassifierHelper = ImageClassifierHelper(this)

        // Dapatkan URI gambar dari intent
        val imageUriString = intent.getStringExtra("imageUri")
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)

            // Tampilkan gambar pada result_image ImageView
            binding.resultImage.setImageURI(imageUri)

            // Lakukan analisis gambar menggunakan model
            analyzeImage(imageUri)
        }
    }

    private fun analyzeImage(imageUri: Uri) {
        // Gunakan ImageClassifierHelper untuk melakukan klasifikasi gambar
        val result = imageClassifierHelper.classifyStaticImage(imageUri)

        // Tampilkan hasil analisis
        binding.resultText.text = result
    }
}
