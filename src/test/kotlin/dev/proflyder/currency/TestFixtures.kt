package dev.proflyder.currency

import dev.proflyder.currency.domain.model.CurrencyRate
import dev.proflyder.currency.domain.model.ExchangeRate
import kotlinx.datetime.Instant

/**
 * Тестовые данные для использования в unit и интеграционных тестах
 */
object TestFixtures {

    // Тестовые курсы валют
    val sampleExchangeRateUsd = ExchangeRate(
        buy = 485.50,
        sell = 487.20
    )

    val sampleExchangeRateRub = ExchangeRate(
        buy = 4.85,
        sell = 4.92
    )

    val sampleCurrencyRate = CurrencyRate(
        usdToKzt = sampleExchangeRateUsd,
        rubToKzt = sampleExchangeRateRub
    )

    // Тестовый timestamp
    val sampleTimestamp: Instant = Instant.parse("2025-11-30T10:00:00Z")

    // Тестовые ID для Telegram
    const val TEST_CHAT_ID = "123456789"

    // Пример отформатированного сообщения для Telegram
    val expectedTelegramMessage = """
        💱 *Курсы валют на kurs.kz*

        🇺🇸 *USD → KZT*
          Покупка: 485,50 ₸
          Продажа: 487,20 ₸

        🇷🇺 *RUB → KZT*
          Покупка: 4,85 ₸
          Продажа: 4,92 ₸

    """.trimIndent()
}
