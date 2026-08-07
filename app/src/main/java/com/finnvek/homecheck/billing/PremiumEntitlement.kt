package com.finnvek.homecheck.billing

data class PremiumEntitlement(val isPremium: Boolean) {
    companion object {
        const val PRODUCT_ID = "homecheck_premium_lifetime"

        fun fromOwnedProductIds(productIds: Set<String>) =
            PremiumEntitlement(PRODUCT_ID in productIds)
    }
}
