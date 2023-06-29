package com.hyh.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory


/**
 * TODO: Add Description
 *
 * @author eriche 2021/11/10
 */
class SqliteActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SqliteActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val passphrase: ByteArray = SQLiteDatabase.getBytes("userEnteredPassphrase".toCharArray())
        val factory = SupportFactory(passphrase)
        /*val room: SomeDatabase = Room.databaseBuilder(this, SomeDatabase::class.java, "DB_NAME")
            .openHelperFactory(factory)
            .build()*/
    }

}


/*
class SomeDatabase : RoomDatabase() {

    override fun createOpenHelper(config: DatabaseConfiguration?): SupportSQLiteOpenHelper {
        TODO("Not yet implemented")
    }

    override fun createInvalidationTracker(): InvalidationTracker {
        TODO("Not yet implemented")
    }

    override fun clearAllTables() {
        TODO("Not yet implemented")
    }

}*/
