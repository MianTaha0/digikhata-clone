package com.digikhata.ui.book

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.data.entity.Business
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookViewModel @Inject constructor(
    private val repo: DigiRepository,
    private val active: ActiveBookHolder,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val bookId: Long = savedStateHandle.get<Long>("bookId") ?: 0L

    val current: StateFlow<Business?> =
        if (bookId != 0L) repo.getBusiness(bookId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        else kotlinx.coroutines.flow.MutableStateFlow(null)

    fun create(name: String, owner: String?, currency: String, colorHex: String, onDone: (Long) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repo.upsertBusiness(
                Business(name = name.trim(), ownerName = owner?.trim()?.ifBlank { null }, currency = currency, colorHex = colorHex)
            )
            active.set(id)
            onDone(id)
        }
    }

    fun update(business: Business) {
        viewModelScope.launch { repo.upsertBusiness(business) }
    }

    fun delete(business: Business, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteBusiness(business)
            onDone()
        }
    }
}
