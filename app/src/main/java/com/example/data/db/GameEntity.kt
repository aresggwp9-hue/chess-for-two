package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность сохранённой партии в базе данных Room.
 */
@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val moveCount: Int = 0,
    val currentTurnWhite: Boolean = true,
    val currentFen: String,
    val whiteCaptured: String = "",
    val blackCaptured: String = "",
    val isAutoSaved: Boolean = false
)
