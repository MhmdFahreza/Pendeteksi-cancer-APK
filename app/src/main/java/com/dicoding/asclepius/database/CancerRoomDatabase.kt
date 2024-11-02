package com.dicoding.asclepius.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dicoding.asclepius.entity.CancerEntity

@Database(entities = [CancerEntity::class], version = 1)
abstract class CancerRoomDatabase : RoomDatabase() {
    abstract fun cancerDao(): CancerDao
}
