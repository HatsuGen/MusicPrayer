package com.musicprayer.vibematch.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TrackEntity::class], version = 4, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun tracks(): TrackDao

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracks ADD COLUMN artworkUri TEXT")
                db.execSQL("ALTER TABLE tracks ADD COLUMN artworkModifiedMs INTEGER")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracks ADD COLUMN albumArtist TEXT")
                db.execSQL("ALTER TABLE tracks ADD COLUMN albumSourceId TEXT")
                db.execSQL("ALTER TABLE tracks ADD COLUMN discNumber INTEGER")
                db.execSQL("ALTER TABLE tracks ADD COLUMN trackNumber INTEGER")
            }
        }

        fun create(context: Context) = Room.databaseBuilder(
            context, MusicDatabase::class.java, "vibematch.db"
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigrationFrom(true, 1)
            .build()
    }
}
