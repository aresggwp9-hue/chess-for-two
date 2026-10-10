package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ChessDatabase
import com.example.data.db.GameEntity
import com.example.data.db.MoveEntity
import com.example.data.repository.ChessRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Состояние экрана истории сохранённых партий.
 */
data class HistoryUiState(
    val searchQuery: String = "",
    val selectedGameForAction: GameEntity? = null,
    val showRenameDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val snackbarMessage: String? = null
)

/**
 * ViewModel для экрана сохранённых игр.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChessRepository(ChessDatabase.getInstance(application))

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    val games: StateFlow<List<GameEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllGames()
            } else {
                repository.searchGames(query.trim())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openRenameDialog(game: GameEntity) {
        _uiState.update { it.copy(selectedGameForAction = game, showRenameDialog = true) }
    }

    fun dismissRenameDialog() {
        _uiState.update { it.copy(selectedGameForAction = null, showRenameDialog = false) }
    }

    fun renameGame(gameId: Long, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            repository.renameGame(gameId, newTitle.trim())
            _uiState.update {
                it.copy(
                    selectedGameForAction = null,
                    showRenameDialog = false,
                    snackbarMessage = "Партия переименована"
                )
            }
        }
    }

    fun openDeleteDialog(game: GameEntity) {
        _uiState.update { it.copy(selectedGameForAction = game, showDeleteDialog = true) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(selectedGameForAction = null, showDeleteDialog = false) }
    }

    fun deleteGame(gameId: Long) {
        viewModelScope.launch {
            repository.deleteGame(gameId)
            _uiState.update {
                it.copy(
                    selectedGameForAction = null,
                    showDeleteDialog = false,
                    snackbarMessage = "Партия удалена"
                )
            }
        }
    }

    fun openImportDialog() {
        _uiState.update { it.copy(showImportDialog = true) }
    }

    fun dismissImportDialog() {
        _uiState.update { it.copy(showImportDialog = false) }
    }

    fun importGame(jsonString: String) {
        viewModelScope.launch {
            try {
                repository.importGameFromJson(jsonString.trim())
                _uiState.update {
                    it.copy(
                        showImportDialog = false,
                        snackbarMessage = "Партия успешно импортирована!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(snackbarMessage = "Ошибка импорта: неверный формат JSON")
                }
            }
        }
    }

    suspend fun getExportJson(gameId: Long): String? {
        return repository.exportGameToJson(gameId)
    }

    suspend fun getGameWithMoves(gameId: Long): Pair<GameEntity, List<MoveEntity>>? {
        return repository.getGameWithMoves(gameId)
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
