package com.digikhata.domain.model

import com.digikhata.data.entity.Client

data class ClientBalance(
    val client: Client,
    val balance: Double
)
