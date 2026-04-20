package com.digikhata.domain.model

data class CashTotals(
    val totalIn: Double = 0.0,
    val totalOut: Double = 0.0
) {
    val net: Double get() = totalIn - totalOut
}
