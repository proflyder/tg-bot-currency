package dev.proflyder.currency.data.dto

import kotlinx.serialization.Serializable

/**
 * Response DTO для endpoint удаления всей истории курсов валют
 */
@Serializable
data class DeleteHistoryResponseDto(
    val success: Boolean,
    val message: String,
    val deletedCount: Int? = null
)
