package com.digikhata.ui.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.data.entity.Product
import com.digikhata.data.entity.StockMovement
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val repo: DigiRepository,
    active: ActiveBookHolder,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val productId: Long = savedStateHandle.get<Long>("productId") ?: 0L

    val product: StateFlow<Product?> = repo.getProduct(productId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val movements: StateFlow<List<StockMovement>> = repo.productMovements(productId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currency: StateFlow<String> = active.active
        .map { it?.currency ?: "Pakistan Rupee-Rs" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Pakistan Rupee-Rs")

    suspend fun delete(p: Product) {
        repo.deleteProduct(p)
    }

    suspend fun adjust(delta: Double, reason: String?) {
        repo.adjustStock(productId, delta, reason)
    }
}
