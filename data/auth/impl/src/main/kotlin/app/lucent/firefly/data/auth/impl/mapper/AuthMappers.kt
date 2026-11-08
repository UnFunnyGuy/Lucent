package app.lucent.firefly.data.auth.impl.mapper

import app.lucent.firefly.data.auth.api.model.AuthUser
import app.lucent.firefly.data.auth.api.model.ServerInfo
import app.lucent.firefly.data.auth.impl.dto.SystemInfoDto
import app.lucent.firefly.data.auth.impl.dto.UserSingleDto

fun SystemInfoDto.toDomain(): ServerInfo = ServerInfo(
    version = data.version,
    apiVersion = data.apiVersion ?: data.version,
    phpVersion = data.phpVersion ?: "",
    os = data.os ?: "",
)

fun UserSingleDto.toDomain(): AuthUser = AuthUser(
    id = data.id,
    email = data.attributes.email,
    role = data.attributes.role ?: "user",
    isBlocked = data.attributes.blocked,
)
