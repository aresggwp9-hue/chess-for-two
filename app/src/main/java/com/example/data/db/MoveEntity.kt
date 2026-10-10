package com.example.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность отдельного хода партии.
 */
@Entity(
    tableName = "moves",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["gameId"])]
)
data class MoveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val moveNumber: Int,
    val pieceType: String,
    val isWhite: Boolean,
    val fromSquare: String,
    val toSquare: String,
    val capturedPiece: String? = null,
    val notation: String,
    val fenAfter: String,
    val whiteCapturedAfter: String = "",
    val blackCapturedAfter: String = ""
)
