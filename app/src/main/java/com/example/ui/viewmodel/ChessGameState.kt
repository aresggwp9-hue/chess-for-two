package com.example.ui.viewmodel

import com.example.data.model.BoardThemeType
import com.example.data.model.BoardUtils
import com.example.data.model.ChessPiece
import com.example.data.model.MoveRecord
import com.example.data.model.PieceColor
import com.example.data.model.PieceStyle
import com.example.data.model.Square

/**
 * Полное состояние текущей игры и настроек.
 */
data class ChessGameState(
    val board: Map<Square, ChessPiece> = BoardUtils.createInitialBoard(),
    val selectedSquare: Square? = null,
    val turn: PieceColor = PieceColor.WHITE,
    val moves: List<MoveRecord> = emptyList(),
    val redoStack: List<MoveRecord> = emptyList(),
    val whiteCaptured: List<ChessPiece> = emptyList(),
    val blackCaptured: List<ChessPiece> = emptyList(),
    val isFlipped: Boolean = false,
    val isEditMode: Boolean = false,
    val paletteSelectedPiece: ChessPiece? = null,
    val previewMoveIndex: Int? = null,
    val previewBoard: Map<Square, ChessPiece>? = null,
    val boardTheme: BoardThemeType = BoardThemeType.WOOD,
    val pieceStyle: PieceStyle = PieceStyle.CLASSIC,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val showCoordinates: Boolean = true,
    val autoSwitchTurn: Boolean = true,
    val snackbarMessage: String? = null
) {
    val activeBoard: Map<Square, ChessPiece>
        get() = previewBoard ?: board

    val lastMove: MoveRecord?
        get() = if (previewMoveIndex != null && previewMoveIndex in moves.indices) {
            moves[previewMoveIndex]
        } else {
            moves.lastOrNull()
        }

    val canUndo: Boolean get() = moves.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
}
