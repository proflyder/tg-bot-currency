package dev.proflyder.currency.data.dto

import kotlinx.serialization.Serializable

/**
 * Response DTO для endpoint принудительного запуска обновления курсов
 *
 * Статус успеха определяется HTTP статус кодом (200 = успех).
 * При ошибке возвращается ErrorResponse.
 */
@Serializable
data class TriggerResponseDto(
    val message: String,
    val executionTimeMs: Long
)
