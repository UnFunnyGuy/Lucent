package app.lucent.firefly.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.lucent.firefly.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsert(categories: List<CategoryEntity>)
}
