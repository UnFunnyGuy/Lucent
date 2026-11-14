package app.lucent.firefly.core.common.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {
    private val eur = Currency(id = "1", code = "EUR", symbol = "€", decimalPlaces = 2)
    private val usd = Currency(id = "2", code = "USD", symbol = "$", decimalPlaces = 2)

    @Test
    fun plusAddsAmountsCorrectly() {
        val a = Money.of("10.50", eur)
        val b = Money.of("4.25", eur)
        val result = a + b
        assertEquals(BigDecimal("14.75"), result.amount)
        assertEquals("€14.75", result.formatDisplay())
    }

    @Test
    fun minusSubtractsAmountsCorrectly() {
        val a = Money.of("10.50", eur)
        val b = Money.of("4.25", eur)
        val result = a - b
        assertEquals(BigDecimal("6.25"), result.amount)
        assertEquals("€6.25", result.formatDisplay())
    }

    @Test(expected = IllegalArgumentException::class)
    fun plusThrowsOnCurrencyMismatch() {
        val a = Money.of("10.00", eur)
        val b = Money.of("5.00", usd)
        val unused = a + b
    }

    @Test
    fun signChecksWorkCorrectly() {
        val positive = Money.of("1.00", eur)
        val negative = Money.of("-1.00", eur)
        val zero = Money.zero(eur)

        assertTrue(positive.isPositive)
        assertFalse(positive.isNegative)

        assertTrue(negative.isNegative)
        assertFalse(negative.isPositive)

        assertTrue(zero.isZero)
    }
}
