package dev.proflyder.currency.data.dto

import kotlinx.serialization.Serializable

/**
 * Response DTO для endpoint последнего актуального курса валют
 *
 * Статус успеха определяется HTTP статус кодом:
 * - 200 OK: курс найден, возвращается CurrencyRateRecordDto
 * - 404 Not Found: курсов нет в базе, возвращается ErrorResponse
 * - 500 Internal Server Error: ошибка сервера, возвращается ErrorResponse
 */
@Serializable
data class LatestCurrencyRateResponseDto(
    val timestamp: kotlinx.datetime.Instant,
    val rates: CurrencyRatesDto
)
