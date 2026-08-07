package com.finnvek.homecheck.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.homecheck.billing.BillingEvent
import com.finnvek.homecheck.billing.BillingManager
import com.finnvek.homecheck.billing.BillingState
import com.finnvek.homecheck.data.preferences.UserPreferences
import com.finnvek.homecheck.data.preferences.UserPreferencesRepository
import com.finnvek.homecheck.data.repository.HomeRepository
import com.finnvek.homecheck.domain.AssetLimitPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppEvent { OPEN_NEW_ASSET }

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val repository: HomeRepository,
    private val billingManager: BillingManager,
) : ViewModel() {
    val preferences: StateFlow<UserPreferences?> = preferencesRepository.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )
    val billing: StateFlow<BillingState> = billingManager.state
    val billingEvents: SharedFlow<BillingEvent> = billingManager.events
    private val mutableShowPremium = MutableStateFlow(false)
    val showPremium = mutableShowPremium.asStateFlow()
    private val mutableEvents = MutableSharedFlow<AppEvent>(extraBufferCapacity = 1)
    val events = mutableEvents.asSharedFlow()

    fun completeOnboarding() = viewModelScope.launch { preferencesRepository.completeOnboarding() }

    fun requestNewAsset() = viewModelScope.launch {
        if (AssetLimitPolicy.canCreate(repository.assetCount(), billing.value.entitlement.isPremium)) {
            mutableEvents.emit(AppEvent.OPEN_NEW_ASSET)
        } else {
            mutableShowPremium.value = true
        }
    }

    fun openPremium() { mutableShowPremium.value = true }
    fun dismissPremium() { mutableShowPremium.value = false }
    fun launchPurchase(activity: Activity) = billingManager.launchPurchase(activity)
    fun restorePurchase() = billingManager.refreshPurchases()
}
