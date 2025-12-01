package dev.proflyder.currency.data.dto

import kotlinx.serialization.Serializable

/**
 * Response DTO для endpoint последнего актуального курса валют
 */
@Serializable
data class LatestCurrencyRateResponseDto(
    val success: Boolean,
    val data: CurrencyRateRecordDto?,
    val message: String? = null
)
