package com.dicoding.asclepius.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.dicoding.asclepius.entity.CancerEntity

@Dao
interface CancerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: CancerEntity)
}
