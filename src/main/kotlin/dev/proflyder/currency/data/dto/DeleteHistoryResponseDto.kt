package dev.proflyder.currency.data.dto

import kotlinx.serialization.Serializable

/**
 * Response DTO для endpoint удаления всей истории курсов валют
 *
 * Статус успеха определяется HTTP статус кодом (200 = успех).
 * При ошибке возвращается ErrorResponse.
 */
@Serializable
data class DeleteHistoryResponseDto(
    val deletedCount: Int,
    val message: String
)
