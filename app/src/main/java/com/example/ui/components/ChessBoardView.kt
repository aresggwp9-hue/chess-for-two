package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.example.data.model.BoardThemeType
import com.example.data.model.ChessPiece
import com.example.data.model.MoveRecord
import com.example.data.model.PieceStyle
import com.example.data.model.Square
import kotlin.math.roundToInt

/**
 * Интерактивная шахматная доска 8x8 на Compose Canvas.
 * Поддерживает нажатия, перетаскивание (drag-and-drop), подсветку ходов,
 * координаты a-h / 1-8, переворот доски и режим редактирования.
 */
@Composable
fun ChessBoardView(
    board: Map<Square, ChessPiece>,
    selectedSquare: Square?,
    lastMove: MoveRecord?,
    isFlipped: Boolean,
    boardTheme: BoardThemeType,
    pieceStyle: PieceStyle,
    showCoordinates: Boolean,
    isEditMode: Boolean,
    paletteSelectedPiece: ChessPiece?,
    onSquareClick: (Square) -> Unit,
    onMove: (from: Square, to: Square) -> Unit,
    onRemovePiece: (Square) -> Unit,
    onPlacePiece: (Square, ChessPiece) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .testTag("chess_board_container"),
        contentAlignment = Alignment.Center
    ) {
        val boardSizePx = with(LocalDensity.current) { maxWidth.toPx() }
        val squareSizePx = boardSizePx / 8f
        val squareSizeDp = maxWidth / 8f

        // Состояние перетаскиваемой фигуры
        var draggingFromSquare by remember { mutableStateOf<Square?>(null) }
        var dragOffset by remember { mutableStateOf(Offset.Zero) }

        // Перевод координат экрана в клетку доски
        fun getSquareFromOffset(offset: Offset): Square? {
            if (offset.x < 0 || offset.x >= boardSizePx || offset.y < 0 || offset.y >= boardSizePx) return null
            val colIndex = (offset.x / squareSizePx).toInt().coerceIn(0, 7)
            val rowIndex = (offset.y / squareSizePx).toInt().coerceIn(0, 7)

            val finalCol = if (isFlipped) 7 - colIndex else colIndex
            val finalRow = if (isFlipped) 7 - rowIndex else rowIndex
            return Square(finalCol, finalRow)
        }

        // Позиция клетки на экране в пикселях (TopLeft)
        fun getSquareScreenOffset(square: Square): Offset {
            val visualCol = if (isFlipped) 7 - square.col else square.col
            val visualRow = if (isFlipped) 7 - square.row else square.row
            return Offset(visualCol * squareSizePx, visualRow * squareSizePx)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isFlipped, board, isEditMode, paletteSelectedPiece, selectedSquare) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            val sq = getSquareFromOffset(tapOffset) ?: return@detectTapGestures
                            if (isEditMode && paletteSelectedPiece != null) {
                                onPlacePiece(sq, paletteSelectedPiece)
                            } else {
                                onSquareClick(sq)
                            }
                        },
                        onLongPress = { longPressOffset ->
                            val sq = getSquareFromOffset(longPressOffset) ?: return@detectTapGestures
                            if (board.containsKey(sq)) {
                                onRemovePiece(sq)
                            }
                        }
                    )
                }
                .pointerInput(isFlipped, board, isEditMode) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            if (isEditMode && paletteSelectedPiece != null) return@detectDragGestures
                            val sq = getSquareFromOffset(startOffset)
                            if (sq != null && board.containsKey(sq)) {
                                draggingFromSquare = sq
                                dragOffset = startOffset
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                        },
                        onDragEnd = {
                            val from = draggingFromSquare
                            if (from != null) {
                                val targetSquare = getSquareFromOffset(dragOffset)
                                if (targetSquare != null && targetSquare != from) {
                                    onMove(from, targetSquare)
                                }
                            }
                            draggingFromSquare = null
                            dragOffset = Offset.Zero
                        },
                        onDragCancel = {
                            draggingFromSquare = null
                            dragOffset = Offset.Zero
                        }
                    )
                }
        ) {
            // Отрисовка клеток, координат и подсветок на Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawBoardCanvas(
                    boardTheme = boardTheme,
                    isFlipped = isFlipped,
                    showCoordinates = showCoordinates,
                    squareSize = squareSizePx,
                    selectedSquare = selectedSquare,
                    lastMove = lastMove
                )
            }

            // Отрисовка статических фигур на клетках (кроме той, что сейчас перетаскивается)
            for ((sq, piece) in board) {
                if (sq == draggingFromSquare) continue

                val visualCol = if (isFlipped) 7 - sq.col else sq.col
                val visualRow = if (isFlipped) 7 - sq.row else sq.row
                val xOffset = squareSizeDp * visualCol
                val yOffset = squareSizeDp * visualRow

                val isSelected = sq == selectedSquare
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.12f else 1f,
                    animationSpec = spring(),
                    label = "piece_scale_${sq.algebraic}"
                )

                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .size(squareSizeDp)
                        .scale(scale)
                        .testTag("piece_${sq.algebraic}"),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = PieceDrawableProvider.getDrawableRes(piece, pieceStyle)),
                        contentDescription = "${piece.color.displayNameRu} ${piece.type.letterRu} на ${sq.algebraic}",
                        modifier = Modifier
                            .fillMaxSize(0.88f)
                    )
                }
            }

            // Отрисовка перетаскиваемой фигуры прямо под пальцем
            draggingFromSquare?.let { fromSq ->
                val piece = board[fromSq]
                if (piece != null) {
                    val density = LocalDensity.current
                    val halfSqDp = squareSizeDp / 2
                    val offsetX = with(density) { dragOffset.x.toDp() } - halfSqDp
                    val offsetY = with(density) { dragOffset.y.toDp() } - halfSqDp

                    Box(
                        modifier = Modifier
                            .offset(x = offsetX, y = offsetY)
                            .size(squareSizeDp)
                            .scale(1.22f)
                            .shadow(elevation = 12.dp, shape = MaterialTheme.shapes.small)
                            .testTag("dragging_piece"),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = PieceDrawableProvider.getDrawableRes(piece, pieceStyle)),
                            contentDescription = "Перетаскиваемая фигура",
                            modifier = Modifier.fillMaxSize(0.92f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Отрисовка клеток шахматной доски, подсветок последнего хода, выделения и буквенно-цифровых координат.
 */
private fun DrawScope.drawBoardCanvas(
    boardTheme: BoardThemeType,
    isFlipped: Boolean,
    showCoordinates: Boolean,
    squareSize: Float,
    selectedSquare: Square?,
    lastMove: MoveRecord?
) {
    val textPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        textSize = squareSize * 0.22f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
    }

    for (row in 0 until 8) {
        for (col in 0 until 8) {
            val visualCol = col
            val visualRow = row
            val actualCol = if (isFlipped) 7 - visualCol else visualCol
            val actualRow = if (isFlipped) 7 - visualRow else visualRow
            val currentSq = Square(actualCol, actualRow)

            val isLightSquare = (actualCol + actualRow) % 2 == 0
            val baseColor = if (isLightSquare) boardTheme.lightSquare else boardTheme.darkSquare

            val left = visualCol * squareSize
            val top = visualRow * squareSize

            // Базовый цвет клетки
            drawRect(
                color = baseColor,
                topLeft = Offset(left, top),
                size = Size(squareSize, squareSize)
            )

            // Подсветка последнего хода (откуда и куда)
            if (lastMove != null && (currentSq == lastMove.from || currentSq == lastMove.to)) {
                drawRect(
                    color = boardTheme.highlightColor,
                    topLeft = Offset(left, top),
                    size = Size(squareSize, squareSize)
                )
            }

            // Подсветка выбранной клетки
            if (currentSq == selectedSquare) {
                drawRect(
                    color = boardTheme.selectedColor,
                    topLeft = Offset(left, top),
                    size = Size(squareSize, squareSize)
                )
            }

            // Координаты a-h и 1-8
            if (showCoordinates) {
                val labelColor = if (isLightSquare) boardTheme.lightLabelColor else boardTheme.darkLabelColor
                textPaint.color = android.graphics.Color.argb(
                    (labelColor.alpha * 255).toInt(),
                    (labelColor.red * 255).toInt(),
                    (labelColor.green * 255).toInt(),
                    (labelColor.blue * 255).toInt()
                )

                // Цифры рангов (на левом краю, col == 0)
                if (visualCol == 0) {
                    val rankText = (8 - actualRow).toString()
                    drawContext.canvas.nativeCanvas.drawText(
                        rankText,
                        left + squareSize * 0.08f,
                        top + squareSize * 0.26f,
                        textPaint
                    )
                }

                // Буквы вертикалей (на нижнем краю, row == 7)
                if (visualRow == 7) {
                    val fileText = ('a' + actualCol).toString()
                    drawContext.canvas.nativeCanvas.drawText(
                        fileText,
                        left + squareSize * 0.78f,
                        top + squareSize * 0.92f,
                        textPaint
                    )
                }
            }
        }
    }
}
