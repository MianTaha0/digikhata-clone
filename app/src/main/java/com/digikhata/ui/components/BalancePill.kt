package com.digikhata.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digikhata.ui.theme.DigiError
import com.digikhata.ui.theme.DigiGreen
import com.digikhata.util.CurrencyUtils
import kotlin.math.abs

@Composable
fun BalancePill(
    balance: Double,
    currency: String,
    type: Int = 0, // 0=customer, 1=supplier
    modifier: Modifier = Modifier
) {
    // For customers: balance > 0 = they owe you (green "will get"). < 0 = you owe (red "will give").
    // For suppliers: balance > 0 = they owe you (green "will receive"). < 0 = you owe (red "will pay").
    val isPositive = balance >= 0
    val color = if (isPositive) DigiGreen else DigiError
    val label = when {
        balance == 0.0 -> "Settled"
        isPositive && type == 0 -> "You will get"
        isPositive && type == 1 -> "You will receive"
        !isPositive && type == 0 -> "You will give"
        else -> "You will pay"
    }
    val amount = CurrencyUtils.format(abs(balance), currency)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(text = label, color = color.copy(alpha = 0.85f), fontSize = 11.sp)
        Text(text = amount, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
