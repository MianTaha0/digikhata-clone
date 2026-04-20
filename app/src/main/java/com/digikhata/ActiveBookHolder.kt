package com.digikhata

import com.digikhata.data.entity.Business
import com.digikhata.domain.repository.DigiRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveBookHolder @Inject constructor(
    private val repo: DigiRepository
) {
    private val _id = MutableStateFlow<Long?>(null)
    val id: StateFlow<Long?> = _id.asStateFlow()

    fun set(bookId: Long) { _id.value = bookId }

    @OptIn(ExperimentalCoroutinesApi::class)
    val active: Flow<Business?> = id.flatMapLatest { bid ->
        if (bid == null) flowOf(null) else repo.getBusiness(bid)
    }
}
