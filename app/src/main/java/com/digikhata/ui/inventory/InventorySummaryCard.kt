package com.digikhata.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digikhata.ui.theme.DigiRed
import com.digikhata.util.CurrencyUtils

@Composable
fun InventorySummaryCard(
    itemCount: Int,
    totalValue: Double,
    lowCount: Int,
    currency: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryCell(
                value = itemCount.toString(),
                label = "Items",
                valueColor = MaterialTheme.colorScheme.onSurface
            )
            SummaryCell(
                value = CurrencyUtils.format(totalValue, currency),
                label = "Stock Value",
                valueColor = DigiRed
            )
            SummaryCell(
                value = lowCount.toString(),
                label = "Low",
                valueColor = if (lowCount > 0) DigiRed else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SummaryCell(
    value: String,
    label: String,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
