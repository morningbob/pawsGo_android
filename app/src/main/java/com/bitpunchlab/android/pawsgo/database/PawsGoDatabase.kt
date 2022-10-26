package com.bitpunchlab.android.pawsgo.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bitpunchlab.android.pawsgo.modelsRoom.UserRoom
import kotlinx.coroutines.InternalCoroutinesApi

@Database(entities = [UserRoom::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PawsGoDatabase : RoomDatabase() {
    abstract val pawsDAO: PawsDAO

    companion object {
        @Volatile
        private var INSTANCE: PawsGoDatabase? = null

        @InternalCoroutinesApi
        fun getInstance(context: Context?): PawsGoDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context!!.applicationContext,
                        PawsGoDatabase::class.java,
                        "pawsGo_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()

                    INSTANCE = instance
                }

                return instance
            }
        }
    }

}