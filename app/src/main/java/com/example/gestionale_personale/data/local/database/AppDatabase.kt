package com.example.gestionale_personale.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.gestionale_personale.data.local.dao.EmployeeDao
import com.example.gestionale_personale.data.local.dao.WorkEntryDao
import com.example.gestionale_personale.data.local.entity.EmployeeEntity
import com.example.gestionale_personale.data.local.entity.WorkEntryEntity

@Database(
    entities = [EmployeeEntity::class, WorkEntryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun employeeDao(): EmployeeDao
    abstract fun workEntryDao(): WorkEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ciottis_workers_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
