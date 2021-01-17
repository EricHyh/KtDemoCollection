package com.hyh.paging3demo.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hyh.paging3demo.bean.ProjectBean
import com.hyh.paging3demo.bean.ProjectRemoteKey

@Database(
    entities = [ProjectBean::class, ProjectRemoteKey::class],
    version = 1,
    exportSchema = false
)
abstract class ProjectDB : RoomDatabase() {

    companion object {
        private val LOCK = Any()
        private var instance: ProjectDB? = null
        fun get(context: Context): ProjectDB {
            if (instance != null) return instance!!
            synchronized(LOCK) {
                if (instance == null) {
                    instance = create(context)
                }
                return instance!!
            }
        }

        private fun create(context: Context, useInMemory: Boolean = false): ProjectDB {
            val databaseBuilder = if (useInMemory) {
                Room.inMemoryDatabaseBuilder(context, ProjectDB::class.java)
            } else {
                Room.databaseBuilder(context, ProjectDB::class.java, "project.db")
            }
            return databaseBuilder
                .fallbackToDestructiveMigration()
                .build()
        }
    }

    abstract fun projects(): ProjectDao
    abstract fun remoteKeys(): ProjectRemoteKeyDao
}