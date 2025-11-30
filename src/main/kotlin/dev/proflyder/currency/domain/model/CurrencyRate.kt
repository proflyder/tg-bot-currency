package dev.proflyder.currency.domain.model

data class CurrencyRate(
    val usdToKzt: ExchangeRate,
    val rubToKzt: ExchangeRate
)

data class ExchangeRate(
    val buy: Double,
    val sell: Double
)
