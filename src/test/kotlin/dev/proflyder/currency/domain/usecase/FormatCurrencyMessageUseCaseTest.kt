package dev.proflyder.currency.domain.usecase

import dev.proflyder.currency.TestFixtures
import dev.proflyder.currency.domain.model.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested

@DisplayName("FormatCurrencyMessageUseCase")
class FormatCurrencyMessageUseCaseTest {

    private lateinit var useCase: FormatCurrencyMessageUseCase

    @BeforeEach
    fun setup() {
        useCase = FormatCurrencyMessageUseCase()
    }

    @Nested
    @DisplayName("Базовое форматирование курсов")
    inner class BasicFormatting {

        @Test
        fun `должен отформатировать сообщение без алертов`() {
            // Arrange
            val rates = TestFixtures.sampleCurrencyRate

            // Act
            val message = useCase(rates, emptyList())

            // Assert
            message shouldContain "💱 *Курсы валют на kurs.kz*"
            message shouldContain "🇺🇸 *USD → KZT*"
            message shouldContain "🇷🇺 *RUB → KZT*"
            message shouldContain "Покупка:"
            message shouldContain "Продажа:"

            // Не должно быть секций с алертами
            message shouldNotContain "ПРЕДУПРЕЖДЕНИЯ"
            message shouldNotContain "КРИТИЧЕСКИЕ ИЗМЕНЕНИЯ"
        }

        @Test
        fun `должен корректно отформатировать числа с двумя знаками после запятой`() {
            // Arrange
            val rates = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 485.50, sell = 487.20),
                rubToKzt = ExchangeRate(buy = 4.85, sell = 4.92)
            )

            // Act
            val message = useCase(rates, emptyList())

            // Assert
            // Проверяем числа (может быть как запятая, так и точка в зависимости от локали)
            val hasUsdBuy = message.contains("485,50") || message.contains("485.50")
            val hasUsdSell = message.contains("487,20") || message.contains("487.20")
            val hasRubBuy = message.contains("4,85") || message.contains("4.85")
            val hasRubSell = message.contains("4,92") || message.contains("4.92")

            hasUsdBuy shouldBe true
            hasUsdSell shouldBe true
            hasRubBuy shouldBe true
            hasRubSell shouldBe true
        }

        @Test
        fun `должен добавить символ тенге`() {
            // Arrange
            val rates = TestFixtures.sampleCurrencyRate

            // Act
            val message = useCase(rates, emptyList())

            // Assert
            message shouldContain "₸"
        }
    }

    @Nested
    @DisplayName("Форматирование алертов WARNING")
    inner class WarningAlerts {

        @Test
        fun `должен добавить секцию ПРЕДУПРЕЖДЕНИЯ при наличии WARNING алертов`() {
            // Arrange
            val rates = TestFixtures.sampleCurrencyRate
            val alert = CurrencyAlert(
                level = AlertLevel.WARNING,
                period = AlertPeriod.DAY,
                pair = CurrencyPair.USD_TO_KZT,
                direction = ChangeDirection.UP,
                changePercent = 1.5,
                oldRate = 480.0,
                newRate = 487.2
            )

            // Act
            val message = useCase(rates, listOf(alert))

            // Assert
            message shouldContain "⚠️ *ПРЕДУПРЕЖДЕНИЯ*"
            message shouldContain "─────────────────────────"
        }

        @Test
        fun `должен отформатировать WARNING алерт с ростом курса`() {
            // Arrange
            val rates = TestFixtures.sampleCurrencyRate
            val alert = CurrencyAlert(
                level = AlertLevel.WARNING,
                period = AlertPeriod.HOUR,
                pair = CurrencyPair.USD_TO_KZT,
                direction = ChangeDirection.UP,
                changePercent = 0.8,
                oldRate = 480.0,
                newRate = 483.84
            )

            // Act
            val message = useCase(rates, listOf(alert))

            // Assert
            message shouldContain "📈" // Emoji роста
            message shouldContain "🇺🇸" // Флаг США
            message shouldContain "USD → KZT"
            message shouldContain "вырос"
            val hasPercent = message.contains("0,80") || message.contains("0.80")
            hasPercent shouldBe true
            message shouldContain "час" // Период
            message shouldContain "480" // Старый курс
            message shouldContain "483" // Новый курс
        }

        @Test
        fun `должен отформатировать WARNING алерт с падением курса`() {
            // Arrange
            val rates = TestFixtures.sampleCurrencyRate
            val alert = CurrencyAlert(
                level = AlertLevel.WARNING,
                period = AlertPeriod.WEEK,
                pair = CurrencyPair.RUB_TO_KZT,
                direction = ChangeDirection.DOWN,
                changePercent = -2.5,
                oldRate = 5.00,
                newRate = 4.875
            )

            // Act
            val message = useCase(rates, listOf(alert))

            // Assert
            message shouldContain "📉" // Emoji падения
            message shouldContain "🇷🇺" // Флаг России
            message shouldContain "RUB → KZT"
            message shouldContain "упал"
            val hasPercent = message.contains("2,50") || message.contains("2.50")
            hasPercent shouldBe true
            message shouldContain "неделю" // Период
        }

        @Test
        fun `должен отформатировать несколько WARNING алертов`() {
            // Arrange
            val rates = TestFixtures.sampleCurrencyRate
            val alerts = listOf(
                CurrencyAlert(
                    level = AlertLevel.WARNING,
                    period = AlertPeriod.DAY,
                    pair = CurrencyPair.USD_TO_KZT,
                    direction = ChangeDirection.UP,
                    changePercent = 1.2,
                    oldRate = 480.0,
                    newRate = 485.76
                ),
                CurrencyAlert(
                    level = AlertLevel.WARNING,
                    period = AlertPeriod.HOUR,
                    pair = CurrencyPair.RUB_TO_KZT,
                    direction = ChangeDirection.DOWN,
                    changePercent = -0.6,
                    oldRate = 4.95,
                    newRate = 4.92
                )
            )

            // Act
            val message = useCase(rates, alerts)

            // Assert
            message shouldContain "⚠️ *ПРЕДУПРЕЖДЕНИЯ*"
            // Должны быть оба алерта
            message shouldContain "USD → KZT"
            message shouldContain "RUB → KZT"
            message shouldContain "вырос"
            message shouldContain "упал"
        }
    }

    @Nested
    @DisplayName("Форматирование алертов CRITICAL")
    inner class CriticalAlerts {

        @Test
        fun `должен добавить секцию КРИТИЧЕСКИЕ ИЗМЕНЕНИЯ при наличии CRITICAL алертов`() {
            // Arrange
            val rates = TestFixtures.sampleCurrencyRate
            val alert = CurrencyAlert(
                level = AlertLevel.CRITICAL,
                period = AlertPeriod.MONTH,
                pair = CurrencyPair.USD_TO_KZT,
                direction = ChangeDirection.UP,
                changePercent = 5.5,
                oldRate = 460.0,
                newRate = 485.3
            )

            // Act
            val message = useCase(rates, listOf(alert))

            // Assert
            message shouldContain "🚨 *КРИТИЧЕСКИЕ ИЗМЕНЕНИЯ*"
            message shouldContain "─────────────────────────"
        }

        @Test
        fun `должен отформатировать CRITICAL алерт`() {
            // Arrange
            val rates = TestFixtures.sampleCurrencyRate
            val alert = CurrencyAlert(
                level = AlertLevel.CRITICAL,
                period = AlertPeriod.DAY,
                pair = CurrencyPair.USD_TO_KZT,
                direction = ChangeDirection.DOWN,
                changePercent = -2.5,
                oldRate = 490.0,
                newRate = 478.25
            )

            // Act
            val message = useCase(rates, listOf(alert))

            // Assert
            message shouldContain "🚨 *КРИТИЧЕСКИЕ ИЗМЕНЕНИЯ*"
            message shouldContain "📉"
            message shouldContain "USD → KZT"
            message shouldContain "упал"
            val hasPercent = message.contains("2,50") || message.contains("2.50")
            hasPercent shouldBe true
            message shouldContain "сутки"
        }
    }

    @Nested
    @DisplayName("Смешанное форматирование")
    inner class MixedFormatting {

        @Test
        fun `должен отформатировать сообщение с WARNING и CRITICAL алертами`() {
            // Arrange
            val rates = TestFixtures.sampleCurrencyRate
            val alerts = listOf(
                CurrencyAlert(
                    level = AlertLevel.WARNING,
                    period = AlertPeriod.HOUR,
                    pair = CurrencyPair.USD_TO_KZT,
                    direction = ChangeDirection.UP,
                    changePercent = 0.8,
                    oldRate = 480.0,
                    newRate = 483.84
                ),
                CurrencyAlert(
                    level = AlertLevel.CRITICAL,
                    period = AlertPeriod.WEEK,
                    pair = CurrencyPair.RUB_TO_KZT,
                    direction = ChangeDirection.DOWN,
                    changePercent = -4.2,
                    oldRate = 5.10,
                    newRate = 4.886
                )
            )

            // Act
            val message = useCase(rates, alerts)

            // Assert
            // Должны быть обе секции
            message shouldContain "⚠️ *ПРЕДУПРЕЖДЕНИЯ*"
            message shouldContain "🚨 *КРИТИЧЕСКИЕ ИЗМЕНЕНИЯ*"

            // Проверяем порядок (WARNING должен идти перед CRITICAL)
            val warningIndex = message.indexOf("ПРЕДУПРЕЖДЕНИЯ")
            val criticalIndex = message.indexOf("КРИТИЧЕСКИЕ ИЗМЕНЕНИЯ")
            (warningIndex < criticalIndex) shouldBe true
        }

        @Test
        fun `должен правильно группировать алерты по уровню`() {
            // Arrange
            val rates = TestFixtures.sampleCurrencyRate
            val alerts = listOf(
                // 2 WARNING
                CurrencyAlert(
                    level = AlertLevel.WARNING,
                    period = AlertPeriod.HOUR,
                    pair = CurrencyPair.USD_TO_KZT,
                    direction = ChangeDirection.UP,
                    changePercent = 0.7,
                    oldRate = 480.0,
                    newRate = 483.36
                ),
                CurrencyAlert(
                    level = AlertLevel.WARNING,
                    period = AlertPeriod.DAY,
                    pair = CurrencyPair.RUB_TO_KZT,
                    direction = ChangeDirection.UP,
                    changePercent = 1.5,
                    oldRate = 4.80,
                    newRate = 4.872
                ),
                // 1 CRITICAL
                CurrencyAlert(
                    level = AlertLevel.CRITICAL,
                    period = AlertPeriod.MONTH,
                    pair = CurrencyPair.USD_TO_KZT,
                    direction = ChangeDirection.UP,
                    changePercent = 6.0,
                    oldRate = 460.0,
                    newRate = 487.6
                )
            )

            // Act
            val message = useCase(rates, alerts)

            // Assert
            message shouldContain "⚠️ *ПРЕДУПРЕЖДЕНИЯ*"
            message shouldContain "🚨 *КРИТИЧЕСКИЕ ИЗМЕНЕНИЯ*"

            // В секции ПРЕДУПРЕЖДЕНИЯ должно быть 2 алерта
            val warningSection = message.substringAfter("⚠️ *ПРЕДУПРЕЖДЕНИЯ*")
                .substringBefore("🚨 *КРИТИЧЕСКИЕ ИЗМЕНЕНИЯ*")
            val warningAlertCount = warningSection.split("📈", "📉").size - 1
            warningAlertCount shouldBe 2
        }
    }

    @Nested
    @DisplayName("Периоды времени")
    inner class TimePeriods {

        @Test
        fun `должен правильно отформатировать все периоды времени`() {
            // Arrange
            val rates = TestFixtures.sampleCurrencyRate

            val periods = listOf(
                AlertPeriod.HOUR to "час",
                AlertPeriod.DAY to "сутки",
                AlertPeriod.WEEK to "неделю",
                AlertPeriod.MONTH to "месяц"
            )

            periods.forEach { (period, expectedText) ->
                val alert = CurrencyAlert(
                    level = AlertLevel.WARNING,
                    period = period,
                    pair = CurrencyPair.USD_TO_KZT,
                    direction = ChangeDirection.UP,
                    changePercent = 1.0,
                    oldRate = 480.0,
                    newRate = 484.8
                )

                // Act
                val message = useCase(rates, listOf(alert))

                // Assert
                message shouldContain expectedText
            }
        }
    }

}
