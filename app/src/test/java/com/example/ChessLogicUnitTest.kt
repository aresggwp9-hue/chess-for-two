package com.example

import com.example.data.model.BoardUtils
import com.example.data.model.ChessPiece
import com.example.data.model.PieceColor
import com.example.data.model.PieceType
import com.example.data.model.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessLogicUnitTest {

    @Test
    fun testInitialBoardHas32Pieces() {
        val board = BoardUtils.createInitialBoard()
        assertEquals(32, board.size)

        // Проверяем ключевые фигуры
        val e1 = Square.fromAlgebraic("e1")
        assertNotNull(e1)
        val whiteKing = board[e1]
        assertNotNull(whiteKing)
        assertEquals(PieceType.KING, whiteKing?.type)
        assertEquals(PieceColor.WHITE, whiteKing?.color)

        val e8 = Square.fromAlgebraic("e8")
        assertNotNull(e8)
        val blackKing = board[e8]
        assertNotNull(blackKing)
        assertEquals(PieceType.KING, blackKing?.type)
        assertEquals(PieceColor.BLACK, blackKing?.color)
    }

    @Test
    fun testFenRoundTrip() {
        val initialBoard = BoardUtils.createInitialBoard()
        val fen = BoardUtils.toFen(initialBoard)
        assertEquals(BoardUtils.INITIAL_FEN, fen)

        val parsedBoard = BoardUtils.parseFen(fen)
        assertEquals(initialBoard.size, parsedBoard.size)
        for ((sq, piece) in initialBoard) {
            val parsedPiece = parsedBoard[sq]
            assertNotNull(parsedPiece)
            assertEquals(piece.type, parsedPiece?.type)
            assertEquals(piece.color, parsedPiece?.color)
        }
    }

    @Test
    fun testSquareAlgebraic() {
        assertEquals("a1", Square(0, 7).algebraic)
        assertEquals("h8", Square(7, 0).algebraic)
        assertEquals("e4", Square(4, 4).algebraic)

        val sq = Square.fromAlgebraic("d5")
        assertNotNull(sq)
        assertEquals(3, sq?.col)
        assertEquals(3, sq?.row)
    }

    @Test
    fun testCapturedSerialization() {
        val pieces = listOf(
            ChessPiece(PieceType.QUEEN, PieceColor.WHITE),
            ChessPiece(PieceType.PAWN, PieceColor.BLACK)
        )
        val serialized = BoardUtils.serializeCaptured(pieces)
        assertEquals("Qp", serialized)

        val deserialized = BoardUtils.parseCaptured(serialized)
        assertEquals(2, deserialized.size)
        assertEquals(PieceType.QUEEN, deserialized[0].type)
        assertEquals(PieceColor.WHITE, deserialized[0].color)
        assertEquals(PieceType.PAWN, deserialized[1].type)
        assertEquals(PieceColor.BLACK, deserialized[1].color)
    }
}
