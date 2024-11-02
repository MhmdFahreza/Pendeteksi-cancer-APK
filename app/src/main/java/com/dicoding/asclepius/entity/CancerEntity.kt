package com.dicoding.asclepius.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cancer_results")
data class CancerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imageUri: String,
    val prediction: String,
    val confidence: String
)
