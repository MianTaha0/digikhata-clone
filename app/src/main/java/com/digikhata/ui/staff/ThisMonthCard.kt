package com.digikhata.ui.staff

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digikhata.ui.theme.DigiGreen
import com.digikhata.ui.theme.DigiRed
import com.digikhata.util.CurrencyUtils

@Composable
fun ThisMonthCard(
    monthlySalary: Double,
    paidThisMonth: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    val due = (monthlySalary - paidThisMonth).coerceAtLeast(0.0)
    val dueColor = if (due <= 0) DigiGreen else DigiRed
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Cell(
                value = CurrencyUtils.format(monthlySalary, currency),
                label = "Monthly salary",
                valueColor = MaterialTheme.colorScheme.onSurface
            )
            Cell(
                value = CurrencyUtils.format(paidThisMonth, currency),
                label = "Paid this month",
                valueColor = MaterialTheme.colorScheme.onSurface
            )
            Cell(
                value = CurrencyUtils.format(due, currency),
                label = "Due",
                valueColor = dueColor
            )
        }
    }
}

@Composable
private fun Cell(value: String, label: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
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
