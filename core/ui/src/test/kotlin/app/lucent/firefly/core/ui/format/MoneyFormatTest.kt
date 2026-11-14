package app.lucent.firefly.core.ui.format

import app.lucent.firefly.core.common.model.Currency
import app.lucent.firefly.core.common.model.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MoneyFormatTest {
    private val eur = Currency(code = "EUR", symbol = "€")

    @Test
    fun groupsThousandsAndKeepsDecimals() {
        assertEquals("€1,240.50", Money.of("1240.5", eur).formatAmount(locale = Locale.US))
    }

    @Test
    fun putsTheSignBeforeTheSymbol() {
        assertEquals("−€12.00", Money.of("-12", eur).formatAmount(locale = Locale.US))
        assertEquals("+€300.00", Money.of("300", eur).formatAmount(showPlusSign = true, locale = Locale.US))
    }

    @Test
    fun respectsCurrenciesWithoutDecimals() {
        assertEquals(
            "¥1,500",
            Money.of("1500", Currency(code = "JPY", symbol = "¥", decimalPlaces = 0)).formatAmount(locale = Locale.US),
        )
    }
}
