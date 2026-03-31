package org.example.project.domain.model

import org.example.project.domain.id.MerchantId

data class Merchant(
    val id: MerchantId,
    val name: String,
    val description: String?,
    val location: String?,
    val theme: String?,
    val iconPath: String?,
    val isActive: Boolean
)
