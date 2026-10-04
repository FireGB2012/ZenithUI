package com.gabriel.zenithui.data.server

import com.gabriel.zenithui.data.Aura
import com.gabriel.zenithui.data.ZenithSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** What an account changes on the phone that is signed in to it: its code, and what its code has earned. */
class AccountRulesTest {

    private fun account(code: String = "WTR4RT", referrals: Int = 0, redeemed: Boolean = false) =
        Account(username = "gabriel", google = false, code = code, referrals = referrals, referralCap = 10, redeemed = redeemed, backupAt = null)

    @Test
    fun `signing in takes the account's code and pays for each friend who used it`() {
        val phone = ZenithSettings(aura = 40, referralCode = "BCDFGH")
        val after = AccountRules.settle(phone, account(referrals = 2))
        assertEquals("WTR4RT", after.referralCode)
        assertEquals(40 + 2 * Aura.REFERRAL_REWARD, after.aura)
        assertEquals(mapOf("WTR4RT" to 2), after.referralsPaid)
    }

    @Test
    fun `looking again pays nothing more, and one more friend pays once more`() {
        val once = AccountRules.settle(ZenithSettings(), account(referrals = 2))
        assertEquals(once, AccountRules.settle(once, account(referrals = 2)))
        val more = AccountRules.settle(once, account(referrals = 3))
        assertEquals(once.aura + Aura.REFERRAL_REWARD, more.aura)
        assertEquals(mapOf("WTR4RT" to 3), more.referralsPaid)
    }

    @Test
    fun `a backup from before the last friends pays for them when it is put back`() {
        // The backup carries the aura it was made with, and how many friends that aura was paid for.
        val restored = ZenithSettings(aura = 300, referralCode = "WTR4RT", referralsPaid = mapOf("WTR4RT" to 2))
        assertEquals(300 + 3 * Aura.REFERRAL_REWARD, AccountRules.settle(restored, account(referrals = 5)).aura)
    }

    @Test
    fun `a code used on another phone pays this one once`() {
        val after = AccountRules.settle(ZenithSettings(aura = 10), account(redeemed = true))
        assertEquals(10 + Aura.REFERRAL_REWARD, after.aura)
        assertEquals(Aura.USED_ON_ACCOUNT, after.referredBy)
        assertEquals(after, AccountRules.settle(after, account(redeemed = true)))
    }

    @Test
    fun `a phone that used the code itself is not paid for it again`() {
        val phone = ZenithSettings(aura = Aura.REFERRAL_REWARD, referredBy = "BCDFGH")
        val after = AccountRules.settle(phone, account(redeemed = true))
        assertEquals(Aura.REFERRAL_REWARD, after.aura)
        assertEquals("BCDFGH", after.referredBy)
    }

    @Test
    fun `a new account given an old account's code starts a count of its own`() {
        // The old account was deleted with three friends paid for; the new one has none yet.
        val phone = ZenithSettings(aura = 500, referralCode = "WTR4RT", referralsPaid = mapOf("WTR4RT" to 3))
        val fresh = AccountRules.settle(phone, account(referrals = 0))
        assertEquals("nothing is taken back", 500, fresh.aura)
        assertEquals(mapOf("WTR4RT" to 0), fresh.referralsPaid)
        // Its first friend is paid for, not lost among the old account's three.
        assertEquals(500 + Aura.REFERRAL_REWARD, AccountRules.settle(fresh, account(referrals = 1)).aura)
    }

    @Test
    fun `each account's count is its own`() {
        val phone = AccountRules.settle(ZenithSettings(), account(code = "WTR4RT", referrals = 4))
        val other = AccountRules.settle(phone, account(code = "BCDFGH", referrals = 1))
        assertEquals(5 * Aura.REFERRAL_REWARD, other.aura)
        assertEquals(mapOf("WTR4RT" to 4, "BCDFGH" to 1), other.referralsPaid)
        // Back to the first account: its four are still paid for.
        assertEquals(other.aura, AccountRules.settle(other, account(code = "WTR4RT", referrals = 4)).aura)
    }

    @Test
    fun `an account the server described badly changes nothing`() {
        val phone = ZenithSettings(aura = 70, referralCode = "BCDFGH")
        assertSame(phone, AccountRules.settle(phone, account(code = "", referrals = 9, redeemed = true)))
    }

    @Test
    fun `a backup for the account leaves the PIN and the purchase on the phone`() {
        val phone = ZenithSettings(
            userName = "Gabriel",
            aura = 310,
            hiddenApps = listOf("a/b"),
            hiddenPin = "4321",
            plusTrialStart = 1_791_000_000_000,
            plusPurchased = true,
        )
        val sent = AccountRules.forAccount(phone)
        assertEquals("", sent.hiddenPin)
        assertEquals(0L, sent.plusTrialStart)
        assertEquals(false, sent.plusPurchased)
        // Everything else goes as it is.
        assertEquals(phone, sent.copy(hiddenPin = "4321", plusTrialStart = 1_791_000_000_000, plusPurchased = true))
    }
}
