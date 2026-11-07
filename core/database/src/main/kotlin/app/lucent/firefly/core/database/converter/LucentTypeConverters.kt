package app.lucent.firefly.core.database.converter

import androidx.room.TypeConverter
import app.lucent.firefly.core.database.model.SyncAction
import app.lucent.firefly.core.database.model.SyncEntityType
import app.lucent.firefly.core.database.model.SyncQueueStatus
import app.lucent.firefly.core.database.model.SyncStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LucentTypeConverters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter
    fun fromSyncAction(action: SyncAction): String = action.name

    @TypeConverter
    fun toSyncAction(value: String): SyncAction = SyncAction.valueOf(value)

    @TypeConverter
    fun fromSyncEntityType(type: SyncEntityType): String = type.name

    @TypeConverter
    fun toSyncEntityType(value: String): SyncEntityType = SyncEntityType.valueOf(value)

    @TypeConverter
    fun fromSyncQueueStatus(status: SyncQueueStatus): String = status.name

    @TypeConverter
    fun toSyncQueueStatus(value: String): SyncQueueStatus = SyncQueueStatus.valueOf(value)

    @TypeConverter
    fun fromStringList(list: List<String>): String = json.encodeToString(list)

    @TypeConverter
    fun toStringList(value: String): List<String> = runCatching {
        json.decodeFromString<List<String>>(value)
    }.getOrDefault(emptyList())
}
