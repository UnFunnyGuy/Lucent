package app.lucent.firefly.data.accounts.impl.repository

import app.lucent.firefly.core.common.network.IoDispatcher
import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.database.dao.AccountDao
import app.lucent.firefly.core.database.dao.TransactionDao
import app.lucent.firefly.core.database.entity.AccountEntity
import app.lucent.firefly.core.database.model.SyncStatus
import app.lucent.firefly.core.network.model.hasNextPage
import app.lucent.firefly.data.accounts.api.model.Account
import app.lucent.firefly.data.accounts.api.repository.AccountRepository
import app.lucent.firefly.data.accounts.impl.balance.computePendingDeltas
import app.lucent.firefly.data.accounts.impl.datasource.AccountRemoteDataSource
import app.lucent.firefly.data.accounts.impl.dto.AccountReadDto
import app.lucent.firefly.data.accounts.impl.mapper.toDomain
import app.lucent.firefly.data.accounts.impl.mapper.toEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Singleton
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class AccountRepositoryImpl(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val remoteDataSource: AccountRemoteDataSource,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AccountRepository {
    override fun observeAccounts(): Flow<List<Account>> = withPendingDeltas(accountDao.observeAccounts()).flowOn(ioDispatcher)

    override fun observeActiveAccounts(): Flow<List<Account>> = withPendingDeltas(accountDao.observeActiveAccounts()).flowOn(ioDispatcher)

    override fun observeAccount(id: String): Flow<Account?> =
        accountDao
            .observeAccount(id)
            .flatMapLatest { entity ->
                if (entity == null) {
                    flowOf(null)
                } else {
                    transactionDao.observeUnsyncedSplits().map { splits ->
                        entity.toDomain(computePendingDeltas(listOf(entity), splits)[entity.localId] ?: BigDecimal.ZERO)
                    }
                }
            }.flowOn(ioDispatcher)

    private fun withPendingDeltas(accounts: Flow<List<AccountEntity>>): Flow<List<Account>> =
        combine(accounts, transactionDao.observeUnsyncedSplits()) { entities, splits ->
            val deltas = computePendingDeltas(entities, splits)
            entities.map { it.toDomain(deltas[it.localId] ?: BigDecimal.ZERO) }
        }

    override suspend fun refreshAccounts(): Result<List<Account>> =
        withContext(ioDispatcher) {
            val dtos = mutableListOf<AccountReadDto>()
            for (type in SYNCED_ACCOUNT_TYPES) {
                when (val result = fetchAllPages(type)) {
                    is Result.Success -> dtos += result.data
                    is Result.Error -> return@withContext result
                }
            }

            val localAccounts = accountDao.getAccounts()
            val localIdByServerId =
                localAccounts
                    .mapNotNull { account ->
                        account.serverId?.let { it to account.localId }
                    }.toMap()
            val entities = dtos.map { dto -> dto.toEntity(localIdByServerId[dto.id]) }
            val remoteIds = dtos.mapTo(mutableSetOf()) { it.id }

            accountDao.insertOrReplace(entities)
            localAccounts
                .filter { it.syncStatus == SyncStatus.SYNCED && it.serverId !in remoteIds }
                .forEach { accountDao.deleteByLocalId(it.localId) }

            Result.Success(entities.map { it.toDomain() })
        }

    private suspend fun fetchAllPages(type: String): Result<List<AccountReadDto>> {
        val accounts = mutableListOf<AccountReadDto>()
        var page = 1
        while (true) {
            when (val result = remoteDataSource.getAccounts(type = type, page = page)) {
                is Result.Success -> {
                    accounts += result.data.data
                    if (!result.data.meta.hasNextPage) return Result.Success(accounts)
                    page++
                }
                is Result.Error -> return result
            }
        }
    }

    override suspend fun getAccount(id: String): Result<Account> =
        withContext(ioDispatcher) {
            val entity = accountDao.getAccountByLocalId(id)
            if (entity != null) {
                Result.Success(entity.toDomain())
            } else {
                Result.Error(AppError.Common("Account not found with ID: $id"))
            }
        }

    private companion object {
        /** Accounts the user owns; expense and revenue accounts are counterparties, not balances. */
        val SYNCED_ACCOUNT_TYPES = listOf("asset", "liabilities")
    }
}
