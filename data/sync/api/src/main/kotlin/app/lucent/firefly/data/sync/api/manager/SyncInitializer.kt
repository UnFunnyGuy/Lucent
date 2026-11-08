package app.lucent.firefly.data.sync.api.manager

/** Starts session-driven sync scheduling. Called once from the application. */
fun interface SyncInitializer {
    fun initialize()
}
