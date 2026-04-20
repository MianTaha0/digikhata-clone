package com.digikhata.util

object CurrencyUtils {
    fun format(amount: Double, currency: String): String {
        val symbol = when {
            currency.contains("Pakistan Rupee", ignoreCase = true) -> "Rs"
            currency.startsWith("Rs", ignoreCase = true) -> "Rs"
            currency.contains("-") -> currency.substringBefore("-")
            else -> currency
        }
        val formatted = if (amount % 1.0 == 0.0) {
            String.format("%,.0f", amount)
        } else {
            String.format("%,.2f", amount)
        }
        return "$symbol $formatted"
    }
}
