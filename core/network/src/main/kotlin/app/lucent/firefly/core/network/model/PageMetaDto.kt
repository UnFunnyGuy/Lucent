package app.lucent.firefly.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PageMetaDto(
    val pagination: PaginationDto? = null,
)

@Serializable
data class PaginationDto(
    val total: Int,
    val count: Int,
    @SerialName("per_page") val perPage: Int,
    @SerialName("current_page") val currentPage: Int,
    @SerialName("total_pages") val totalPages: Int,
)

val PageMetaDto?.hasNextPage: Boolean
    get() = this?.pagination?.let { it.currentPage < it.totalPages } ?: false
