package org.example.project.domain.repository

import org.example.project.domain.id.MerchantId
import org.example.project.domain.model.Merchant
import org.example.project.domain.model.ShippingMethod

interface MerchantRepository {
    suspend fun getAllMerchants(): List<Merchant>
    suspend fun getMerchantOrNull(id: MerchantId): Merchant?
    suspend fun getShippingMethodsForMerchant(merchantId: MerchantId): List<ShippingMethod>
}
