package com.finnvek.homecheck.billing

import android.app.Activity
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

data class BillingState(
    val entitlement: PremiumEntitlement = PremiumEntitlement(false),
    val formattedPrice: String? = null,
    val isLoading: Boolean = true,
    val isAvailable: Boolean = false,
)

enum class BillingEvent { PURCHASED, PENDING, CANCELLED, ALREADY_OWNED, NOT_FOUND, UNAVAILABLE, FAILED }

interface BillingManager {
    val state: StateFlow<BillingState>
    val events: SharedFlow<BillingEvent>
    fun launchPurchase(activity: Activity)
    fun refreshPurchases()
}
