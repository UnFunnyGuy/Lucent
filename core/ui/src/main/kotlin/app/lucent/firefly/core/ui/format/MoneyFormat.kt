package app.lucent.firefly.core.ui.format

import app.lucent.firefly.core.common.model.Money
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formats money for display with grouping separators: `€1,240.50`, `−€12.00`, `+€300.00`.
 * The sign goes before the symbol and uses a real minus sign.
 */
fun Money.formatAmount(
    showPlusSign: Boolean = false,
    locale: Locale = Locale.getDefault(),
): String {
    val pattern = if (currency.decimalPlaces > 0) "#,##0." + "0".repeat(currency.decimalPlaces) else "#,##0"
    val formatter = DecimalFormat(pattern, DecimalFormatSymbols.getInstance(locale))
    val digits = formatter.format(amount.abs().setScale(currency.decimalPlaces, RoundingMode.HALF_UP))
    val sign =
        when {
            amount.signum() < 0 -> "−"
            showPlusSign && amount.signum() > 0 -> "+"
            else -> ""
        }
    return "$sign${currency.symbol}$digits"
}
