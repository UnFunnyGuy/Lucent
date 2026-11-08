package app.lucent.firefly.data.auth.impl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SystemInfoDto(
    val data: SystemInfoDataDto,
)

@Serializable
data class SystemInfoDataDto(
    val version: String,
    @SerialName("api_version")
    val apiVersion: String? = null,
    @SerialName("php_version")
    val phpVersion: String? = null,
    val os: String? = null,
    val driver: String? = null,
)
