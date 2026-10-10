package com.example.data.repository

import com.example.data.db.ChessDatabase
import com.example.data.db.GameEntity
import com.example.data.db.MoveEntity
import com.example.data.model.BoardUtils
import com.example.data.model.ChessPiece
import com.example.data.model.MoveRecord
import com.example.data.model.PieceColor
import com.example.data.model.Square
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Репозиторий для управления сохранением и загрузкой партий.
 */
class ChessRepository(private val database: ChessDatabase) {

    private val gameDao = database.gameDao()
    private val moveDao = database.moveDao()

    fun getAllGames(): Flow<List<GameEntity>> = gameDao.getAllSavedGames()

    fun searchGames(query: String): Flow<List<GameEntity>> = gameDao.searchGames(query)

    suspend fun saveGame(
        title: String,
        moves: List<MoveRecord>,
        currentBoard: Map<Square, ChessPiece>,
        currentTurn: PieceColor,
        whiteCaptured: List<ChessPiece>,
        blackCaptured: List<ChessPiece>
    ): Long = withContext(Dispatchers.IO) {
        val gameEntity = GameEntity(
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            moveCount = moves.size,
            currentTurnWhite = currentTurn == PieceColor.WHITE,
            currentFen = BoardUtils.toFen(currentBoard),
            whiteCaptured = BoardUtils.serializeCaptured(whiteCaptured),
            blackCaptured = BoardUtils.serializeCaptured(blackCaptured),
            isAutoSaved = false
        )
        val gameId = gameDao.insertGame(gameEntity)

        val moveEntities = moves.mapIndexed { index, move ->
            MoveEntity(
                gameId = gameId,
                moveNumber = index + 1,
                pieceType = move.piece.codeChar.toString(),
                isWhite = move.piece.isWhite,
                fromSquare = move.from.algebraic,
                toSquare = move.to.algebraic,
                capturedPiece = move.capturedPiece?.codeChar?.toString(),
                notation = move.notation,
                fenAfter = BoardUtils.toFen(move.boardSnapshot),
                whiteCapturedAfter = BoardUtils.serializeCaptured(move.whiteCapturedSnapshot),
                blackCapturedAfter = BoardUtils.serializeCaptured(move.blackCapturedSnapshot)
            )
        }
        moveDao.insertMoves(moveEntities)
        gameId
    }

    suspend fun autoSaveCurrentGame(
        moves: List<MoveRecord>,
        currentBoard: Map<Square, ChessPiece>,
        currentTurn: PieceColor,
        whiteCaptured: List<ChessPiece>,
        blackCaptured: List<ChessPiece>
    ) = withContext(Dispatchers.IO) {
        val existing = gameDao.getAutoSavedGame()
        val gameEntity = GameEntity(
            id = existing?.id ?: 0,
            title = "Текущая партия",
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            moveCount = moves.size,
            currentTurnWhite = currentTurn == PieceColor.WHITE,
            currentFen = BoardUtils.toFen(currentBoard),
            whiteCaptured = BoardUtils.serializeCaptured(whiteCaptured),
            blackCaptured = BoardUtils.serializeCaptured(blackCaptured),
            isAutoSaved = true
        )
        val gameId = gameDao.insertGame(gameEntity)

        moveDao.deleteMovesForGame(gameId)
        val moveEntities = moves.mapIndexed { index, move ->
            MoveEntity(
                gameId = gameId,
                moveNumber = index + 1,
                pieceType = move.piece.codeChar.toString(),
                isWhite = move.piece.isWhite,
                fromSquare = move.from.algebraic,
                toSquare = move.to.algebraic,
                capturedPiece = move.capturedPiece?.codeChar?.toString(),
                notation = move.notation,
                fenAfter = BoardUtils.toFen(move.boardSnapshot),
                whiteCapturedAfter = BoardUtils.serializeCaptured(move.whiteCapturedSnapshot),
                blackCapturedAfter = BoardUtils.serializeCaptured(move.blackCapturedSnapshot)
            )
        }
        moveDao.insertMoves(moveEntities)
    }

    suspend fun getAutoSavedGame(): Pair<GameEntity, List<MoveEntity>>? = withContext(Dispatchers.IO) {
        val game = gameDao.getAutoSavedGame() ?: return@withContext null
        val moves = moveDao.getMovesForGame(game.id)
        Pair(game, moves)
    }

    suspend fun getGameWithMoves(gameId: Long): Pair<GameEntity, List<MoveEntity>>? = withContext(Dispatchers.IO) {
        val game = gameDao.getGameById(gameId) ?: return@withContext null
        val moves = moveDao.getMovesForGame(gameId)
        Pair(game, moves)
    }

    suspend fun renameGame(gameId: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val game = gameDao.getGameById(gameId) ?: return@withContext
        gameDao.updateGame(game.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteGame(gameId: Long) = withContext(Dispatchers.IO) {
        moveDao.deleteMovesForGame(gameId)
        gameDao.deleteGameById(gameId)
    }

    /**
     * Экспорт партии в JSON строку.
     */
    suspend fun exportGameToJson(gameId: Long): String? = withContext(Dispatchers.IO) {
        val pair = getGameWithMoves(gameId) ?: return@withContext null
        val game = pair.first
        val moves = pair.second

        val json = JSONObject()
        json.put("version", 1)
        json.put("title", game.title)
        json.put("createdAt", game.createdAt)
        json.put("currentFen", game.currentFen)
        json.put("currentTurnWhite", game.currentTurnWhite)
        json.put("whiteCaptured", game.whiteCaptured)
        json.put("blackCaptured", game.blackCaptured)

        val movesArray = JSONArray()
        for (m in moves) {
            val mObj = JSONObject()
            mObj.put("moveNumber", m.moveNumber)
            mObj.put("pieceType", m.pieceType)
            mObj.put("isWhite", m.isWhite)
            mObj.put("from", m.fromSquare)
            mObj.put("to", m.toSquare)
            mObj.put("capturedPiece", m.capturedPiece ?: "")
            mObj.put("notation", m.notation)
            mObj.put("fenAfter", m.fenAfter)
            mObj.put("whiteCapturedAfter", m.whiteCapturedAfter)
            mObj.put("blackCapturedAfter", m.blackCapturedAfter)
            movesArray.put(mObj)
        }
        json.put("moves", movesArray)
        json.toString(2)
    }

    /**
     * Импорт партии из JSON строки.
     */
    suspend fun importGameFromJson(jsonString: String): Long = withContext(Dispatchers.IO) {
        val json = JSONObject(jsonString)
        val title = json.optString("title", "Импортированная партия")
        val currentFen = json.optString("currentFen", BoardUtils.INITIAL_FEN)
        val currentTurnWhite = json.optBoolean("currentTurnWhite", true)
        val whiteCaptured = json.optString("whiteCaptured", "")
        val blackCaptured = json.optString("blackCaptured", "")

        val movesArray = json.optJSONArray("moves") ?: JSONArray()
        val gameEntity = GameEntity(
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            moveCount = movesArray.length(),
            currentTurnWhite = currentTurnWhite,
            currentFen = currentFen,
            whiteCaptured = whiteCaptured,
            blackCaptured = blackCaptured,
            isAutoSaved = false
        )
        val gameId = gameDao.insertGame(gameEntity)

        val moveEntities = mutableListOf<MoveEntity>()
        for (i in 0 until movesArray.length()) {
            val m = movesArray.getJSONObject(i)
            moveEntities.add(
                MoveEntity(
                    gameId = gameId,
                    moveNumber = m.optInt("moveNumber", i + 1),
                    pieceType = m.optString("pieceType", "P"),
                    isWhite = m.optBoolean("isWhite", true),
                    fromSquare = m.optString("from", ""),
                    toSquare = m.optString("to", ""),
                    capturedPiece = m.optString("capturedPiece").ifEmpty { null },
                    notation = m.optString("notation", ""),
                    fenAfter = m.optString("fenAfter", currentFen),
                    whiteCapturedAfter = m.optString("whiteCapturedAfter", ""),
                    blackCapturedAfter = m.optString("blackCapturedAfter", "")
                )
            )
        }
        moveDao.insertMoves(moveEntities)
        gameId
    }
}
