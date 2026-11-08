package app.lucent.firefly.data.auth.impl.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserSingleDto(
    val data: UserReadDto,
)

@Serializable
data class UserReadDto(
    val id: String,
    val attributes: UserAttributesDto,
)

@Serializable
data class UserAttributesDto(
    val email: String,
    val blocked: Boolean = false,
    val role: String? = null,
)
