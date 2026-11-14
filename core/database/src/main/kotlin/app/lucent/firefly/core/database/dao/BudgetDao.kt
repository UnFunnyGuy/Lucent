package app.lucent.firefly.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.lucent.firefly.core.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE active = 1 ORDER BY name ASC")
    fun observeActiveBudgets(): Flow<List<BudgetEntity>>

    @Upsert
    suspend fun upsert(budgets: List<BudgetEntity>)
}
