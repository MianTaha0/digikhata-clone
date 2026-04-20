package com.digikhata.ui.book

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ColorSwatchRow(
    selectedHex: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DIGI_BOOK_COLORS.forEach { hex ->
            val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
            val selected = hex.equals(selectedHex, ignoreCase = true)
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(c)
                    .border(
                        BorderStroke(if (selected) 3.dp else 1.dp, if (selected) Color.Black else Color.LightGray),
                        CircleShape
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}
