package com.digikhata.ui.inventory

import androidx.lifecycle.ViewModel
import com.digikhata.data.entity.Product
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProductSheetViewModel @Inject constructor(
    private val repo: DigiRepository
) : ViewModel() {

    suspend fun save(product: Product, imagePath: String?): Long =
        repo.addProduct(product, imagePath)

    suspend fun update(product: Product) {
        repo.updateProduct(product)
    }
}
