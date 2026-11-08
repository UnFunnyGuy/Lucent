package app.lucent.firefly.data.accounts.api.model

enum class AccountType {
    ASSET,
    EXPENSE,
    REVENUE,
    LIABILITY,
    OTHER;

    companion object {
        fun fromString(value: String): AccountType = when (value.lowercase()) {
            "asset" -> ASSET
            "expense" -> EXPENSE
            "revenue" -> REVENUE
            "liability", "liabilities" -> LIABILITY
            else -> OTHER
        }
    }
}
