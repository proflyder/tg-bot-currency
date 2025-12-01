package dev.proflyder.currency.data.dto

import kotlinx.serialization.Serializable

/**
 * Response DTO для endpoint принудительного запуска обновления курсов
 */
@Serializable
data class TriggerResponseDto(
    val success: Boolean,
    val message: String
)
