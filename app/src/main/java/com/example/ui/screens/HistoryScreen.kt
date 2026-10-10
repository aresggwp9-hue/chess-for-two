package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.db.GameEntity
import com.example.ui.viewmodel.HistoryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Экран истории сохранённых партий.
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onContinueGame: (Long) -> Unit,
    onViewGame: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val games by viewModel.games.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var renameInput by remember { mutableStateOf("") }
    var importInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Верхняя строка с заголовком и кнопкой импорта
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.history_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedButton(
                    onClick = {
                        importInput = ""
                        viewModel.openImportDialog()
                    },
                    modifier = Modifier.testTag("btn_import_game")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_import))
                }
            }

            // Поисковая строка
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text(stringResource(R.string.history_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Список партий
            if (games.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = stringResource(R.string.history_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("saved_games_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(games, key = { it.id }) { game ->
                        GameItemCard(
                            game = game,
                            onContinue = { onContinueGame(game.id) },
                            onView = { onViewGame(game.id) },
                            onRename = {
                                renameInput = game.title
                                viewModel.openRenameDialog(game)
                            },
                            onDelete = { viewModel.openDeleteDialog(game) },
                            onShare = {
                                coroutineScope.launch {
                                    val json = viewModel.getExportJson(game.id)
                                    if (json != null) {
                                        shareGameJson(context, game.title, json)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    // Диалог переименования
    if (uiState.showRenameDialog && uiState.selectedGameForAction != null) {
        val game = uiState.selectedGameForAction!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissRenameDialog() },
            title = { Text(stringResource(R.string.dialog_rename_title)) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_game_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.renameGame(game.id, renameInput) },
                    modifier = Modifier.testTag("confirm_rename_btn")
                ) {
                    Text(stringResource(R.string.dialog_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRenameDialog() }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    // Диалог подтверждения удаления
    if (uiState.showDeleteDialog && uiState.selectedGameForAction != null) {
        val game = uiState.selectedGameForAction!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = { Text("Удалить партию «${game.title}»? ${stringResource(R.string.dialog_delete_desc)}") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteGame(game.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_game_btn")
                ) {
                    Text(stringResource(R.string.dialog_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    // Диалог импорта партии из JSON
    if (uiState.showImportDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportDialog() },
            title = { Text(stringResource(R.string.import_dialog_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.import_paste_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importInput,
                        onValueChange = { importInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("import_json_input"),
                        placeholder = { Text("{\"version\":1, \"title\":\"...\"}") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.importGame(importInput) },
                    enabled = importInput.isNotBlank(),
                    modifier = Modifier.testTag("confirm_import_btn")
                ) {
                    Text(stringResource(R.string.btn_import))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissImportDialog() }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

/**
 * Карточка сохранённой партии в списке.
 */
@Composable
private fun GameItemCard(
    game: GameEntity,
    onContinue: () -> Unit,
    onView: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(game.updatedAt) { dateFormat.format(Date(game.updatedAt)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("game_card_${game.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$formattedDate  •  ${stringResource(R.string.history_moves_count, game.moveCount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(36.dp).testTag("btn_share_game_${game.id}")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Поделиться", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onRename,
                        modifier = Modifier.size(36.dp).testTag("btn_rename_game_${game.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Переименовать", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp).testTag("btn_delete_game_${game.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onView,
                    modifier = Modifier.weight(1f).testTag("btn_view_game_${game.id}")
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_view_game), style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onContinue,
                    modifier = Modifier.weight(1.2f).testTag("btn_continue_game_${game.id}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_continue_game), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun shareGameJson(context: Context, title: String, json: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, json)
        putExtra(Intent.EXTRA_TITLE, "Шахматная партия: $title")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Поделиться партией")
    context.startActivity(shareIntent)
}
