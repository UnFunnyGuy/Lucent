package app.lucent.firefly.core.database.model

enum class EnqueueResult {
    /** A new operation was queued. */
    QUEUED,

    /** Folded into an operation already queued for the entity. */
    MERGED,

    /** A DELETE cancelled a CREATE that never reached the server; nothing is left to push. */
    CANCELLED_CREATE,
}
