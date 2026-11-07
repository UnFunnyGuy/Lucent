package app.lucent.firefly.core.database.model

enum class SyncQueueStatus {
    /** Waiting to be pushed (or being retried after a transient error). */
    PENDING,

    /** Rejected by the server or out of retries; needs the user to retry or discard. */
    FAILED,
}
