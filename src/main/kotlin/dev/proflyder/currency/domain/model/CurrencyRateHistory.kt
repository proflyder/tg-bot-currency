package dev.proflyder.currency.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * История курсов валют
 * Хранится в JSON файле для персистентности между запусками
 */
@Serializable
data class CurrencyRateHistory(
    val records: List<CurrencyRateRecord> = emptyList()
)

/**
 * Одна запись в истории курсов
 * @property timestamp Дата и время парсинга
 * @property rates Курсы валют на момент парсинга
 */
@Serializable
data class CurrencyRateRecord(
    val timestamp: Instant,
    val rates: CurrencyRateSnapshot
)

/**
 * Снимок курсов валют для сериализации
 * (CurrencyRate из domain не serializable, поэтому создаем snapshot)
 */
@Serializable
data class CurrencyRateSnapshot(
    val usdToKzt: ExchangeRateSnapshot,
    val rubToKzt: ExchangeRateSnapshot
)

@Serializable
data class ExchangeRateSnapshot(
    val buy: Double,
    val sell: Double
)

/**
 * Extension для конвертации CurrencyRate в Snapshot
 */
fun CurrencyRate.toSnapshot(): CurrencyRateSnapshot {
    return CurrencyRateSnapshot(
        usdToKzt = ExchangeRateSnapshot(
            buy = this.usdToKzt.buy,
            sell = this.usdToKzt.sell
        ),
        rubToKzt = ExchangeRateSnapshot(
            buy = this.rubToKzt.buy,
            sell = this.rubToKzt.sell
        )
    )
}
