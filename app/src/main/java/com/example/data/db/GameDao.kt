package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM games WHERE isAutoSaved = 0 ORDER BY updatedAt DESC")
    fun getAllSavedGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE isAutoSaved = 0 AND title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchGames(query: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    suspend fun getGameById(id: Long): GameEntity?

    @Query("SELECT * FROM games WHERE isAutoSaved = 1 LIMIT 1")
    suspend fun getAutoSavedGame(): GameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity): Long

    @Update
    suspend fun updateGame(game: GameEntity)

    @Delete
    suspend fun deleteGame(game: GameEntity)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGameById(id: Long)

    @Query("DELETE FROM games WHERE isAutoSaved = 1")
    suspend fun deleteAutoSavedGame()
}
