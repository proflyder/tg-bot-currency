package dev.proflyder.currency.domain.usecase

import dev.proflyder.currency.TestFixtures
import dev.proflyder.currency.domain.model.*
import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.*
import kotlin.time.Duration.Companion.hours

@DisplayName("CheckCurrencyThresholdsUseCase")
class CheckCurrencyThresholdsUseCaseTest {

    private lateinit var historyRepository: CurrencyHistoryRepository
    private lateinit var useCase: CheckCurrencyThresholdsUseCase

    @BeforeEach
    fun setup() {
        historyRepository = mockk()
        useCase = CheckCurrencyThresholdsUseCase(historyRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Nested
    @DisplayName("Сценарии с превышением порогов")
    inner class ThresholdExceeded {

        @Test
        fun `должен вернуть WARNING алерт при превышении порога предупреждения`() = runTest {
            // Arrange
            val currentRates = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 485.0, sell = 487.85), // 487.85 текущий
                rubToKzt = ExchangeRate(buy = 4.80, sell = 4.90)
            )

            // Час назад было 485.0, сейчас 487.85 = +0.59% (выше WARNING 0.5%, ниже CRITICAL 1.0%)
            val historicalRecord = CurrencyRateRecord(
                timestamp = Clock.System.now() - 2.hours,
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 480.0, sell = 485.0),
                    rubToKzt = ExchangeRateSnapshot(buy = 4.80, sell = 4.90)
                )
            )

            coEvery { historyRepository.getRecordBefore(any()) } returns Result.success(historicalRecord)

            // Act
            val result = useCase(currentRates)

            // Assert
            result.isSuccess shouldBe true
            val alerts = result.getOrThrow()

            // Должны быть алерты для USD
            val usdAlerts = alerts.filter { it.pair == CurrencyPair.USD_TO_KZT }
            (usdAlerts.size > 0) shouldBe true

            // Проверяем что есть WARNING алерты
            val warningAlerts = usdAlerts.filter { it.level == AlertLevel.WARNING }
            (warningAlerts.size > 0) shouldBe true
        }

        @Test
        fun `должен вернуть CRITICAL алерт при превышении критического порога`() = runTest {
            // Arrange
            val currentRates = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 490.0, sell = 500.0), // 500.0 текущий
                rubToKzt = ExchangeRate(buy = 4.80, sell = 4.90)
            )

            // Час назад было 485.0, сейчас 500.0 = +3.09% (выше CRITICAL 1.0%)
            val historicalRecord = CurrencyRateRecord(
                timestamp = Clock.System.now() - 2.hours,
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 480.0, sell = 485.0),
                    rubToKzt = ExchangeRateSnapshot(buy = 4.80, sell = 4.90)
                )
            )

            coEvery { historyRepository.getRecordBefore(any()) } returns Result.success(historicalRecord)

            // Act
            val result = useCase(currentRates)

            // Assert
            result.isSuccess shouldBe true
            val alerts = result.getOrThrow()

            // Проверяем что есть CRITICAL алерты для USD
            val criticalAlerts = alerts.filter {
                it.pair == CurrencyPair.USD_TO_KZT && it.level == AlertLevel.CRITICAL
            }
            (criticalAlerts.size > 0) shouldBe true
        }

        @Test
        fun `должен определить направление изменения UP при росте курса`() = runTest {
            // Arrange
            val currentRates = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 490.0, sell = 495.0),
                rubToKzt = ExchangeRate(buy = 4.80, sell = 4.90)
            )

            val historicalRecord = CurrencyRateRecord(
                timestamp = Clock.System.now() - 2.hours,
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 480.0, sell = 485.0), // Было 485.0, стало 495.0
                    rubToKzt = ExchangeRateSnapshot(buy = 4.80, sell = 4.90)
                )
            )

            coEvery { historyRepository.getRecordBefore(any()) } returns Result.success(historicalRecord)

            // Act
            val result = useCase(currentRates)

            // Assert
            result.isSuccess shouldBe true
            val alerts = result.getOrThrow()
            val usdAlerts = alerts.filter { it.pair == CurrencyPair.USD_TO_KZT }

            // Все USD алерты должны показывать рост
            usdAlerts.forEach { alert ->
                alert.direction shouldBe ChangeDirection.UP
                (alert.changePercent > 0.0) shouldBe true
            }
        }

        @Test
        fun `должен определить направление изменения DOWN при падении курса`() = runTest {
            // Arrange
            val currentRates = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 475.0, sell = 480.0), // Стало 480.0
                rubToKzt = ExchangeRate(buy = 4.80, sell = 4.90)
            )

            val historicalRecord = CurrencyRateRecord(
                timestamp = Clock.System.now() - 2.hours,
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 485.0, sell = 490.0), // Было 490.0
                    rubToKzt = ExchangeRateSnapshot(buy = 4.80, sell = 4.90)
                )
            )

            coEvery { historyRepository.getRecordBefore(any()) } returns Result.success(historicalRecord)

            // Act
            val result = useCase(currentRates)

            // Assert
            result.isSuccess shouldBe true
            val alerts = result.getOrThrow()
            val usdAlerts = alerts.filter { it.pair == CurrencyPair.USD_TO_KZT }

            // Все USD алерты должны показывать падение
            usdAlerts.forEach { alert ->
                alert.direction shouldBe ChangeDirection.DOWN
                (alert.changePercent < 0.0) shouldBe true
            }
        }

        @Test
        fun `должен проверить обе валютные пары`() = runTest {
            // Arrange - обе валюты превышают пороги
            val currentRates = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 490.0, sell = 495.0), // USD +2.06%
                rubToKzt = ExchangeRate(buy = 5.00, sell = 5.10)   // RUB +4.08%
            )

            val historicalRecord = CurrencyRateRecord(
                timestamp = Clock.System.now() - 2.hours,
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 480.0, sell = 485.0),
                    rubToKzt = ExchangeRateSnapshot(buy = 4.85, sell = 4.90)
                )
            )

            coEvery { historyRepository.getRecordBefore(any()) } returns Result.success(historicalRecord)

            // Act
            val result = useCase(currentRates)

            // Assert
            result.isSuccess shouldBe true
            val alerts = result.getOrThrow()

            // Должны быть алерты для обеих валют
            val usdAlerts = alerts.filter { it.pair == CurrencyPair.USD_TO_KZT }
            val rubAlerts = alerts.filter { it.pair == CurrencyPair.RUB_TO_KZT }

            (usdAlerts.size > 0) shouldBe true
            (rubAlerts.size > 0) shouldBe true
        }
    }

    @Nested
    @DisplayName("Сценарии без превышения порогов")
    inner class NoThresholdExceeded {

        @Test
        fun `должен вернуть пустой список если пороги не превышены`() = runTest {
            // Arrange - изменение 0.1% (меньше WARNING 0.5%)
            val currentRates = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 485.0, sell = 485.5), // +0.1%
                rubToKzt = ExchangeRate(buy = 4.80, sell = 4.90)
            )

            val historicalRecord = CurrencyRateRecord(
                timestamp = Clock.System.now() - 2.hours,
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 480.0, sell = 485.0),
                    rubToKzt = ExchangeRateSnapshot(buy = 4.80, sell = 4.90)
                )
            )

            coEvery { historyRepository.getRecordBefore(any()) } returns Result.success(historicalRecord)

            // Act
            val result = useCase(currentRates)

            // Assert
            result.isSuccess shouldBe true
            result.getOrThrow().shouldBeEmpty()
        }

        @Test
        fun `должен вернуть пустой список если нет исторических данных`() = runTest {
            // Arrange - первый запуск, нет истории
            val currentRates = TestFixtures.sampleCurrencyRate

            coEvery { historyRepository.getRecordBefore(any()) } returns Result.success(null)

            // Act
            val result = useCase(currentRates)

            // Assert
            result.isSuccess shouldBe true
            result.getOrThrow().shouldBeEmpty()
        }

        @Test
        fun `должен проверить все 4 периода`() = runTest {
            // Arrange
            val currentRates = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 490.0, sell = 495.0),
                rubToKzt = ExchangeRate(buy = 4.80, sell = 4.90)
            )

            val historicalRecord = CurrencyRateRecord(
                timestamp = Clock.System.now() - 2.hours,
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 480.0, sell = 485.0),
                    rubToKzt = ExchangeRateSnapshot(buy = 4.80, sell = 4.90)
                )
            )

            coEvery { historyRepository.getRecordBefore(any()) } returns Result.success(historicalRecord)

            // Act
            val result = useCase(currentRates)

            // Assert
            result.isSuccess shouldBe true

            // Проверяем что вызывались все 4 периода
            coVerify(exactly = 4) { historyRepository.getRecordBefore(any()) }
        }
    }

    @Nested
    @DisplayName("Обработка ошибок")
    inner class ErrorHandling {

        @Test
        fun `должен продолжить работу если не удалось получить историю для одного периода`() = runTest {
            // Arrange
            val currentRates = TestFixtures.sampleCurrencyRate
            val error = Exception("Database error")

            coEvery { historyRepository.getRecordBefore(any()) } returns Result.failure(error)

            // Act
            val result = useCase(currentRates)

            // Assert - не должно падать
            result.isSuccess shouldBe true
            result.getOrThrow().shouldBeEmpty()
        }

        @Test
        fun `должен вернуть результат даже если часть периодов вернула ошибку`() = runTest {
            // Arrange
            val currentRates = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 490.0, sell = 500.0), // +3%
                rubToKzt = ExchangeRate(buy = 4.80, sell = 4.90)
            )

            val historicalRecord = CurrencyRateRecord(
                timestamp = Clock.System.now() - 2.hours,
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 480.0, sell = 485.0),
                    rubToKzt = ExchangeRateSnapshot(buy = 4.80, sell = 4.90)
                )
            )

            // Первый период вернет успех, остальные - ошибку
            coEvery { historyRepository.getRecordBefore(any()) } returns
                    Result.success(historicalRecord) andThen
                    Result.failure(Exception("Error")) andThen
                    Result.failure(Exception("Error")) andThen
                    Result.failure(Exception("Error"))

            // Act
            val result = useCase(currentRates)

            // Assert
            result.isSuccess shouldBe true
            // Должны быть алерты хотя бы из одного успешного периода
            val alerts = result.getOrThrow()
            (alerts.size > 0) shouldBe true
        }
    }

}
