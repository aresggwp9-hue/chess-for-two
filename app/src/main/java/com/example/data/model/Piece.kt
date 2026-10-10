package com.example.data.model

import java.util.UUID

/**
 * Тип шахматной фигуры.
 */
enum class PieceType(val letterRu: String, val symbolWhite: String, val symbolBlack: String) {
    KING("Кр", "♔", "♚"),
    QUEEN("Ф", "♕", "♛"),
    ROOK("Л", "♖", "♜"),
    BISHOP("С", "♗", "♝"),
    KNIGHT("К", "♘", "♞"),
    PAWN("п", "♙", "♟")
}

/**
 * Цвет шахматной фигуры.
 */
enum class PieceColor(val displayNameRu: String) {
    WHITE("Белые"),
    BLACK("Чёрные");

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

/**
 * Модель шахматной фигуры.
 */
data class ChessPiece(
    val type: PieceType,
    val color: PieceColor,
    val id: String = UUID.randomUUID().toString()
) {
    val isWhite: Boolean get() = color == PieceColor.WHITE

    /**
     * Однобуквенное обозначение для FEN/сериализации.
     * Заглавная — белые, строчная — чёрные.
     */
    val codeChar: Char
        get() {
            val c = when (type) {
                PieceType.KING -> 'K'
                PieceType.QUEEN -> 'Q'
                PieceType.ROOK -> 'R'
                PieceType.BISHOP -> 'B'
                PieceType.KNIGHT -> 'N'
                PieceType.PAWN -> 'P'
            }
            return if (color == PieceColor.WHITE) c else c.lowercaseChar()
        }

    val symbol: String
        get() = if (color == PieceColor.WHITE) type.symbolWhite else type.symbolBlack

    companion object {
        fun fromCodeChar(c: Char): ChessPiece? {
            val isWhite = c.isUpperCase()
            val color = if (isWhite) PieceColor.WHITE else PieceColor.BLACK
            val type = when (c.uppercaseChar()) {
                'K' -> PieceType.KING
                'Q' -> PieceType.QUEEN
                'R' -> PieceType.ROOK
                'B' -> PieceType.BISHOP
                'N' -> PieceType.KNIGHT
                'P' -> PieceType.PAWN
                else -> return null
            }
            return ChessPiece(type = type, color = color)
        }
    }
}

/**
 * Клетка шахматной доски (col: 0..7 => 'a'..'h', row: 0..7 => 8..1).
 */
data class Square(val col: Int, val row: Int) {
    init {
        require(col in 0..7 && row in 0..7) { "Координаты клетки вне диапазона 8x8: ($col, $row)" }
    }

    val algebraic: String
        get() = "${('a' + col)}${8 - row}"

    val isLight: Boolean
        get() = (col + row) % 2 == 0

    companion object {
        fun fromAlgebraic(str: String): Square? {
            if (str.length != 2) return null
            val col = str[0].lowercaseChar() - 'a'
            val rank = str[1].digitToIntOrNull() ?: return null
            val row = 8 - rank
            if (col !in 0..7 || row !in 0..7) return null
            return Square(col, row)
        }
    }
}

/**
 * Запись одного хода в истории партии.
 */
data class MoveRecord(
    val moveNumber: Int,
    val piece: ChessPiece,
    val from: Square,
    val to: Square,
    val capturedPiece: ChessPiece? = null,
    val notation: String = "",
    val boardSnapshot: Map<Square, ChessPiece> = emptyMap(),
    val whiteCapturedSnapshot: List<ChessPiece> = emptyList(),
    val blackCapturedSnapshot: List<ChessPiece> = emptyList(),
    val turnAfter: PieceColor = PieceColor.WHITE
)
