package app.lucent.firefly.data.accounts.impl.dto

import app.lucent.firefly.core.network.model.PageMetaDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountArrayDto(
    val data: List<AccountReadDto> = emptyList(),
    val meta: PageMetaDto? = null,
)

@Serializable
data class AccountSingleDto(
    val data: AccountReadDto,
)

@Serializable
data class AccountReadDto(
    val type: String,
    val id: String,
    val attributes: AccountPropertiesDto,
)

@Serializable
data class AccountPropertiesDto(
    val name: String,
    val type: String,
    @SerialName("account_role") val accountRole: String? = null,
    val active: Boolean? = null,
    val order: Int? = null,
    @SerialName("include_net_worth") val includeNetWorth: Boolean? = null,
    @SerialName("current_balance") val currentBalance: String? = null,
    @SerialName("pc_current_balance") val primaryCurrencyBalance: String? = null,
    @SerialName("currency_id") val currencyId: String? = null,
    @SerialName("currency_code") val currencyCode: String? = null,
    @SerialName("currency_symbol") val currencySymbol: String? = null,
    @SerialName("currency_decimal_places") val currencyDecimalPlaces: Int? = null,
    @SerialName("primary_currency_code") val primaryCurrencyCode: String? = null,
    @SerialName("primary_currency_symbol") val primaryCurrencySymbol: String? = null,
    @SerialName("primary_currency_decimal_places") val primaryCurrencyDecimalPlaces: Int? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
