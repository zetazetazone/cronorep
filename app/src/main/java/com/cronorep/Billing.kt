package com.cronorep

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * One-time "lifetime unlock" purchase through Google Play.
 * Google Play is the source of truth. A local copy lets the app work offline.
 */
object Billing : PurchasesUpdatedListener {
    /** Must match the product ID in the Google Play Console. */
    const val PRODUCT_ID = "lifetime_unlock"

    private const val PREFS = "cronorep"
    private const val KEY_UNLOCKED = "unlocked"

    var unlocked by mutableStateOf(false)
        private set

    /** Price from Google Play in the user's currency. Null until the store answers. */
    var priceText by mutableStateOf<String?>(null)
        private set

    /** Short status for the paywall. Null = nothing to say. */
    var message by mutableStateOf<String?>(null)
        private set

    private var appContext: Context? = null
    private var client: BillingClient? = null
    private var product: ProductDetails? = null

    fun init(context: Context) {
        if (client != null) return
        val app = context.applicationContext
        appContext = app
        unlocked = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_UNLOCKED, false)
        client = BillingClient.newBuilder(app)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        connect()
    }

    private fun connect() {
        client?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) refresh()
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    /** Asks Google Play again for the purchase and the price. */
    fun refresh() {
        val c = client ?: return
        if (!c.isReady) {
            connect()
            return
        }

        c.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        ) { result, purchases ->
            if (result.responseCode == BillingResponseCode.OK) handle(purchases, fullList = true)
        }

        val query = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(ProductType.INAPP)
                        .build()
                )
            )
            .build()
        c.queryProductDetailsAsync(query) { result, detailsResult ->
            if (result.responseCode == BillingResponseCode.OK) {
                product = detailsResult.productDetailsList.firstOrNull()
                priceText = product?.oneTimePurchaseOfferDetails?.formattedPrice
            }
        }
    }

    fun buy(activity: Activity) {
        val c = client
        val p = product
        if (c == null || !c.isReady || p == null) {
            message = "Store not ready. Try again."
            refresh()
            return
        }
        message = null
        c.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(p)
                            .build()
                    )
                )
                .build()
        )
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> handle(purchases.orEmpty(), fullList = false)
            BillingResponseCode.USER_CANCELED -> Unit
            BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            else -> message = "Payment failed. Try again."
        }
    }

    /** [fullList] = the list has all purchases, so "not in the list" means "not owned". */
    private fun handle(purchases: List<Purchase>, fullList: Boolean) {
        val mine = purchases.filter { PRODUCT_ID in it.products }
        val paid = mine.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }

        when {
            paid.isNotEmpty() -> {
                saveUnlocked(true)
                message = null
                // Google refunds a purchase that the app does not acknowledge in 3 days.
                paid.filterNot { it.isAcknowledged }.forEach { purchase ->
                    client?.acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                    ) { }
                }
            }
            mine.any { it.purchaseState == Purchase.PurchaseState.PENDING } ->
                message = "Payment pending."
            // Covers refunds: Google Play no longer lists the purchase.
            fullList -> saveUnlocked(false)
        }
    }

    private fun saveUnlocked(value: Boolean) {
        unlocked = value
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()?.putBoolean(KEY_UNLOCKED, value)?.apply()
    }
}
