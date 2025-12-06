package dev.proflyder.currency.data.dto

import dev.proflyder.currency.domain.model.CurrencyRateRecord
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * DTO для ответа API с историей курсов валют
 *
 * Статус успеха определяется HTTP статус кодом (200 = успех, 4xx/5xx = ошибка).
 * Для ошибок используется ErrorResponse с унифицированным форматом.
 */
@Serializable
data class CurrencyHistoryResponseDto(
    val records: List<CurrencyRateRecordDto>,
    val totalCount: Int
)


/**
 * DTO одной записи курса валюты
 */
@Serializable
data class CurrencyRateRecordDto(
    val timestamp: Instant,
    val rates: CurrencyRatesDto
)

/**
 * DTO курсов валют
 */
@Serializable
data class CurrencyRatesDto(
    val usdToKzt: ExchangeRateDto,
    val rubToKzt: ExchangeRateDto
)

/**
 * DTO курса обмена
 */
@Serializable
data class ExchangeRateDto(
    val buy: Double,
    val sell: Double
)

/**
 * Extension функция для конвертации domain модели в DTO
 */
fun CurrencyRateRecord.toDto(): CurrencyRateRecordDto {
    return CurrencyRateRecordDto(
        timestamp = this.timestamp,
        rates = CurrencyRatesDto(
            usdToKzt = ExchangeRateDto(
                buy = this.rates.usdToKzt.buy,
                sell = this.rates.usdToKzt.sell
            ),
            rubToKzt = ExchangeRateDto(
                buy = this.rates.rubToKzt.buy,
                sell = this.rates.rubToKzt.sell
            )
        )
    )
}
