package app.lucent.firefly.core.database

import app.lucent.firefly.core.common.network.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

/** Removes every locally stored record, for example when a different server or user signs in. */
fun interface LocalDataEraser {
    suspend fun eraseAll()
}

@Factory
class RoomLocalDataEraser(
    private val database: LucentDatabase,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LocalDataEraser {

    override suspend fun eraseAll() = withContext(ioDispatcher) {
        database.clearAllTables()
    }
}
