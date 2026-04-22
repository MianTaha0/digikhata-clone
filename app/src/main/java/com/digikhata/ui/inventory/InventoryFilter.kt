package com.digikhata.ui.inventory

import com.digikhata.data.entity.Product

enum class InventoryFilter(val label: String) {
    ALL("All"),
    LOW("Low"),
    OUT("Out");

    fun matches(product: Product): Boolean = when (this) {
        ALL -> true
        LOW -> product.lowStockThreshold > 0 &&
                product.quantity <= product.lowStockThreshold &&
                product.quantity > 0
        OUT -> product.quantity <= 0
    }
}
