package com.example.data.model

/**
 * Утилиты для работы с состоянием доски и стандартной нотацией FEN.
 */
object BoardUtils {

    const val INITIAL_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"

    /**
     * Создает стандартную начальную расстановку 8x8.
     */
    fun createInitialBoard(): Map<Square, ChessPiece> {
        return parseFen(INITIAL_FEN)
    }

    /**
     * Парсит позицию из FEN (только поле расположения фигур).
     */
    fun parseFen(fen: String): Map<Square, ChessPiece> {
        val board = mutableMapOf<Square, ChessPiece>()
        val ranks = fen.trim().split(" ")[0].split("/")
        if (ranks.size != 8) return board

        for (row in 0 until 8) {
            val rankStr = ranks[row]
            var col = 0
            for (char in rankStr) {
                if (char.isDigit()) {
                    col += char.digitToInt()
                } else {
                    val piece = ChessPiece.fromCodeChar(char)
                    if (piece != null && col in 0..7) {
                        board[Square(col, row)] = piece
                    }
                    col++
                }
            }
        }
        return board
    }

    /**
     * Сериализует позицию фигур на доске в строку FEN.
     */
    fun toFen(board: Map<Square, ChessPiece>): String {
        val sb = StringBuilder()
        for (row in 0 until 8) {
            var emptyCount = 0
            for (col in 0 until 8) {
                val piece = board[Square(col, row)]
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(piece.codeChar)
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount)
            }
            if (row < 7) {
                sb.append('/')
            }
        }
        return sb.toString()
    }

    /**
     * Сериализует список съеденных фигур в строку символов.
     */
    fun serializeCaptured(pieces: List<ChessPiece>): String {
        return pieces.joinToString("") { it.codeChar.toString() }
    }

    /**
     * Восстанавливает список съеденных фигур из строки.
     */
    fun parseCaptured(str: String): List<ChessPiece> {
        return str.mapNotNull { ChessPiece.fromCodeChar(it) }
    }
}
