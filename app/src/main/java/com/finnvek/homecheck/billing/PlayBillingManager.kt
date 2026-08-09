package com.finnvek.homecheck.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.finnvek.homecheck.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayBillingManager
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val preferences: UserPreferencesRepository,
    ) : BillingManager,
        PurchasesUpdatedListener {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val mutableState = MutableStateFlow(BillingState())
        private val mutableEvents = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 4)
        private var productDetails: ProductDetails? = null

        @Volatile private var connecting = false

        @Volatile private var notifyRestoreAfterConnect = false

        override val state: StateFlow<BillingState> = mutableState.asStateFlow()
        override val events: SharedFlow<BillingEvent> = mutableEvents.asSharedFlow()

        private val client =
            BillingClient
                .newBuilder(context)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build()

        init {
            scope.launch {
                val cached = preferences.preferences.first().premiumCached
                mutableState.value = mutableState.value.copy(entitlement = PremiumEntitlement(cached))
            }
            connect(notifyRestore = false)
        }

        private fun connect(notifyRestore: Boolean) {
            if (notifyRestore) notifyRestoreAfterConnect = true
            if (client.isReady) {
                queryPurchases(notifyRestore)
                return
            }
            if (connecting) return
            connecting = true
            client.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        connecting = false
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            queryProduct()
                            val notify = notifyRestoreAfterConnect
                            notifyRestoreAfterConnect = false
                            queryPurchases(notify)
                        } else {
                            unavailable()
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        connecting = false
                        mutableState.value = mutableState.value.copy(isAvailable = false, isLoading = false)
                    }
                },
            )
        }

        private fun queryProduct() {
            val product =
                QueryProductDetailsParams.Product
                    .newBuilder()
                    .setProductId(PremiumEntitlement.PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
            client.queryProductDetailsAsync(params) { result, response ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    productDetails = response.productDetailsList.firstOrNull()
                    mutableState.value =
                        mutableState.value.copy(
                            formattedPrice = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice,
                            isAvailable = productDetails != null,
                            isLoading = false,
                        )
                } else {
                    unavailable()
                }
            }
        }

        override fun refreshPurchases() {
            if (!client.isReady) {
                connect(notifyRestore = true)
                return
            }
            queryPurchases(notifyRestore = true)
        }

        private fun queryPurchases(notifyRestore: Boolean) {
            val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
            client.queryPurchasesAsync(params) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    processPurchases(purchases, ownedEvent = if (notifyRestore) BillingEvent.ALREADY_OWNED else null)
                    if (notifyRestore && purchases.none { it.purchaseState == Purchase.PurchaseState.PURCHASED } &&
                        purchases.none { it.purchaseState == Purchase.PurchaseState.PENDING }
                    ) {
                        mutableEvents.tryEmit(BillingEvent.NOT_FOUND)
                    }
                } else {
                    mutableState.value = mutableState.value.copy(isLoading = false)
                    mutableEvents.tryEmit(BillingEvent.UNAVAILABLE)
                }
            }
        }

        override fun launchPurchase(activity: Activity) {
            val details = productDetails
            if (!client.isReady || details == null) {
                mutableEvents.tryEmit(BillingEvent.UNAVAILABLE)
                return
            }
            val productParams =
                BillingFlowParams.ProductDetailsParams
                    .newBuilder()
                    .setProductDetails(details)
                    .build()
            val result =
                client.launchBillingFlow(
                    activity,
                    BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams)).build(),
                )
            if (result.responseCode != BillingClient.BillingResponseCode.OK) emitResult(result.responseCode)
        }

        override fun onPurchasesUpdated(
            result: BillingResult,
            purchases: List<Purchase>?,
        ) {
            if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                processPurchases(purchases, ownedEvent = BillingEvent.PURCHASED)
            } else {
                emitResult(result.responseCode)
            }
        }

        private fun processPurchases(
            purchases: List<Purchase>,
            ownedEvent: BillingEvent?,
        ) {
            val ownedIds =
                purchases
                    .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    .flatMapTo(mutableSetOf(), Purchase::getProducts)
            val entitlement = PremiumEntitlement.fromOwnedProductIds(ownedIds)
            mutableState.value = mutableState.value.copy(entitlement = entitlement, isLoading = false)
            scope.launch { preferences.setPremiumCached(entitlement.isPremium) }
            purchases
                .filter { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        PremiumEntitlement.PRODUCT_ID in purchase.products && !purchase.isAcknowledged
                }.forEach(::acknowledge)
            if (entitlement.isPremium && ownedEvent != null) mutableEvents.tryEmit(ownedEvent)
            if (purchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }) mutableEvents.tryEmit(BillingEvent.PENDING)
        }

        private fun acknowledge(purchase: Purchase) {
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            client.acknowledgePurchase(params) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) mutableEvents.tryEmit(BillingEvent.FAILED)
            }
        }

        private fun emitResult(responseCode: Int) {
            mutableEvents.tryEmit(
                when (responseCode) {
                    BillingClient.BillingResponseCode.USER_CANCELED -> BillingEvent.CANCELLED

                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> BillingEvent.ALREADY_OWNED

                    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                    BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                    -> BillingEvent.UNAVAILABLE

                    else -> BillingEvent.FAILED
                },
            )
        }

        private fun unavailable() {
            mutableState.value = mutableState.value.copy(isAvailable = false, isLoading = false)
            mutableEvents.tryEmit(BillingEvent.UNAVAILABLE)
        }
    }
