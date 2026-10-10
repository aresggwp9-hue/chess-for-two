package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [GameEntity::class, MoveEntity::class], version = 1, exportSchema = false)
abstract class ChessDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao
    abstract fun moveDao(): MoveDao

    companion object {
        @Volatile
        private var INSTANCE: ChessDatabase? = null

        fun getInstance(context: Context): ChessDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChessDatabase::class.java,
                    "chess_for_two.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
