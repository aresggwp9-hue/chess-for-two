package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ChessDatabase
import com.example.data.model.BoardThemeType
import com.example.data.model.BoardUtils
import com.example.data.model.ChessPiece
import com.example.data.model.MoveRecord
import com.example.data.model.PieceColor
import com.example.data.model.PieceStyle
import com.example.data.model.Square
import com.example.data.repository.ChessRepository
import com.example.data.sound.ChessSoundPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Основная ViewModel для управления партией в шахматы.
 */
class ChessViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChessRepository(ChessDatabase.getInstance(application))
    private val soundPlayer = ChessSoundPlayer(application)

    private val _uiState = MutableStateFlow(ChessGameState())
    val uiState: StateFlow<ChessGameState> = _uiState.asStateFlow()

    init {
        restoreAutoSavedGame()
    }

    /**
     * Восстанавливает последнюю автосохранённую партию при старте.
     */
    private fun restoreAutoSavedGame() {
        viewModelScope.launch {
            val autoSaved = repository.getAutoSavedGame()
            if (autoSaved != null) {
                val game = autoSaved.first
                val movesEntities = autoSaved.second
                val restoredBoard = BoardUtils.parseFen(game.currentFen)
                val whiteCap = BoardUtils.parseCaptured(game.whiteCaptured)
                val blackCap = BoardUtils.parseCaptured(game.blackCaptured)

                val restoredMoves = movesEntities.map { entity ->
                    MoveRecord(
                        moveNumber = entity.moveNumber,
                        piece = ChessPiece.fromCodeChar(entity.pieceType.first()) ?: ChessPiece(com.example.data.model.PieceType.PAWN, PieceColor.WHITE),
                        from = Square.fromAlgebraic(entity.fromSquare) ?: Square(0, 0),
                        to = Square.fromAlgebraic(entity.toSquare) ?: Square(0, 0),
                        capturedPiece = entity.capturedPiece?.let { ChessPiece.fromCodeChar(it.first()) },
                        notation = entity.notation,
                        boardSnapshot = BoardUtils.parseFen(entity.fenAfter),
                        whiteCapturedSnapshot = BoardUtils.parseCaptured(entity.whiteCapturedAfter),
                        blackCapturedSnapshot = BoardUtils.parseCaptured(entity.blackCapturedAfter),
                        turnAfter = if (entity.isWhite) PieceColor.BLACK else PieceColor.WHITE
                    )
                }

                _uiState.update { current ->
                    current.copy(
                        board = restoredBoard,
                        moves = restoredMoves,
                        turn = if (game.currentTurnWhite) PieceColor.WHITE else PieceColor.BLACK,
                        whiteCaptured = whiteCap,
                        blackCaptured = blackCap
                    )
                }
            }
        }
    }

    /**
     * Обработка нажатия на клетку шахматной доски.
     */
    fun onSquareClick(square: Square) {
        val state = _uiState.value

        // Если активен режим просмотра предыдущего хода, нажатие возвращает к текущей позиции
        if (state.previewMoveIndex != null) {
            cancelPreview()
            return
        }

        val selected = state.selectedSquare
        if (selected == null) {
            // Выделяем фигуру, если она есть на клетке
            if (state.board.containsKey(square)) {
                _uiState.update { it.copy(selectedSquare = square) }
            }
        } else {
            if (selected == square) {
                // Повторное нажатие — снятие выделения
                _uiState.update { it.copy(selectedSquare = null) }
            } else {
                // Свободный ход на любую клетку!
                makeMove(from = selected, to = square)
                _uiState.update { it.copy(selectedSquare = null) }
            }
        }
    }

    /**
     * Выполнение хода. В режиме песочницы любая фигура может переместиться на любую клетку.
     * При занятости клетки фигура съедается вне зависимости от цвета.
     */
    fun makeMove(from: Square, to: Square) {
        val state = _uiState.value
        val movingPiece = state.board[from] ?: return

        // Выход из режима предпросмотра хода, если был открыт
        val currentBoard = state.board
        val targetPiece = currentBoard[to]
        val isCapture = targetPiece != null

        val newBoard = currentBoard.toMutableMap()
        newBoard.remove(from)
        newBoard[to] = movingPiece

        val newWhiteCaptured = state.whiteCaptured.toMutableList()
        val newBlackCaptured = state.blackCaptured.toMutableList()

        if (targetPiece != null) {
            if (targetPiece.color == PieceColor.WHITE) {
                newWhiteCaptured.add(targetPiece)
            } else {
                newBlackCaptured.add(targetPiece)
            }
        }

        val nextTurn = if (state.autoSwitchTurn) {
            if (movingPiece.color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        } else {
            state.turn
        }

        val moveNumber = state.moves.size + 1
        val capSymbol = if (isCapture) "×" else "→"
        val notation = "$moveNumber. ${movingPiece.type.letterRu} ${from.algebraic} $capSymbol ${to.algebraic}"

        val moveRecord = MoveRecord(
            moveNumber = moveNumber,
            piece = movingPiece,
            from = from,
            to = to,
            capturedPiece = targetPiece,
            notation = notation,
            boardSnapshot = newBoard,
            whiteCapturedSnapshot = newWhiteCaptured,
            blackCapturedSnapshot = newBlackCaptured,
            turnAfter = nextTurn
        )

        val updatedMoves = state.moves + moveRecord

        _uiState.update {
            it.copy(
                board = newBoard,
                selectedSquare = null,
                turn = nextTurn,
                moves = updatedMoves,
                redoStack = emptyList(), // сброс стека возврата при новом ходе
                whiteCaptured = newWhiteCaptured,
                blackCaptured = newBlackCaptured,
                previewMoveIndex = null,
                previewBoard = null
            )
        }

        // Звуковая и вибро-отдача
        soundPlayer.playMoveFeedback(
            isCapture = isCapture,
            soundEnabled = state.soundEnabled,
            vibrationEnabled = state.vibrationEnabled
        )

        // Автосохранение
        scheduleAutoSave()
    }

    /**
     * Отмена последнего хода (Undo).
     */
    fun undoMove() {
        val state = _uiState.value
        if (state.moves.isEmpty()) return

        val lastMove = state.moves.last()
        val remainingMoves = state.moves.dropLast(1)
        val newRedo = state.redoStack + lastMove

        // Восстановление позиции
        val (restoredBoard, whiteCap, blackCap, restoredTurn) = if (remainingMoves.isNotEmpty()) {
            val prev = remainingMoves.last()
            Tuple4(prev.boardSnapshot, prev.whiteCapturedSnapshot, prev.blackCapturedSnapshot, prev.turnAfter)
        } else {
            Tuple4(BoardUtils.createInitialBoard(), emptyList(), emptyList(), PieceColor.WHITE)
        }

        _uiState.update {
            it.copy(
                board = restoredBoard,
                moves = remainingMoves,
                redoStack = newRedo,
                whiteCaptured = whiteCap,
                blackCaptured = blackCap,
                turn = restoredTurn,
                selectedSquare = null,
                previewMoveIndex = null,
                previewBoard = null
            )
        }
        scheduleAutoSave()
    }

    /**
     * Повтор отменённого хода (Redo).
     */
    fun redoMove() {
        val state = _uiState.value
        if (state.redoStack.isEmpty()) return

        val moveToRedo = state.redoStack.last()
        val newRedo = state.redoStack.dropLast(1)
        val updatedMoves = state.moves + moveToRedo

        _uiState.update {
            it.copy(
                board = moveToRedo.boardSnapshot,
                moves = updatedMoves,
                redoStack = newRedo,
                whiteCaptured = moveToRedo.whiteCapturedSnapshot,
                blackCaptured = moveToRedo.blackCapturedSnapshot,
                turn = moveToRedo.turnAfter,
                selectedSquare = null,
                previewMoveIndex = null,
                previewBoard = null
            )
        }
        scheduleAutoSave()
    }

    /**
     * Ручное переключение подсказки стороны хода.
     */
    fun toggleTurn() {
        _uiState.update { it.copy(turn = it.turn.opposite()) }
        scheduleAutoSave()
    }

    /**
     * Переворот доски на 180 градусов.
     */
    fun flipBoard() {
        _uiState.update { it.copy(isFlipped = !it.isFlipped) }
    }

    /**
     * Начало новой партии (сброс к стандартной позиции).
     */
    fun newGame() {
        _uiState.update {
            it.copy(
                board = BoardUtils.createInitialBoard(),
                selectedSquare = null,
                turn = PieceColor.WHITE,
                moves = emptyList(),
                redoStack = emptyList(),
                whiteCaptured = emptyList(),
                blackCaptured = emptyList(),
                previewMoveIndex = null,
                previewBoard = null
            )
        }
        scheduleAutoSave()
    }

    /**
     * Переключение режима редактирования / свободной расстановки.
     */
    fun toggleEditMode() {
        _uiState.update { it.copy(isEditMode = !it.isEditMode, paletteSelectedPiece = null) }
    }

    fun setPalettePiece(piece: ChessPiece?) {
        _uiState.update { it.copy(paletteSelectedPiece = piece) }
    }

    /**
     * Установка фигуры из палитры на клетку.
     */
    fun placePiece(square: Square, piece: ChessPiece) {
        val newBoard = _uiState.value.board.toMutableMap()
        newBoard[square] = piece
        _uiState.update { it.copy(board = newBoard) }
        scheduleAutoSave()
    }

    /**
     * Удаление фигуры с клетки (по долгому нажатию или в режиме редактирования).
     */
    fun removePiece(square: Square) {
        val currentBoard = _uiState.value.board
        if (currentBoard.containsKey(square)) {
            val newBoard = currentBoard.toMutableMap()
            newBoard.remove(square)
            _uiState.update { it.copy(board = newBoard, selectedSquare = null) }
            soundPlayer.playMoveFeedback(
                isCapture = false,
                soundEnabled = _uiState.value.soundEnabled,
                vibrationEnabled = _uiState.value.vibrationEnabled
            )
            scheduleAutoSave()
        }
    }

    /**
     * Очистка всей доски (в режиме расстановки).
     */
    fun clearBoard() {
        _uiState.update {
            it.copy(
                board = emptyMap(),
                selectedSquare = null,
                whiteCaptured = emptyList(),
                blackCaptured = emptyList()
            )
        }
        scheduleAutoSave()
    }

    /**
     * Расстановка начальной классической позиции на доске.
     */
    fun resetToInitialBoard() {
        _uiState.update {
            it.copy(
                board = BoardUtils.createInitialBoard(),
                selectedSquare = null,
                whiteCaptured = emptyList(),
                blackCaptured = emptyList()
            )
        }
        scheduleAutoSave()
    }

    /**
     * Просмотр позиции на момент конкретного хода в ленте истории.
     */
    fun previewMove(index: Int) {
        val state = _uiState.value
        if (index in state.moves.indices) {
            val targetMove = state.moves[index]
            _uiState.update {
                it.copy(
                    previewMoveIndex = index,
                    previewBoard = targetMove.boardSnapshot
                )
            }
        }
    }

    /**
     * Продолжить игру от просматриваемого хода (отсекает последующие ходы).
     */
    fun continueFromPreview() {
        val state = _uiState.value
        val index = state.previewMoveIndex ?: return
        if (index in state.moves.indices) {
            val targetMove = state.moves[index]
            val truncatedMoves = state.moves.subList(0, index + 1)
            _uiState.update {
                it.copy(
                    board = targetMove.boardSnapshot,
                    moves = truncatedMoves,
                    redoStack = emptyList(),
                    whiteCaptured = targetMove.whiteCapturedSnapshot,
                    blackCaptured = targetMove.blackCapturedSnapshot,
                    turn = targetMove.turnAfter,
                    previewMoveIndex = null,
                    previewBoard = null,
                    selectedSquare = null
                )
            }
            scheduleAutoSave()
        }
    }

    /**
     * Возврат к текущему живому состоянию игры без отката ходов.
     */
    fun cancelPreview() {
        _uiState.update {
            it.copy(
                previewMoveIndex = null,
                previewBoard = null
            )
        }
    }

    /**
     * Сохранение партии в базу данных Room с заданным названием.
     */
    fun saveGame(titleInput: String) {
        viewModelScope.launch {
            val title = if (titleInput.isBlank()) {
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                "Партия от ${dateFormat.format(Date())}"
            } else {
                titleInput.trim()
            }

            val state = _uiState.value
            repository.saveGame(
                title = title,
                moves = state.moves,
                currentBoard = state.board,
                currentTurn = state.turn,
                whiteCaptured = state.whiteCaptured,
                blackCaptured = state.blackCaptured
            )

            _uiState.update { it.copy(snackbarMessage = "Партия «$title» сохранена!") }
        }
    }

    /**
     * Загружает сохранённую партию и устанавливает её как текущую для продолжения игры.
     */
    fun loadGameToPlay(gameId: Long) {
        viewModelScope.launch {
            val pair = repository.getGameWithMoves(gameId) ?: return@launch
            val game = pair.first
            val movesEntities = pair.second

            val loadedBoard = BoardUtils.parseFen(game.currentFen)
            val whiteCap = BoardUtils.parseCaptured(game.whiteCaptured)
            val blackCap = BoardUtils.parseCaptured(game.blackCaptured)

            val loadedMoves = movesEntities.map { entity ->
                MoveRecord(
                    moveNumber = entity.moveNumber,
                    piece = ChessPiece.fromCodeChar(entity.pieceType.first()) ?: ChessPiece(com.example.data.model.PieceType.PAWN, PieceColor.WHITE),
                    from = Square.fromAlgebraic(entity.fromSquare) ?: Square(0, 0),
                    to = Square.fromAlgebraic(entity.toSquare) ?: Square(0, 0),
                    capturedPiece = entity.capturedPiece?.let { ChessPiece.fromCodeChar(it.first()) },
                    notation = entity.notation,
                    boardSnapshot = BoardUtils.parseFen(entity.fenAfter),
                    whiteCapturedSnapshot = BoardUtils.parseCaptured(entity.whiteCapturedAfter),
                    blackCapturedSnapshot = BoardUtils.parseCaptured(entity.blackCapturedAfter),
                    turnAfter = if (entity.isWhite) PieceColor.BLACK else PieceColor.WHITE
                )
            }

            _uiState.update {
                it.copy(
                    board = loadedBoard,
                    moves = loadedMoves,
                    redoStack = emptyList(),
                    turn = if (game.currentTurnWhite) PieceColor.WHITE else PieceColor.BLACK,
                    whiteCaptured = whiteCap,
                    blackCaptured = blackCap,
                    previewMoveIndex = null,
                    previewBoard = null,
                    selectedSquare = null
                )
            }
            scheduleAutoSave()
        }
    }

    fun setBoardTheme(theme: BoardThemeType) {
        _uiState.update { it.copy(boardTheme = theme) }
    }

    fun setPieceStyle(style: PieceStyle) {
        _uiState.update { it.copy(pieceStyle = style) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        _uiState.update { it.copy(soundEnabled = enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(vibrationEnabled = enabled) }
    }

    fun setShowCoordinates(enabled: Boolean) {
        _uiState.update { it.copy(showCoordinates = enabled) }
    }

    fun setAutoSwitchTurn(enabled: Boolean) {
        _uiState.update { it.copy(autoSwitchTurn = enabled) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun scheduleAutoSave() {
        viewModelScope.launch {
            val state = _uiState.value
            repository.autoSaveCurrentGame(
                moves = state.moves,
                currentBoard = state.board,
                currentTurn = state.turn,
                whiteCaptured = state.whiteCaptured,
                blackCaptured = state.blackCaptured
            )
        }
    }

    private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
