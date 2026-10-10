package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MoveDao {

    @Query("SELECT * FROM moves WHERE gameId = :gameId ORDER BY moveNumber ASC")
    suspend fun getMovesForGame(gameId: Long): List<MoveEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoves(moves: List<MoveEntity>)

    @Query("DELETE FROM moves WHERE gameId = :gameId")
    suspend fun deleteMovesForGame(gameId: Long)
}
