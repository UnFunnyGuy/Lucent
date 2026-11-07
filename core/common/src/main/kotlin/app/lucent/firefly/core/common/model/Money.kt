package app.lucent.firefly.core.common.model

import java.math.BigDecimal
import java.math.RoundingMode

data class Currency(
    val id: String? = null,
    val code: String,
    val symbol: String,
    val decimalPlaces: Int = 2,
)

data class Money(
    val amount: BigDecimal,
    val currency: Currency,
) : Comparable<Money> {

    operator fun plus(other: Money): Money {
        require(currency.code == other.currency.code) {
            "Cannot add currencies with mismatched codes: ${currency.code} and ${other.currency.code}"
        }
        return copy(amount = amount.add(other.amount))
    }

    operator fun minus(other: Money): Money {
        require(currency.code == other.currency.code) {
            "Cannot subtract currencies with mismatched codes: ${currency.code} and ${other.currency.code}"
        }
        return copy(amount = amount.subtract(other.amount))
    }

    val isPositive: Boolean get() = amount > BigDecimal.ZERO
    val isNegative: Boolean get() = amount < BigDecimal.ZERO
    val isZero: Boolean get() = amount.compareTo(BigDecimal.ZERO) == 0

    fun formatDisplay(): String {
        val scaled = amount.setScale(currency.decimalPlaces, RoundingMode.HALF_UP)
        return "${currency.symbol}$scaled"
    }

    override fun compareTo(other: Money): Int {
        require(currency.code == other.currency.code) {
            "Cannot compare currencies with mismatched codes: ${currency.code} and ${other.currency.code}"
        }
        return amount.compareTo(other.amount)
    }

    companion object {
        fun zero(currency: Currency): Money =
            Money(BigDecimal.ZERO.setScale(currency.decimalPlaces, RoundingMode.HALF_UP), currency)

        fun of(amountStr: String, currency: Currency): Money {
            val parsed = amountStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
            return Money(parsed.setScale(currency.decimalPlaces, RoundingMode.HALF_UP), currency)
        }
    }
}
