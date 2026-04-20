package com.digikhata.ui.expense

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.ui.graphics.vector.ImageVector

enum class PaymentMethod(val key: String, val label: String, val icon: ImageVector) {
    CASH("cash", "Cash", Icons.Default.Payments),
    BANK("bank", "Bank", Icons.Default.AccountBalance),
    CARD("card", "Card", Icons.Default.CreditCard),
    DIGITAL("digital", "Digital", Icons.Default.Smartphone);

    companion object {
        fun fromKey(key: String): PaymentMethod = values().firstOrNull { it.key == key } ?: CASH
    }
}
