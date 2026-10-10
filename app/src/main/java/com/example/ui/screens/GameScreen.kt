package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PieceColor
import com.example.ui.components.CapturedPiecesBar
import com.example.ui.components.ChessBoardView
import com.example.ui.components.MoveHistoryStrip
import com.example.ui.components.PiecePalette
import com.example.ui.viewmodel.ChessGameState
import com.example.ui.viewmodel.ChessViewModel

/**
 * Главный экран свободной игры в шахматы для двоих.
 */
@Composable
fun GameScreen(
    viewModel: ChessViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showNewGameDialog by remember { mutableStateOf(false) }
    var showSaveGameDialog by remember { mutableStateOf(false) }
    var saveTitleInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isLandscape) {
            GameScreenLandscape(
                state = state,
                viewModel = viewModel,
                onNewGameClick = { showNewGameDialog = true },
                onSaveGameClick = {
                    saveTitleInput = ""
                    showSaveGameDialog = true
                }
            )
        } else {
            GameScreenPortrait(
                state = state,
                viewModel = viewModel,
                onNewGameClick = { showNewGameDialog = true },
                onSaveGameClick = {
                    saveTitleInput = ""
                    showSaveGameDialog = true
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    // Диалог подтверждения новой партии
    if (showNewGameDialog) {
        AlertDialog(
            onDismissRequest = { showNewGameDialog = false },
            title = { Text(stringResource(R.string.dialog_new_game_title)) },
            text = { Text(stringResource(R.string.dialog_new_game_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.newGame()
                        showNewGameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_new_game_btn")
                ) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGameDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    // Диалог сохранения партии
    if (showSaveGameDialog) {
        AlertDialog(
            onDismissRequest = { showSaveGameDialog = false },
            title = { Text(stringResource(R.string.dialog_save_game_title)) },
            text = {
                Column {
                    Text(
                        text = "Введите название партии или оставьте пустым для названия с текущей датой:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = saveTitleInput,
                        onValueChange = { saveTitleInput = it },
                        placeholder = { Text(stringResource(R.string.dialog_save_game_hint)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_game_title_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveGame(saveTitleInput)
                        showSaveGameDialog = false
                    },
                    modifier = Modifier.testTag("confirm_save_game_btn")
                ) {
                    Text(stringResource(R.string.dialog_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveGameDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

/**
 * Портретная компоновка экрана игры.
 */
@Composable
private fun GameScreenPortrait(
    state: ChessGameState,
    viewModel: ChessViewModel,
    onNewGameClick: () -> Unit,
    onSaveGameClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Верхняя панель: индикатор хода и быстрая кнопка переворота
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TurnIndicator(
                turn = state.turn,
                onToggleTurn = { viewModel.toggleTurn() }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { viewModel.flipBoard() },
                    modifier = Modifier.testTag("btn_flip_board")
                ) {
                    Icon(
                        Icons.Default.SwapVert,
                        contentDescription = stringResource(R.string.btn_flip),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleEditMode() },
                    modifier = Modifier.testTag("btn_toggle_edit")
                ) {
                    Icon(
                        if (state.isEditMode) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = stringResource(R.string.btn_edit_board),
                        tint = if (state.isEditMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Если активен предпросмотр хода — плашка с кнопками
        if (state.previewMoveIndex != null) {
            PreviewBanner(
                previewIndex = state.previewMoveIndex + 1,
                onContinue = { viewModel.continueFromPreview() },
                onCancel = { viewModel.cancelPreview() }
            )
        }

        // Съеденные фигуры противника (сверху)
        val topCaptured = if (state.isFlipped) state.whiteCaptured else state.blackCaptured
        val topColor = if (state.isFlipped) PieceColor.WHITE else PieceColor.BLACK
        val topTitle = if (state.isFlipped) stringResource(R.string.captured_white) else stringResource(R.string.captured_black)

        CapturedPiecesBar(
            capturedPieces = topCaptured,
            sideName = topTitle,
            sideColor = topColor,
            pieceStyle = state.pieceStyle
        )

        // Шахматная доска 8x8
        ChessBoardView(
            board = state.activeBoard,
            selectedSquare = state.selectedSquare,
            lastMove = state.lastMove,
            isFlipped = state.isFlipped,
            boardTheme = state.boardTheme,
            pieceStyle = state.pieceStyle,
            showCoordinates = state.showCoordinates,
            isEditMode = state.isEditMode,
            paletteSelectedPiece = state.paletteSelectedPiece,
            onSquareClick = { viewModel.onSquareClick(it) },
            onMove = { from, to -> viewModel.makeMove(from, to) },
            onRemovePiece = { viewModel.removePiece(it) },
            onPlacePiece = { sq, p -> viewModel.placePiece(sq, p) },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
        )

        // Съеденные фигуры свои (снизу)
        val bottomCaptured = if (state.isFlipped) state.blackCaptured else state.whiteCaptured
        val bottomColor = if (state.isFlipped) PieceColor.BLACK else PieceColor.WHITE
        val bottomTitle = if (state.isFlipped) stringResource(R.string.captured_black) else stringResource(R.string.captured_white)

        CapturedPiecesBar(
            capturedPieces = bottomCaptured,
            sideName = bottomTitle,
            sideColor = bottomColor,
            pieceStyle = state.pieceStyle
        )

        // Лента истории ходов
        MoveHistoryStrip(
            moves = state.moves,
            selectedMoveIndex = state.previewMoveIndex,
            pieceStyle = state.pieceStyle,
            onMoveClick = { viewModel.previewMove(it) }
        )

        // Палитра редактирования (при включённом режиме)
        if (state.isEditMode) {
            PiecePalette(
                selectedPiece = state.paletteSelectedPiece,
                pieceStyle = state.pieceStyle,
                onSelectPiece = { viewModel.setPalettePiece(it) },
                onClearBoard = { viewModel.clearBoard() },
                onResetToInitial = { viewModel.resetToInitialBoard() }
            )
        }

        // Панель основных действий: Отмена, Возврат, Новая, Сохранить
        GameControlButtons(
            canUndo = state.canUndo,
            canRedo = state.canRedo,
            onUndo = { viewModel.undoMove() },
            onRedo = { viewModel.redoMove() },
            onNewGame = onNewGameClick,
            onSaveGame = onSaveGameClick
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Альбомная компоновка экрана игры.
 */
@Composable
private fun GameScreenLandscape(
    state: ChessGameState,
    viewModel: ChessViewModel,
    onNewGameClick: () -> Unit,
    onSaveGameClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Слева: доска
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1.1f),
            contentAlignment = Alignment.Center
        ) {
            ChessBoardView(
                board = state.activeBoard,
                selectedSquare = state.selectedSquare,
                lastMove = state.lastMove,
                isFlipped = state.isFlipped,
                boardTheme = state.boardTheme,
                pieceStyle = state.pieceStyle,
                showCoordinates = state.showCoordinates,
                isEditMode = state.isEditMode,
                paletteSelectedPiece = state.paletteSelectedPiece,
                onSquareClick = { viewModel.onSquareClick(it) },
                onMove = { from, to -> viewModel.makeMove(from, to) },
                onRemovePiece = { viewModel.removePiece(it) },
                onPlacePiece = { sq, p -> viewModel.placePiece(sq, p) },
                modifier = Modifier.fillMaxHeight()
            )
        }

        // Справа: управление, лента ходов, съеденные
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.9f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TurnIndicator(
                    turn = state.turn,
                    onToggleTurn = { viewModel.toggleTurn() }
                )

                Row {
                    IconButton(onClick = { viewModel.flipBoard() }) {
                        Icon(Icons.Default.SwapVert, contentDescription = "Перевернуть")
                    }
                    IconButton(onClick = { viewModel.toggleEditMode() }) {
                        Icon(
                            if (state.isEditMode) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = "Расстановка"
                        )
                    }
                }
            }

            if (state.previewMoveIndex != null) {
                PreviewBanner(
                    previewIndex = state.previewMoveIndex + 1,
                    onContinue = { viewModel.continueFromPreview() },
                    onCancel = { viewModel.cancelPreview() }
                )
            }

            CapturedPiecesBar(
                capturedPieces = state.blackCaptured,
                sideName = stringResource(R.string.captured_black),
                sideColor = PieceColor.BLACK,
                pieceStyle = state.pieceStyle
            )

            CapturedPiecesBar(
                capturedPieces = state.whiteCaptured,
                sideName = stringResource(R.string.captured_white),
                sideColor = PieceColor.WHITE,
                pieceStyle = state.pieceStyle
            )

            MoveHistoryStrip(
                moves = state.moves,
                selectedMoveIndex = state.previewMoveIndex,
                pieceStyle = state.pieceStyle,
                onMoveClick = { viewModel.previewMove(it) }
            )

            if (state.isEditMode) {
                PiecePalette(
                    selectedPiece = state.paletteSelectedPiece,
                    pieceStyle = state.pieceStyle,
                    onSelectPiece = { viewModel.setPalettePiece(it) },
                    onClearBoard = { viewModel.clearBoard() },
                    onResetToInitial = { viewModel.resetToInitialBoard() }
                )
            }

            GameControlButtons(
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                onUndo = { viewModel.undoMove() },
                onRedo = { viewModel.redoMove() },
                onNewGame = onNewGameClick,
                onSaveGame = onSaveGameClick
            )
        }
    }
}

/**
 * Индикатор хода (чей ход) с возможностью ручного нажатия/переключения.
 */
@Composable
private fun TurnIndicator(
    turn: PieceColor,
    onToggleTurn: () -> Unit
) {
    val isWhite = turn == PieceColor.WHITE
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        modifier = Modifier
            .clickable { onToggleTurn() }
            .testTag("turn_indicator_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        if (isWhite) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.primary
                    )
            )
            Column {
                Text(
                    text = if (isWhite) stringResource(R.string.turn_white) else stringResource(R.string.turn_black),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.tap_to_toggle_turn),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * Плашка активного режима просмотра хода.
 */
@Composable
private fun PreviewBanner(
    previewIndex: Int,
    onContinue: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.preview_move_title, previewIndex),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("btn_cancel_preview")
                ) {
                    Text(stringResource(R.string.btn_return_to_current), style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = onContinue,
                    modifier = Modifier.testTag("btn_continue_from_here")
                ) {
                    Text(stringResource(R.string.btn_continue_from_here), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/**
 * Блок кнопок управления партией.
 */
@Composable
private fun GameControlButtons(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onNewGame: () -> Unit,
    onSaveGame: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = onUndo,
            enabled = canUndo,
            modifier = Modifier
                .weight(1f)
                .testTag("btn_undo_move")
        ) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.btn_undo), style = MaterialTheme.typography.labelMedium)
        }

        FilledTonalButton(
            onClick = onRedo,
            enabled = canRedo,
            modifier = Modifier
                .weight(1f)
                .testTag("btn_redo_move")
        ) {
            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.btn_redo), style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
            onClick = onNewGame,
            modifier = Modifier
                .weight(1f)
                .testTag("btn_new_game")
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.btn_new_game), style = MaterialTheme.typography.labelMedium)
        }

        Button(
            onClick = onSaveGame,
            modifier = Modifier
                .weight(1f)
                .testTag("btn_save_game")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.btn_save_game), style = MaterialTheme.typography.labelMedium)
        }
    }
}
