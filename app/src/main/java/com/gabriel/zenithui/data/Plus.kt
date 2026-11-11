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
 * Zenith Plus: hidden apps, backup and restore, the island and the rest Plus unlocks.
 *
 * Everything that shipped free stays free. Plus is only ever additions, it runs for a week on
 * every install so you can judge it with your own home screen, and then it is one payment that
 * never expires.
 */
object Plus {

    /** The one-time pass, "Forever" in the app. Bought once, kept for ever, on every phone you sign in to. */
    const val LIFETIME_ID = "zenith_plus"

    /**
     * The subscription. It is named for the monthly plan it started with, and Play never lets an ID
     * change, but it carries both plans now as base plans of this one product: a month at a time,
     * with the free trial, and three months at a time. See [SubscriptionPlan].
     */
    const val MONTHLY_ID = "zenith_plus_monthly"

    const val TRIAL_DAYS = 7

    /**
     * Shown until Play tells us the real, locally formatted prices — which it will, in the
     * viewer's own currency. These are the reference prices set in Play Console.
     */
    const val FALLBACK_MONTHLY = "$4.00"
    const val FALLBACK_QUARTERLY = "$8.00"
    const val FALLBACK_LIFETIME = "$10.00"

    /** The same reference prices as amounts, in millionths as Play counts them, for [payOnce]. */
    const val FALLBACK_MONTHLY_MICROS = 4_000_000L
    const val FALLBACK_LIFETIME_MICROS = 10_000_000L

    /**
     * How the prices are meant to be read, in one line: what paying once for the pass comes to in
     * months of the monthly plan.
     *
     * Every price is real and honestly labelled; the pass simply happens to cost less than a few
     * months of the subscription, and saying so is the whole argument for it. It is worked out
     * from the amounts Play charges rather than written down, because Play sets each country's
     * prices on its own and the sum does not come out the same everywhere, and it is rounded up
     * to a whole month so that it never claims more than is true. There is no invented "was"
     * price anywhere — Play rejects those, and they are the kind of thing that makes people trust
     * an app less the moment they work it out.
     *
     * Null when there is nothing honest to say, which is when either amount is missing.
     */
    fun payOnce(amounts: PriceAmounts): String? {
        val once = amounts.lifetimeMicros
        val month = amounts.monthlyMicros
        if (once <= 0L || month <= 0L) return null
        val whole = once / month
        return if (once % month == 0L) {
            "Pay once: the price of ${months(whole)} of the monthly plan."
        } else {
            "Pay once: less than ${months(whole + 1)} of the monthly plan."
        }
    }

    private val NUMBERS = listOf(
        "zero", "one", "two", "three", "four", "five", "six",
        "seven", "eight", "nine", "ten", "eleven", "twelve",
    )

    /** "three months": in words while they are short, as anyone would say them aloud. */
    private fun months(count: Long): String {
        val number = NUMBERS.getOrNull(count.toInt()) ?: count.toString()
        return if (count == 1L) "$number month" else "$number months"
    }

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

    /**
     * What the Plus sheet and setup list. FEATURE_ICONS gives them their icons by position, so an
     * entry comes out of both or neither.
     */
    val FEATURES = listOf(
        "Hidden apps" to "Keep chosen apps out of the drawer, behind a PIN.",
        "Backup & restore" to "Save your whole home screen to a file and put it back any time.",
        "Island" to "A pill round the camera, over every app: music, charging, notifications.",
    )
}

/**
 * The subscription's two plans. Play calls them base plans of the one product, [Plus.MONTHLY_ID],
 * so a purchase of either is that product and being subscribed covers both. Play tells them apart
 * by how often they bill, as an ISO 8601 period, and so does Zenith: never by their place in
 * Play's list, which nothing promises to keep in order.
 */
enum class SubscriptionPlan(val billingPeriod: String) {
    MONTHLY("P1M"),
    QUARTERLY("P3M"),
}

/**
 * What the pass and the monthly plan cost, as amounts in one currency (millionths, as Play counts
 * them), so that [Plus.payOnce] can set one against the other: Play's own once it has priced
 * both, the reference prices until then, and never one of each, which would weigh pounds against
 * dollars.
 */
@Immutable
data class PriceAmounts(
    val lifetimeMicros: Long = Plus.FALLBACK_LIFETIME_MICROS,
    val monthlyMicros: Long = Plus.FALLBACK_MONTHLY_MICROS,
)

/** What the user is entitled to right now. */
@Immutable
data class PlusStatus(
    /** Entitled by either route: the pass, or a live subscription. */
    val purchased: Boolean = false,
    /** Specifically the pass — the one that cannot lapse. */
    val lifetime: Boolean = false,
    /** Specifically a live subscription, on either plan. */
    val subscribed: Boolean = false,
    val trialDaysLeft: Int = 0,
    /** True while the free week has not been taken yet, so it can still be offered. */
    val trialAvailable: Boolean = true,
    val monthlyPrice: String = Plus.FALLBACK_MONTHLY,
    /** The three-month plan's price, paid every three months. */
    val quarterlyPrice: String = Plus.FALLBACK_QUARTERLY,
    val lifetimePrice: String = Plus.FALLBACK_LIFETIME,
    /** False once Play has the subscription without a monthly plan, so there is none to offer. */
    val monthlyOffered: Boolean = true,
    /** False once Play has the subscription without a three-month plan, so there is none to offer. */
    val quarterlyOffered: Boolean = true,
    /** The pass against the monthly plan, for the honest comparison between the two. */
    val amounts: PriceAmounts = PriceAmounts(),
    /** False when this device has no Play Billing, so the buy button can say so honestly. */
    val purchasable: Boolean = true,
    /** True once the monthly plan has a free trial in Play to sell, which it does not yet. */
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

    private val _quarterlyPrice = MutableStateFlow(Plus.FALLBACK_QUARTERLY)

    /** What the three-month plan costs, paid every three months, as Play formats it. */
    val quarterlyPrice: StateFlow<String> = _quarterlyPrice.asStateFlow()

    private val _monthlyOffered = MutableStateFlow(true)
    private val _quarterlyOffered = MutableStateFlow(true)

    /**
     * Whether each plan is there to be sold. Until Play has the subscription at all, both are, at
     * their reference prices, as the monthly one always was, so a phone that has not heard from
     * Play still sees the whole offer. Once Play has it, a plan it does not carry is not offered:
     * that is how a base plan not yet made in Play Console stays out of sight.
     */
    val monthlyOffered: StateFlow<Boolean> = _monthlyOffered.asStateFlow()
    val quarterlyOffered: StateFlow<Boolean> = _quarterlyOffered.asStateFlow()

    private val _amounts = MutableStateFlow(PriceAmounts())

    /** The pass and the monthly plan as amounts, for comparing the two: see [PriceAmounts]. */
    val amounts: StateFlow<PriceAmounts> = _amounts.asStateFlow()

    private val _available = MutableStateFlow(true)

    /** False when this device cannot buy anything, so the UI can stop offering. */
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private var lifetime: ProductDetails? = null
    private var monthly: ProductDetails? = null

    private val _trialOffer = MutableStateFlow(false)

    /** True when Play's monthly plan carries a free-trial offer for us to launch. */
    val trialOffer: StateFlow<Boolean> = _trialOffer.asStateFlow()

    private val _subscribed = MutableStateFlow(false)

    /** True while a subscription is live. Unlike the pass, this one can go away again. */
    val subscribed: StateFlow<Boolean> = _subscribed.asStateFlow()

    private val _answered = MutableStateFlow(false)

    /**
     * True once Play has answered both questions, one-time purchases and subscriptions, so that
     * "nothing bought" is Play's word rather than only that nobody has asked yet.
     */
    val answered: StateFlow<Boolean> = _answered.asStateFlow()

    private var answeredTypes = emptySet<String>()

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
                    answeredTypes = answeredTypes + type
                    if (answeredTypes.size == 2) _answered.value = true
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
            // Billing 8 hands back a result object rather than a bare list; anything Play could not
            // fetch is listed separately and simply is not in productDetailsList.
            client.queryProductDetailsAsync(params) { result, details ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
                val found = details.productDetailsList
                    .firstOrNull { it.productId == Plus.LIFETIME_ID } ?: return@queryProductDetailsAsync
                lifetime = found
                found.oneTimePurchaseOfferDetails?.formattedPrice?.let { _lifetimePrice.value = it }
                settleAmounts()
            }
        }
        // Asking for a product that does not exist yet is not an error: Play returns nothing, and
        // both plans stay on show at their reference prices, as the pass does (see [monthlyOffered]).
        runCatching {
            val product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(Plus.MONTHLY_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
            client.queryProductDetailsAsync(params) { result, details ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
                val found = details.productDetailsList.firstOrNull { it.productId == Plus.MONTHLY_ID }
                monthly = found
                _trialOffer.value = found?.trialOffer() != null
                // The price to quote is the one people actually pay, not the free phase.
                found?.offerFor(SubscriptionPlan.MONTHLY)?.paidPhase()?.formattedPrice?.let { _monthlyPrice.value = it }
                found?.offerFor(SubscriptionPlan.QUARTERLY)?.paidPhase()?.formattedPrice?.let { _quarterlyPrice.value = it }
                _monthlyOffered.value = found == null || found.offerFor(SubscriptionPlan.MONTHLY) != null
                _quarterlyOffered.value = found == null || found.offerFor(SubscriptionPlan.QUARTERLY) != null
                settleAmounts()
            }
        }
    }

    /**
     * Sets the pass against the monthly plan in Play's own amounts once it has priced both in one
     * currency. Until then it is the reference prices against each other, never one of each.
     */
    private fun settleAmounts() {
        val once = lifetime?.oneTimePurchaseOfferDetails
        val month = monthly?.offerFor(SubscriptionPlan.MONTHLY)?.paidPhase()
        _amounts.value = if (once != null && month != null && once.priceCurrencyCode == month.priceCurrencyCode) {
            PriceAmounts(lifetimeMicros = once.priceAmountMicros, monthlyMicros = month.priceAmountMicros)
        } else {
            PriceAmounts()
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

    /** Opens Play's buy sheet for one of the subscription's plans, at that plan's own price. */
    fun subscribe(activity: Activity, plan: SubscriptionPlan): Boolean {
        val product = monthly ?: run {
            refresh()
            return false
        }
        val offer = product.offerFor(plan) ?: return false
        return launch(activity, product, offer.offerToken)
    }

    /**
     * Starts the monthly plan's free trial - the flow where Play itself says "7 days free, then
     * the monthly price". Returns false when no such offer exists yet, which is the caller's cue
     * to run the seven days on the device instead.
     */
    fun startPlayTrial(activity: Activity): Boolean {
        if (!_trialOffer.value) return false
        val product = monthly ?: return false
        val offer = product.trialOffer() ?: return false
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

/**
 * The way to buy [plan] at the plan's own price: of the offers whose paid phase bills that often,
 * the base plan's own, which has no offer ID. Any other offer on the plan, the free trial among
 * them, is only the fallback.
 */
private fun ProductDetails.offerFor(plan: SubscriptionPlan): ProductDetails.SubscriptionOfferDetails? {
    val offers = subscriptionOfferDetails.orEmpty().filter { it.paidPhase()?.billingPeriod == plan.billingPeriod }
    return offers.firstOrNull { it.offerId == null } ?: offers.firstOrNull()
}

/**
 * The free trial Plus's week becomes once Play has one. Only the monthly plan's counts, since what
 * the app says follows the trial is the monthly price.
 */
private fun ProductDetails.trialOffer(): ProductDetails.SubscriptionOfferDetails? =
    subscriptionOfferDetails.orEmpty().firstOrNull { offer ->
        offer.paidPhase()?.billingPeriod == SubscriptionPlan.MONTHLY.billingPeriod &&
            offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
    }

/** The phase that is paid for and goes on repeating: what the plan costs once any free days are over. */
private fun ProductDetails.SubscriptionOfferDetails.paidPhase(): ProductDetails.PricingPhase? =
    pricingPhases.pricingPhaseList.lastOrNull { it.priceAmountMicros > 0L }
