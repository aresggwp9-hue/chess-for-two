package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.MoveRecord
import com.example.data.model.PieceStyle

/**
 * Лента истории ходов под доской.
 */
@Composable
fun MoveHistoryStrip(
    moves: List<MoveRecord>,
    selectedMoveIndex: Int?,
    pieceStyle: PieceStyle,
    onMoveClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Автоматическая прокрутка к последнему ходу при добавлении
    LaunchedEffect(moves.size, selectedMoveIndex) {
        if (selectedMoveIndex != null && selectedMoveIndex in moves.indices) {
            listState.animateScrollToItem(selectedMoveIndex)
        } else if (moves.isNotEmpty()) {
            listState.animateScrollToItem(moves.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (moves.isEmpty()) {
            Text(
                text = stringResource(id = R.string.no_moves_yet),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        } else {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("move_history_list")
            ) {
                itemsIndexed(moves) { index, move ->
                    val isSelected = selectedMoveIndex == index
                    val isCapture = move.capturedPiece != null

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onMoveClick(index) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("move_item_$index"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${move.moveNumber}.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.primary
                            )
                        )

                        Image(
                            painter = painterResource(id = PieceDrawableProvider.getDrawableRes(move.piece, pieceStyle)),
                            contentDescription = move.piece.type.letterRu,
                            modifier = Modifier.size(18.dp)
                        )

                        Text(
                            text = "${move.from.algebraic} ${if (isCapture) "×" else "→"} ${move.to.algebraic}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                        )

                        if (move.capturedPiece != null) {
                            Image(
                                painter = painterResource(id = PieceDrawableProvider.getDrawableRes(move.capturedPiece, pieceStyle)),
                                contentDescription = "Взята",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
