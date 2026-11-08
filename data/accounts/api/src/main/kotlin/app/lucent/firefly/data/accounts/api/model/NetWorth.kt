package app.lucent.firefly.data.accounts.api.model

import app.lucent.firefly.core.common.model.Money

/**
 * Net worth the way Firefly III reports it: active asset and liability accounts that have
 * `include_net_worth` set, summed in the primary currency. Liability balances are already
 * negative on the server, so they are added as-is.
 *
 * Accounts in the primary currency use [Account.displayBalance] so pending offline changes
 * count. Other accounts use the server-converted [Account.primaryCurrencyBalance]; accounts
 * that have no conversion are left out rather than mixed into the wrong currency.
 *
 * Returns null when no account qualifies.
 */
fun calculateNetWorth(accounts: List<Account>): Money? {
    val included = accounts.filter { account ->
        account.active &&
            account.includeInNetWorth &&
            (account.type == AccountType.ASSET || account.type == AccountType.LIABILITY)
    }
    val target = included.firstNotNullOfOrNull { it.primaryCurrency }
        ?: included.firstOrNull()?.currency
        ?: return null

    return included.fold(Money.zero(target)) { total, account ->
        val contribution = when {
            account.currency.code == target.code -> account.displayBalance
            account.primaryCurrencyBalance?.currency?.code == target.code -> account.primaryCurrencyBalance
            else -> null
        }
        if (contribution == null) total else Money(total.amount + contribution.amount, target)
    }
}
