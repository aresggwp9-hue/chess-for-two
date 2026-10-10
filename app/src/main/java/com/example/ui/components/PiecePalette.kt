package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChessPiece
import com.example.data.model.PieceColor
import com.example.data.model.PieceStyle
import com.example.data.model.PieceType

/**
 * Палитра фигур для режима редактирования/расстановки.
 */
@Composable
fun PiecePalette(
    selectedPiece: ChessPiece?,
    pieceStyle: PieceStyle,
    onSelectPiece: (ChessPiece) -> Unit,
    onClearBoard: () -> Unit,
    onResetToInitial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val whitePieces = listOf(
        ChessPiece(PieceType.KING, PieceColor.WHITE),
        ChessPiece(PieceType.QUEEN, PieceColor.WHITE),
        ChessPiece(PieceType.ROOK, PieceColor.WHITE),
        ChessPiece(PieceType.BISHOP, PieceColor.WHITE),
        ChessPiece(PieceType.KNIGHT, PieceColor.WHITE),
        ChessPiece(PieceType.PAWN, PieceColor.WHITE)
    )

    val blackPieces = listOf(
        ChessPiece(PieceType.KING, PieceColor.BLACK),
        ChessPiece(PieceType.QUEEN, PieceColor.BLACK),
        ChessPiece(PieceType.ROOK, PieceColor.BLACK),
        ChessPiece(PieceType.BISHOP, PieceColor.BLACK),
        ChessPiece(PieceType.KNIGHT, PieceColor.BLACK),
        ChessPiece(PieceType.PAWN, PieceColor.BLACK)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Палитра для расстановки (выберите фигуру):",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Белые фигуры
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (p in whitePieces) {
                val isSelected = selectedPiece?.type == p.type && selectedPiece.color == p.color
                PalettePieceItem(
                    piece = p,
                    isSelected = isSelected,
                    pieceStyle = pieceStyle,
                    onClick = { onSelectPiece(p) }
                )
            }
        }

        // Чёрные фигуры
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (p in blackPieces) {
                val isSelected = selectedPiece?.type == p.type && selectedPiece.color == p.color
                PalettePieceItem(
                    piece = p,
                    isSelected = isSelected,
                    pieceStyle = pieceStyle,
                    onClick = { onSelectPiece(p) }
                )
            }
        }

        // Кнопки управления доской
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onClearBoard,
                modifier = Modifier
                    .weight(1f)
                    .testTag("palette_clear_board"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Очистить", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
                onClick = onResetToInitial,
                modifier = Modifier
                    .weight(1f)
                    .testTag("palette_reset_initial")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Начальная", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun PalettePieceItem(
    piece: ChessPiece,
    isSelected: Boolean,
    pieceStyle: PieceStyle,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .testTag("palette_piece_${piece.codeChar}"),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = PieceDrawableProvider.getDrawableRes(piece, pieceStyle)),
            contentDescription = "${piece.color.displayNameRu} ${piece.type.letterRu}",
            modifier = Modifier.size(34.dp)
        )
    }
}
