package com.digikhata.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.data.entity.Product
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repo: DigiRepository,
    private val active: ActiveBookHolder
) : ViewModel() {

    val filter: MutableStateFlow<InventoryFilter> = MutableStateFlow(InventoryFilter.ALL)

    val currency: StateFlow<String> = active.active
        .map { it?.currency ?: "Pakistan Rupee-Rs" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Pakistan Rupee-Rs")

    val activeBookId: StateFlow<Long?> = active.id
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val allProducts: StateFlow<List<Product>> = active.id
        .flatMapLatest { bid ->
            if (bid == null) flowOf(emptyList()) else repo.products(bid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val products: StateFlow<List<Product>> =
        combine(allProducts, filter) { list, f ->
            when (f) {
                InventoryFilter.ALL -> list
                InventoryFilter.LOW -> list.filter {
                    it.lowStockThreshold > 0 && it.quantity <= it.lowStockThreshold && it.quantity > 0
                }
                InventoryFilter.OUT -> list.filter { it.quantity <= 0 }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val itemCount: StateFlow<Int> = active.id
        .flatMapLatest { bid ->
            if (bid == null) flowOf(0) else repo.inventoryItemCount(bid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val totalValue: StateFlow<Double> = active.id
        .flatMapLatest { bid ->
            if (bid == null) flowOf(0.0) else repo.inventoryTotalValue(bid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val lowCount: StateFlow<Int> = active.id
        .flatMapLatest { bid ->
            if (bid == null) flowOf(0) else repo.lowStockCount(bid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setFilter(f: InventoryFilter) { filter.value = f }
}
