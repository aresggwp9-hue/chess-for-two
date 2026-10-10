package com.example.data.model

import androidx.compose.ui.graphics.Color

/**
 * Стили отображения фигур.
 */
enum class PieceStyle(val displayNameRu: String) {
    CLASSIC("Классический"),
    CONTRAST("Контрастный")
}

/**
 * Темы оформления шахматной доски.
 */
enum class BoardThemeType(
    val displayNameRu: String,
    val lightSquare: Color,
    val darkSquare: Color,
    val lightLabelColor: Color,
    val darkLabelColor: Color,
    val highlightColor: Color,
    val selectedColor: Color
) {
    WOOD(
        displayNameRu = "Классическое дерево",
        lightSquare = Color(0xFFF0D9B5),
        darkSquare = Color(0xFFB58863),
        lightLabelColor = Color(0xFFB58863),
        darkLabelColor = Color(0xFFF0D9B5),
        highlightColor = Color(0x66CDD26A),
        selectedColor = Color(0x99F6F669)
    ),
    TOURNAMENT(
        displayNameRu = "Турнирная зелёная",
        lightSquare = Color(0xFFEEEED2),
        darkSquare = Color(0xFF769656),
        lightLabelColor = Color(0xFF769656),
        darkLabelColor = Color(0xFFEEEED2),
        highlightColor = Color(0x66F5F682),
        selectedColor = Color(0x99BACA44)
    ),
    DARK(
        displayNameRu = "Тёмный графит",
        lightSquare = Color(0xFFCED3DC),
        darkSquare = Color(0xFF4A5568),
        lightLabelColor = Color(0xFF4A5568),
        darkLabelColor = Color(0xFFCED3DC),
        highlightColor = Color(0x6663B3ED),
        selectedColor = Color(0x994299E1)
    ),
    BLUE(
        displayNameRu = "Морская синяя",
        lightSquare = Color(0xFFDEE3E6),
        darkSquare = Color(0xFF4A7499),
        lightLabelColor = Color(0xFF4A7499),
        darkLabelColor = Color(0xFFDEE3E6),
        highlightColor = Color(0x6681E6D9),
        selectedColor = Color(0x99319795)
    )
}
