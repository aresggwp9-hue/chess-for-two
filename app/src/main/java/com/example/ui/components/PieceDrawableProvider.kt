package com.example.ui.components

import androidx.annotation.DrawableRes
import com.example.R
import com.example.data.model.ChessPiece
import com.example.data.model.PieceColor
import com.example.data.model.PieceStyle
import com.example.data.model.PieceType

/**
 * Предоставляет ресурс векторного изображения для фигуры в зависимости от выбранного стиля.
 */
object PieceDrawableProvider {

    @DrawableRes
    fun getDrawableRes(piece: ChessPiece, style: PieceStyle = PieceStyle.CLASSIC): Int {
        val isWhite = piece.color == PieceColor.WHITE
        return when (style) {
            PieceStyle.CLASSIC -> {
                when (piece.type) {
                    PieceType.KING -> if (isWhite) R.drawable.ic_chess_k_w else R.drawable.ic_chess_k_b
                    PieceType.QUEEN -> if (isWhite) R.drawable.ic_chess_q_w else R.drawable.ic_chess_q_b
                    PieceType.ROOK -> if (isWhite) R.drawable.ic_chess_r_w else R.drawable.ic_chess_r_b
                    PieceType.BISHOP -> if (isWhite) R.drawable.ic_chess_b_w else R.drawable.ic_chess_b_b
                    PieceType.KNIGHT -> if (isWhite) R.drawable.ic_chess_n_w else R.drawable.ic_chess_n_b
                    PieceType.PAWN -> if (isWhite) R.drawable.ic_chess_p_w else R.drawable.ic_chess_p_b
                }
            }
            PieceStyle.CONTRAST -> {
                when (piece.type) {
                    PieceType.KING -> if (isWhite) R.drawable.ic_chess_k_w_c else R.drawable.ic_chess_k_b_c
                    PieceType.QUEEN -> if (isWhite) R.drawable.ic_chess_q_w_c else R.drawable.ic_chess_q_b_c
                    PieceType.ROOK -> if (isWhite) R.drawable.ic_chess_r_w_c else R.drawable.ic_chess_r_b_c
                    PieceType.BISHOP -> if (isWhite) R.drawable.ic_chess_b_w_c else R.drawable.ic_chess_b_b_c
                    PieceType.KNIGHT -> if (isWhite) R.drawable.ic_chess_n_w_c else R.drawable.ic_chess_n_b_c
                    PieceType.PAWN -> if (isWhite) R.drawable.ic_chess_p_w_c else R.drawable.ic_chess_p_b_c
                }
            }
        }
    }
}
