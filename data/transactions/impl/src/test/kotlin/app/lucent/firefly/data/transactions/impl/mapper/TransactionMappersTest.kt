package app.lucent.firefly.data.transactions.impl.mapper

import app.lucent.firefly.core.common.model.Currency
import app.lucent.firefly.core.common.model.Money
import app.lucent.firefly.data.transactions.api.model.TransactionDraft
import app.lucent.firefly.data.transactions.api.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.OffsetDateTime

class TransactionMappersTest {

    @Test
    fun parsesFireflyOffsetDates() {
        assertEquals(1_759_276_800_000L, parseIsoInstant("2025-10-01T00:00:00+00:00"))
        assertEquals(1_759_273_200_000L, parseIsoInstant("2025-10-01T00:00:00+01:00"))
        assertEquals(1_759_276_800_000L, parseIsoInstant("2025-10-01T00:00:00Z"))
    }

    @Test
    fun storeDtoCarriesCurrencyAndOffsetDateAndLeavesCounterpartyEmpty() {
        val draft = TransactionDraft(
            type = TransactionType.WITHDRAWAL,
            amount = Money(BigDecimal("4.50"), Currency(code = "EUR", symbol = "€")),
            description = "Coffee",
            sourceId = "1",
            sourceName = "Checking",
            date = 1_759_276_800_000L,
        )

        val split = draft.toStoreDto(externalId = "g-1").transactions.single()

        assertEquals("withdrawal", split.type)
        assertEquals("4.50", split.amount)
        assertEquals("EUR", split.currencyCode)
        assertEquals("1", split.sourceId)
        assertNull(split.destinationId)
        assertNull(split.destinationName)
        assertEquals("g-1", split.externalId)
        assertEquals(1_759_276_800_000L, OffsetDateTime.parse(split.date).toInstant().toEpochMilli())
    }
}
