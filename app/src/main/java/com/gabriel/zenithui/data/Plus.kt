package com.gabriel.zenithui.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Zenith Plus: the custom theme maker, hidden apps, and backup and restore.
 *
 * Everything that shipped free stays free. Plus is only ever additions, it runs for a week on
 * every install so you can judge it with your own home screen, and then it is one payment that
 * never expires.
 */
object Plus {

    /** The one-time pass. Bought once, kept for ever, on every phone you sign in to. */
    const val LIFETIME_ID = "zenith_plus"

    /** The monthly subscription, which carries the free trial. */
    const val MONTHLY_ID = "zenith_plus_monthly"

    const val TRIAL_DAYS = 7

    /**
     * Shown until Play tells us the real, locally formatted prices — which it will, in the
     * viewer's own currency. These are the reference prices set in Play Console.
     */
    const val FALLBACK_MONTHLY = "$1.99"
    const val FALLBACK_LIFETIME = "$14.99"

    /**
     * How the two prices are meant to be read.
     *
     * Both are real and both are honestly labelled; the lifetime pass simply happens to be worth
     * about eight months of the subscription, and saying so is the whole argument for it. There is
     * no invented "was" price anywhere — Play rejects those, and they are the kind of thing that
     * makes people trust an app less the moment they work it out.
     */
    const val LIFETIME_EQUIVALENT_MONTHS = 8

    /**
     * Codes that unlock Plus without a purchase: your own phones, testers, reviewers, giveaways.
     *
     * They are stored as SHA-256 so the codes are not sitting in the APK as readable strings.
     * That is a speed bump, not a lock — anyone determined enough to pull the app apart can find a
     * way in, and for a five pound launcher that is the right trade. Add a hash here to mint a new
     * code; there is deliberately no way to revoke one already used.
     */
    private val CODE_HASHES = setOf(
        // ZENITH-FOUNDER — the developer's own devices
        "cd7331940682124a34fcda3592a4d0d9f441a01d08bd531c69da9384bf499574",
        // ZENITH-TESTER — hand out to testers and reviewers
        "7d93d86e4fb63076eb310bcaee6a68183e3621b954e48535bf0b840092a8c29c",
    )

    /** Dashes, spaces and case are all forgiven: nobody should fail at typing a gift. */
    fun normaliseCode(input: String): String = input.uppercase().filter { it.isLetterOrDigit() }

    fun isValidCode(input: String): Boolean {
        val cleaned = normaliseCode(input)
        if (cleaned.isEmpty()) return false
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(cleaned.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return digest in CODE_HASHES
    }

    val FEATURES = listOf(
        "Theme maker" to "Build your own palette: any accent, any glass, any background.",
        "Hidden apps" to "Keep chosen apps out of the drawer, behind a PIN.",
        "Backup & restore" to "Save your whole home screen to a file and put it back any time.",
    )
}

/** What the user is entitled to right now. */
@Immutable
data class PlusStatus(
    /** Entitled by either route: the pass, or a live subscription. */
    val purchased: Boolean = false,
    /** Specifically the pass — the one that cannot lapse. */
    val lifetime: Boolean = false,
    /** Specifically a live subscription. */
    val subscribed: Boolean = false,
    val trialDaysLeft: Int = 0,
    /** True while the free week has not been taken yet, so it can still be offered. */
    val trialAvailable: Boolean = true,
    val monthlyPrice: String = Plus.FALLBACK_MONTHLY,
    val lifetimePrice: String = Plus.FALLBACK_LIFETIME,
    /** False when this device has no Play Billing, so the buy button can say so honestly. */
    val purchasable: Boolean = true,
    /** True once Play has a subscription with a free trial to sell, which it does not yet. */
    val playTrialOffer: Boolean = false,
) {
    val inTrial: Boolean get() = !purchased && trialDaysLeft > 0
    val active: Boolean get() = purchased || trialDaysLeft > 0
}

/** Whole days of trial left, counting the day it started as day one. Zero until it is started. */
fun trialDaysLeft(startedAt: Long, now: Long = System.currentTimeMillis()): Int {
    if (startedAt <= 0L) return 0
    val elapsedDays = ((now - startedAt) / 86_400_000L).toInt()
    return (Plus.TRIAL_DAYS - elapsedDays).coerceIn(0, Plus.TRIAL_DAYS)
}

/**
 * A thin wrapper over Play Billing.
 *
 * The launcher must work perfectly on a phone with no Play Store, an old Play Store, or no
 * network, so every failure here is quiet: [purchased] simply stays as it was, and the caller
 * keeps trusting the copy it has saved.
 */
class BillingRepository(context: Context) {

    private val app = context.applicationContext

    private val _purchased = MutableStateFlow(false)

    /** True once Play has confirmed a purchase. Never set back to false by a failure. */
    val purchased: StateFlow<Boolean> = _purchased.asStateFlow()

    private val _permanent = MutableStateFlow(false)

    /** True only for the one-time unlock, the only entitlement worth remembering offline. */
    val permanent: StateFlow<Boolean> = _permanent.asStateFlow()

    private val _lifetimePrice = MutableStateFlow(Plus.FALLBACK_LIFETIME)
    val lifetimePrice: StateFlow<String> = _lifetimePrice.asStateFlow()

    private val _monthlyPrice = MutableStateFlow(Plus.FALLBACK_MONTHLY)
    val monthlyPrice: StateFlow<String> = _monthlyPrice.asStateFlow()

    private val _available = MutableStateFlow(true)

    /** False when this device cannot buy anything, so the UI can stop offering. */
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private var lifetime: ProductDetails? = null
    private var monthly: ProductDetails? = null

    private val _trialOffer = MutableStateFlow(false)

    /** True when Play has a subscription carrying a free-trial offer for us to launch. */
    val trialOffer: StateFlow<Boolean> = _trialOffer.asStateFlow()

    private val _subscribed = MutableStateFlow(false)

    /** True while a subscription is live. Unlike the pass, this one can go away again. */
    val subscribed: StateFlow<Boolean> = _subscribed.asStateFlow()

    private val client = BillingClient.newBuilder(app)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .setListener { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases?.forEach { record(it) }
            }
        }
        .build()

    private var connecting = false

    /** Connects if needed, then refreshes what Play knows. Safe to call as often as you like. */
    fun refresh() {
        if (client.isReady) {
            queryPurchases()
            queryDetails()
            return
        }
        if (connecting) return
        connecting = true
        runCatching {
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    connecting = false
                    val ok = result.responseCode == BillingClient.BillingResponseCode.OK
                    _available.value = ok
                    if (ok) {
                        queryPurchases()
                        queryDetails()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    connecting = false
                }
            })
        }.onFailure {
            connecting = false
            _available.value = false
            Log.w(TAG, "Play Billing unavailable", it)
        }
    }

    private fun queryPurchases() {
        listOf(BillingClient.ProductType.INAPP, BillingClient.ProductType.SUBS).forEach { type ->
            runCatching {
                val params = QueryPurchasesParams.newBuilder().setProductType(type).build()
                client.queryPurchasesAsync(params) { result, purchases ->
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
                    purchases.forEach { record(it) }
                    // Play's answer for subscriptions is the whole truth, so one missing from it has
                    // lapsed or been cancelled. A launcher process lives for days; without this a
                    // lapsed subscriber would keep Plus until the process happened to die.
                    if (type == BillingClient.ProductType.SUBS) {
                        val live = purchases.any { purchase ->
                            purchase.products.contains(Plus.MONTHLY_ID) &&
                                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                        }
                        _subscribed.value = live
                        _purchased.value = _permanent.value || live
                    }
                }
            }
        }
    }

    private fun queryDetails() {
        runCatching {
            val product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(Plus.LIFETIME_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
            client.queryProductDetailsAsync(params) { result, list ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
                val found = list.firstOrNull { it.productId == Plus.LIFETIME_ID } ?: return@queryProductDetailsAsync
                lifetime = found
                found.oneTimePurchaseOfferDetails?.formattedPrice?.let { _lifetimePrice.value = it }
            }
        }
        // Asking for a product that does not exist yet is not an error: Play returns nothing, the
        // monthly option simply does not appear, and the pass carries the whole offer on its own.
        runCatching {
            val product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(Plus.MONTHLY_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
            client.queryProductDetailsAsync(params) { result, list ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
                val found = list.firstOrNull { it.productId == Plus.MONTHLY_ID }
                monthly = found
                val offers = found?.subscriptionOfferDetails.orEmpty()
                _trialOffer.value = offers.any { offer ->
                    offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
                }
                // The price to quote is the one people actually pay, not the free phase.
                offers.firstOrNull()
                    ?.pricingPhases?.pricingPhaseList
                    ?.lastOrNull { it.priceAmountMicros > 0L }
                    ?.formattedPrice
                    ?.let { _monthlyPrice.value = it }
            }
        }
    }

    /**
     * A purchase counts the moment Play says it is paid for. Acknowledging it is required within
     * three days or Play refunds it, so that goes out straight away.
     */
    private fun record(purchase: Purchase) {
        val ours = purchase.products.any { it == Plus.LIFETIME_ID || it == Plus.MONTHLY_ID }
        if (!ours) return
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        // A subscription is entitlement only for as long as Play keeps returning it, so it is
        // honoured but never written down for ever; only the pass is permanent.
        if (purchase.products.any { it == Plus.LIFETIME_ID }) _permanent.value = true
        if (purchase.products.any { it == Plus.MONTHLY_ID }) _subscribed.value = true
        _purchased.value = true
        if (purchase.isAcknowledged) return
        runCatching {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(params) { }
        }
    }

    /** Opens Play's buy sheet for the lifetime pass. False when there is nothing to open. */
    fun buyLifetime(activity: Activity): Boolean {
        val product = lifetime ?: run {
            refresh()
            return false
        }
        return launch(activity, product, offerToken = null)
    }

    /** Opens Play's buy sheet for the monthly subscription. */
    fun subscribe(activity: Activity): Boolean {
        val product = monthly ?: run {
            refresh()
            return false
        }
        val offer = product.subscriptionOfferDetails?.firstOrNull() ?: return false
        return launch(activity, product, offer.offerToken)
    }

    /**
     * Starts the subscription's free trial - the flow where Play itself says "7 days free, then
     * the monthly price". Returns false when no such offer exists yet, which is the caller's cue
     * to run the seven days on the device instead.
     */
    fun startPlayTrial(activity: Activity): Boolean {
        if (!_trialOffer.value) return false
        val product = monthly ?: return false
        val offer = product.subscriptionOfferDetails?.firstOrNull { offer ->
            offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
        } ?: return false
        return launch(activity, product, offer.offerToken)
    }

    private fun launch(activity: Activity, product: ProductDetails, offerToken: String?): Boolean {
        val chosen = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .apply { if (offerToken != null) setOfferToken(offerToken) }
            .build()
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(chosen))
            .build()
        return runCatching {
            client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
        }.getOrDefault(false)
    }

    private companion object {
        const val TAG = "ZenithBilling"
    }
}
