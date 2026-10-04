package com.gabriel.zenithui.data.server

import com.gabriel.zenithui.data.Aura
import com.gabriel.zenithui.data.ZenithSettings

/** What an account changes on the phone that is signed in to it. Rules only: nothing here talks to anything. */
internal object AccountRules {
    /**
     * Brings a phone into line with its account, as the server has just described it:
     *
     *  - the phone's Aura code is the account's, since that is the one friends are checked against;
     *  - each friend who has used the code since this phone last looked is paid for, once. What is
     *    remembered as paid for is the server's count as it stood, so a new account that was given
     *    the code of a deleted one starts counting from its own nothing;
     *  - a friend's code that the account used on another phone pays this phone too, once.
     *
     * Doing it again with the same account changes nothing.
     */
    fun settle(current: ZenithSettings, account: Account): ZenithSettings {
        if (account.code.isEmpty()) return current
        val paid = current.referralsPaid[account.code] ?: 0
        val usedElsewhere = account.redeemed && current.referredBy.isEmpty()
        return current.copy(
            referralCode = account.code,
            referredBy = if (usedElsewhere) Aura.USED_ON_ACCOUNT else current.referredBy,
            aura = current.aura + Aura.referralPayout(account.referrals, paid) + if (usedElsewhere) Aura.REFERRAL_REWARD else 0,
            referralsPaid = current.referralsPaid + (account.code to account.referrals),
        )
    }

    /**
     * The settings as they are sent to the account in a backup: without the hidden-apps PIN, and
     * without anything about the trial or the purchase. Neither is ever put back from a backup,
     * so neither has any business on the server.
     */
    fun forAccount(settings: ZenithSettings): ZenithSettings =
        settings.copy(hiddenPin = "", plusTrialStart = 0L, plusPurchased = false)
}
