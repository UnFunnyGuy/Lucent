package app.lucent.firefly.data.transactions.api.model

enum class TransactionType {
    WITHDRAWAL,
    DEPOSIT,
    TRANSFER,
    RECONCILIATION,
    OTHER,
    ;

    companion object {
        fun fromString(value: String): TransactionType =
            when (value.lowercase()) {
                "withdrawal" -> WITHDRAWAL
                "deposit" -> DEPOSIT
                "transfer" -> TRANSFER
                "reconciliation" -> RECONCILIATION
                else -> OTHER
            }
    }
}
