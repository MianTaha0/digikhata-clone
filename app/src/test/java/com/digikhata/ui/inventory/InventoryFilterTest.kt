package com.digikhata.ui.inventory

import com.digikhata.data.entity.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryFilterTest {

    private fun product(qty: Double, threshold: Double = 0.0) = Product(
        id = 1,
        businessId = 1,
        name = "x",
        costPrice = 0.0,
        sellPrice = 0.0,
        quantity = qty,
        lowStockThreshold = threshold
    )

    @Test
    fun `ALL matches every product`() {
        assertTrue(InventoryFilter.ALL.matches(product(0.0)))
        assertTrue(InventoryFilter.ALL.matches(product(5.0, 2.0)))
        assertTrue(InventoryFilter.ALL.matches(product(-1.0)))
    }

    @Test
    fun `OUT matches zero or negative quantity`() {
        assertTrue(InventoryFilter.OUT.matches(product(0.0)))
        assertTrue(InventoryFilter.OUT.matches(product(-1.0)))
        assertFalse(InventoryFilter.OUT.matches(product(0.5)))
    }

    @Test
    fun `LOW requires positive threshold`() {
        // threshold 0 means no tracking
        assertFalse(InventoryFilter.LOW.matches(product(1.0, 0.0)))
    }

    @Test
    fun `LOW matches when quantity is between zero and threshold inclusive`() {
        assertTrue(InventoryFilter.LOW.matches(product(qty = 3.0, threshold = 5.0)))
        assertTrue(InventoryFilter.LOW.matches(product(qty = 5.0, threshold = 5.0)))
    }

    @Test
    fun `LOW excludes out-of-stock`() {
        assertFalse(InventoryFilter.LOW.matches(product(qty = 0.0, threshold = 5.0)))
    }

    @Test
    fun `LOW excludes above-threshold`() {
        assertFalse(InventoryFilter.LOW.matches(product(qty = 10.0, threshold = 5.0)))
    }

    @Test
    fun `filters partition product list cleanly`() {
        val products = listOf(
            product(qty = 0.0, threshold = 5.0),    // OUT
            product(qty = 3.0, threshold = 5.0),    // LOW
            product(qty = 10.0, threshold = 5.0),   // neither
            product(qty = 5.0, threshold = 0.0),    // untracked, in stock
        )
        assertEquals(4, products.count(InventoryFilter.ALL::matches))
        assertEquals(1, products.count(InventoryFilter.OUT::matches))
        assertEquals(1, products.count(InventoryFilter.LOW::matches))
    }
}
