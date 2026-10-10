package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.db.GameEntity
import com.example.data.db.MoveEntity
import com.example.data.model.BoardThemeType
import com.example.data.model.BoardUtils
import com.example.data.model.ChessPiece
import com.example.data.model.MoveRecord
import com.example.data.model.PieceColor
import com.example.data.model.PieceStyle
import com.example.data.model.Square
import com.example.ui.components.CapturedPiecesBar
import com.example.ui.components.ChessBoardView
import com.example.ui.components.MoveHistoryStrip
import com.example.ui.viewmodel.HistoryViewModel
import kotlinx.coroutines.delay

/**
 * Экран пошагового просмотра и автовоспроизведения сохранённой партии.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameViewerScreen(
    gameId: Long,
    historyViewModel: HistoryViewModel,
    boardTheme: BoardThemeType,
    pieceStyle: PieceStyle,
    onBackClick: () -> Unit,
    onContinueFromHere: (Long, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var loadedGame by remember { mutableStateOf<GameEntity?>(null) }
    var loadedMoves by remember { mutableStateOf<List<MoveEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Текущий шаг просмотра: -1 означает начальную позицию (до 1-го хода)
    var currentStep by remember { mutableIntStateOf(-1) }
    var isAutoPlaying by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) } // 0.5x, 1x, 2x

    LaunchedEffect(gameId) {
        val result = historyViewModel.getGameWithMoves(gameId)
        if (result != null) {
            loadedGame = result.first
            loadedMoves = result.second
            currentStep = if (result.second.isNotEmpty()) result.second.size - 1 else -1
        }
        isLoading = false
    }

    // Цикл автоматического воспроизведения
    LaunchedEffect(isAutoPlaying, currentStep, playbackSpeed) {
        if (isAutoPlaying) {
            if (currentStep < loadedMoves.size - 1) {
                val delayMs = (1200 / playbackSpeed).toLong()
                delay(delayMs)
                currentStep++
            } else {
                isAutoPlaying = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(loadedGame?.title ?: stringResource(R.string.viewer_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("viewer_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val game = loadedGame
            if (game == null) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("Партия не найдена", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                val scrollState = rememberScrollState()

                // Вычисление текущего состояния доски и съеденных фигур
                val currentBoard: Map<Square, ChessPiece>
                val currentWhiteCap: List<ChessPiece>
                val currentBlackCap: List<ChessPiece>
                val currentLastMove: MoveRecord?

                if (currentStep < 0 || loadedMoves.isEmpty()) {
                    currentBoard = BoardUtils.createInitialBoard()
                    currentWhiteCap = emptyList()
                    currentBlackCap = emptyList()
                    currentLastMove = null
                } else {
                    val m = loadedMoves[currentStep.coerceIn(loadedMoves.indices)]
                    currentBoard = BoardUtils.parseFen(m.fenAfter)
                    currentWhiteCap = BoardUtils.parseCaptured(m.whiteCapturedAfter)
                    currentBlackCap = BoardUtils.parseCaptured(m.blackCapturedAfter)

                    val movingPiece = ChessPiece.fromCodeChar(m.pieceType.first())
                        ?: ChessPiece(com.example.data.model.PieceType.PAWN, PieceColor.WHITE)
                    val fromSq = Square.fromAlgebraic(m.fromSquare) ?: Square(0, 0)
                    val toSq = Square.fromAlgebraic(m.toSquare) ?: Square(0, 0)
                    val capPiece = m.capturedPiece?.let { ChessPiece.fromCodeChar(it.first()) }

                    currentLastMove = MoveRecord(
                        moveNumber = m.moveNumber,
                        piece = movingPiece,
                        from = fromSq,
                        to = toSq,
                        capturedPiece = capPiece,
                        notation = m.notation
                    )
                }

                // Преобразуем loadedMoves в MoveRecord для ленты ходов
                val allMoveRecords = remember(loadedMoves) {
                    loadedMoves.map { m ->
                        val p = ChessPiece.fromCodeChar(m.pieceType.first())
                            ?: ChessPiece(com.example.data.model.PieceType.PAWN, PieceColor.WHITE)
                        MoveRecord(
                            moveNumber = m.moveNumber,
                            piece = p,
                            from = Square.fromAlgebraic(m.fromSquare) ?: Square(0, 0),
                            to = Square.fromAlgebraic(m.toSquare) ?: Square(0, 0),
                            capturedPiece = m.capturedPiece?.let { ChessPiece.fromCodeChar(it.first()) },
                            notation = m.notation,
                            boardSnapshot = BoardUtils.parseFen(m.fenAfter)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Панель информации о ходе
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentStep < 0) "Начальная позиция"
                                else "Ход ${currentStep + 1} из ${loadedMoves.size}: ${currentLastMove?.notation ?: ""}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Переключатель скорости
                            OutlinedButton(
                                onClick = {
                                    playbackSpeed = when (playbackSpeed) {
                                        0.5f -> 1.0f
                                        1.0f -> 2.0f
                                        else -> 0.5f
                                    }
                                },
                                modifier = Modifier.testTag("viewer_speed_btn")
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.viewer_speed, playbackSpeed.toString()), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Черные съеденные
                    CapturedPiecesBar(
                        capturedPieces = currentBlackCap,
                        sideName = stringResource(R.string.captured_black),
                        sideColor = PieceColor.BLACK,
                        pieceStyle = pieceStyle
                    )

                    // Доска
                    ChessBoardView(
                        board = currentBoard,
                        selectedSquare = null,
                        lastMove = currentLastMove,
                        isFlipped = false,
                        boardTheme = boardTheme,
                        pieceStyle = pieceStyle,
                        showCoordinates = true,
                        isEditMode = false,
                        paletteSelectedPiece = null,
                        onSquareClick = { },
                        onMove = { _, _ -> },
                        onRemovePiece = { },
                        onPlacePiece = { _, _ -> },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 480.dp)
                    )

                    // Белые съеденные
                    CapturedPiecesBar(
                        capturedPieces = currentWhiteCap,
                        sideName = stringResource(R.string.captured_white),
                        sideColor = PieceColor.WHITE,
                        pieceStyle = pieceStyle
                    )

                    // Лента ходов
                    MoveHistoryStrip(
                        moves = allMoveRecords,
                        selectedMoveIndex = if (currentStep >= 0) currentStep else null,
                        pieceStyle = pieceStyle,
                        onMoveClick = { index ->
                            isAutoPlaying = false
                            currentStep = index
                        }
                    )

                    // Панель управления воспроизведением (⏮ ◀ ▶/⏸ ▶ ⏭)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                isAutoPlaying = false
                                currentStep = -1
                            },
                            enabled = currentStep >= 0,
                            modifier = Modifier.testTag("viewer_btn_first")
                        ) {
                            Icon(Icons.Default.FastRewind, contentDescription = stringResource(R.string.viewer_start))
                        }

                        FilledTonalIconButton(
                            onClick = {
                                isAutoPlaying = false
                                if (currentStep > -1) currentStep--
                            },
                            enabled = currentStep > -1,
                            modifier = Modifier.testTag("viewer_btn_prev")
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.viewer_step_back))
                        }

                        Button(
                            onClick = { isAutoPlaying = !isAutoPlaying },
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("viewer_btn_play_pause")
                        ) {
                            Icon(
                                if (isAutoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isAutoPlaying) stringResource(R.string.viewer_pause) else stringResource(R.string.viewer_play),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        FilledTonalIconButton(
                            onClick = {
                                isAutoPlaying = false
                                if (currentStep < loadedMoves.size - 1) currentStep++
                            },
                            enabled = currentStep < loadedMoves.size - 1,
                            modifier = Modifier.testTag("viewer_btn_next")
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.viewer_step_forward))
                        }

                        FilledTonalIconButton(
                            onClick = {
                                isAutoPlaying = false
                                currentStep = loadedMoves.size - 1
                            },
                            enabled = currentStep < loadedMoves.size - 1,
                            modifier = Modifier.testTag("viewer_btn_last")
                        ) {
                            Icon(Icons.Default.FastForward, contentDescription = stringResource(R.string.viewer_end))
                        }
                    }

                    // Кнопка продолжить игру с этой позиции
                    Button(
                        onClick = { onContinueFromHere(game.id, currentStep) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("viewer_btn_continue_game")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_continue_game))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
