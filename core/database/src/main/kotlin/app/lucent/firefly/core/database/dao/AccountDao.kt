package app.lucent.firefly.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.lucent.firefly.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY `order` ASC, name ASC")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE active = 1 ORDER BY `order` ASC, name ASC")
    fun observeActiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts")
    suspend fun getAccounts(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE localId = :localId")
    fun observeAccount(localId: String): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE localId = :localId")
    suspend fun getAccountByLocalId(localId: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE serverId = :serverId")
    suspend fun getAccountByServerId(serverId: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(accounts: List<AccountEntity>)

    @Query("DELETE FROM accounts WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: String)
}
