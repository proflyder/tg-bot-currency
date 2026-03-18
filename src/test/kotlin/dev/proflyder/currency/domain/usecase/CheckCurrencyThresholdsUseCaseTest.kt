package dev.proflyder.currency.domain.usecase

import dev.proflyder.currency.TestFixtures
import dev.proflyder.currency.domain.model.*
import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import dev.proflyder.currency.domain.repository.SentAlertRepository
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
    private lateinit var sentAlertRepository: SentAlertRepository
    private lateinit var useCase: CheckCurrencyThresholdsUseCase

    @BeforeEach
    fun setup() {
        historyRepository = mockk()
        sentAlertRepository = mockk()
        // По умолчанию: нет предыдущих алертов, запись/очистка успешны
        coEvery { sentAlertRepository.getLastSentAlert(any()) } returns Result.success(null)
        coEvery { sentAlertRepository.recordSentAlert(any()) } returns Result.success(Unit)
        coEvery { sentAlertRepository.clearSentAlert(any()) } returns Result.success(Unit)
        useCase = CheckCurrencyThresholdsUseCase(historyRepository, sentAlertRepository)
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
                    usdToKzt = ExchangeRateSnapshot(buy = 485.0, sell = 485.0),
                    rubToKzt = ExchangeRateSnapshot(buy = 4.80, sell = 4.90)
                )
            )

            coEvery { historyRepository.getRecordBefore(any()) } returns Result.success(historicalRecord)

            // Act
            val result = useCase(currentRates)

            // Assert
            result.isSuccess shouldBe true
            val alerts = result.getOrThrow()

            // Должны быть алерты для USD SELL
            val usdAlerts = alerts.filter { it.pair == CurrencyPair.USD_TO_KZT && it.rateType == RateType.SELL }
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

        @Test
        fun `должен проверять и BUY и SELL курсы`() = runTest {
            // Arrange - buy вырос, sell остался
            val currentRates = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 495.0, sell = 495.0), // buy +2.06%, sell +2.06%
                rubToKzt = ExchangeRate(buy = 4.80, sell = 4.90)
            )

            val historicalRecord = CurrencyRateRecord(
                timestamp = Clock.System.now() - 2.hours,
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 485.0, sell = 485.0),
                    rubToKzt = ExchangeRateSnapshot(buy = 4.80, sell = 4.90)
                )
            )

            coEvery { historyRepository.getRecordBefore(any()) } returns Result.success(historicalRecord)

            // Act
            val result = useCase(currentRates)

            // Assert
            result.isSuccess shouldBe true
            val alerts = result.getOrThrow()

            val buyAlerts = alerts.filter { it.pair == CurrencyPair.USD_TO_KZT && it.rateType == RateType.BUY }
            val sellAlerts = alerts.filter { it.pair == CurrencyPair.USD_TO_KZT && it.rateType == RateType.SELL }

            (buyAlerts.size > 0) shouldBe true
            (sellAlerts.size > 0) shouldBe true
        }

        @Test
        fun `алерт должен содержать rateType`() = runTest {
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

            val result = useCase(currentRates)
            val alerts = result.getOrThrow()

            // Каждый алерт должен иметь rateType
            alerts.forEach { alert ->
                (alert.rateType == RateType.BUY || alert.rateType == RateType.SELL) shouldBe true
            }
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
                    usdToKzt = ExchangeRateSnapshot(buy = 485.0, sell = 485.0),
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

        @Test
        fun `должен очистить sent alert при нормализации курса`() = runTest {
            // Arrange - курс нормализовался (нет превышения)
            val currentRates = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 485.0, sell = 485.1),
                rubToKzt = ExchangeRate(buy = 4.80, sell = 4.90)
            )

            val historicalRecord = CurrencyRateRecord(
                timestamp = Clock.System.now() - 2.hours,
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 485.0, sell = 485.0),
                    rubToKzt = ExchangeRateSnapshot(buy = 4.80, sell = 4.90)
                )
            )

            coEvery { historyRepository.getRecordBefore(any()) } returns Result.success(historicalRecord)

            // Act
            useCase(currentRates)

            // Assert - clearSentAlert должен быть вызван (курс вернулся в норму)
            coVerify(atLeast = 1) { sentAlertRepository.clearSentAlert(any()) }
        }
    }

    @Nested
    @DisplayName("Дедупликация алертов")
    inner class AlertDeduplication {

        @Test
        fun `должен подавить дублирующий алерт если курс не изменился`() = runTest {
            // Arrange: +0.62% — превышает только HOUR WARNING (0.5%), не CRITICAL (1.0%)
            // Не превышает DAY/WEEK/MONTH пороги
            val currentRates = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 488.0, sell = 488.0),
                rubToKzt = ExchangeRate(buy = 4.90, sell = 4.90)
            )

            val historicalRecord = CurrencyRateRecord(
                timestamp = Clock.System.now() - 2.hours,
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 485.0, sell = 485.0),
                    rubToKzt = ExchangeRateSnapshot(buy = 4.90, sell = 4.90)
                )
            )

            coEvery { historyRepository.getRecordBefore(any()) } returns Result.success(historicalRecord)

            // Алерт уже отправлялся с тем же курсом — для всех ключей
            coEvery { sentAlertRepository.getLastSentAlert(any()) } answers {
                Result.success(
                    SentAlert(
                        key = firstArg(),
                        level = AlertLevel.WARNING,
                        direction = ChangeDirection.UP,
                        rateAtAlert = 488.0, // Тот же курс что и текущий
                        changePercent = 0.62,
                        sentAt = Clock.System.now() - 1.hours
                    )
                )
            }

            // Act
            val result = useCase(currentRates)

            // Assert - алерты должны быть подавлены (курс не изменился с последнего алерта)
            result.isSuccess shouldBe true
            result.getOrThrow().shouldBeEmpty()
        }

        @Test
        fun `должен отправить алерт при эскалации уровня`() = runTest {
            // Arrange
            val currentRates = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 490.0, sell = 500.0), // +3.09% = CRITICAL
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

            // Предыдущий алерт был WARNING
            coEvery { sentAlertRepository.getLastSentAlert(any()) } returns Result.success(
                SentAlert(
                    key = AlertKey(CurrencyPair.USD_TO_KZT, AlertPeriod.HOUR, RateType.SELL),
                    level = AlertLevel.WARNING,
                    direction = ChangeDirection.UP,
                    rateAtAlert = 500.0,
                    changePercent = 2.06,
                    sentAt = Clock.System.now() - 1.hours
                )
            )

            // Act
            val result = useCase(currentRates)

            // Assert - CRITICAL должен пройти несмотря на дедупликацию
            result.isSuccess shouldBe true
            val alerts = result.getOrThrow()
            val criticalAlerts = alerts.filter {
                it.pair == CurrencyPair.USD_TO_KZT && it.level == AlertLevel.CRITICAL
            }
            (criticalAlerts.size > 0) shouldBe true
        }

        @Test
        fun `должен отправить алерт при смене направления`() = runTest {
            // Arrange - курс упал (а раньше рос)
            val currentRates = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 475.0, sell = 480.0), // sell: 480 < 485 = DOWN
                rubToKzt = ExchangeRate(buy = 4.80, sell = 4.90)
            )

            val historicalRecord = CurrencyRateRecord(
                timestamp = Clock.System.now() - 2.hours,
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 490.0, sell = 490.0), // sell: 490 -> 480 = -2.04%
                    rubToKzt = ExchangeRateSnapshot(buy = 4.80, sell = 4.90)
                )
            )

            coEvery { historyRepository.getRecordBefore(any()) } returns Result.success(historicalRecord)

            // Предыдущий алерт был UP
            coEvery { sentAlertRepository.getLastSentAlert(any()) } returns Result.success(
                SentAlert(
                    key = AlertKey(CurrencyPair.USD_TO_KZT, AlertPeriod.HOUR, RateType.SELL),
                    level = AlertLevel.WARNING,
                    direction = ChangeDirection.UP, // Раньше рос
                    rateAtAlert = 480.0,
                    changePercent = 2.06,
                    sentAt = Clock.System.now() - 1.hours
                )
            )

            // Act
            val result = useCase(currentRates)

            // Assert - алерт о смене направления должен пройти
            result.isSuccess shouldBe true
            val alerts = result.getOrThrow()
            val downAlerts = alerts.filter {
                it.pair == CurrencyPair.USD_TO_KZT && it.direction == ChangeDirection.DOWN
            }
            (downAlerts.size > 0) shouldBe true
        }

        @Test
        fun `должен отправить алерт если курс значительно изменился с последнего алерта`() = runTest {
            // Arrange
            val currentRates = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 492.0, sell = 498.0), // sell: 498 vs 495 = +0.6% > re-alert 0.25%
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

            // Предыдущий алерт с другим курсом
            coEvery { sentAlertRepository.getLastSentAlert(any()) } returns Result.success(
                SentAlert(
                    key = AlertKey(CurrencyPair.USD_TO_KZT, AlertPeriod.HOUR, RateType.SELL),
                    level = AlertLevel.WARNING,
                    direction = ChangeDirection.UP,
                    rateAtAlert = 495.0, // Курс был 495, сейчас 498 — разница 0.6%
                    changePercent = 2.06,
                    sentAt = Clock.System.now() - 1.hours
                )
            )

            // Act
            val result = useCase(currentRates)

            // Assert
            result.isSuccess shouldBe true
            val alerts = result.getOrThrow()
            val usdSellAlerts = alerts.filter {
                it.pair == CurrencyPair.USD_TO_KZT && it.rateType == RateType.SELL
            }
            (usdSellAlerts.size > 0) shouldBe true
        }
    }

    @Nested
    @DisplayName("skipDedup")
    inner class SkipDedup {

        @Test
        fun `skipDedup=true должен пропустить дедупликацию и вернуть все алерты`() = runTest {
            val currentRates = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 488.0, sell = 488.0),
                rubToKzt = ExchangeRate(buy = 4.90, sell = 4.90)
            )

            val historicalRecord = CurrencyRateRecord(
                timestamp = Clock.System.now() - 2.hours,
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 485.0, sell = 485.0),
                    rubToKzt = ExchangeRateSnapshot(buy = 4.90, sell = 4.90)
                )
            )

            coEvery { historyRepository.getRecordBefore(any()) } returns Result.success(historicalRecord)

            // Деdup подавил бы — тот же курс
            coEvery { sentAlertRepository.getLastSentAlert(any()) } answers {
                Result.success(
                    SentAlert(
                        key = firstArg(),
                        level = AlertLevel.WARNING,
                        direction = ChangeDirection.UP,
                        rateAtAlert = 488.0,
                        changePercent = 0.62,
                        sentAt = Clock.System.now() - 1.hours
                    )
                )
            }

            // Без skipDedup — подавляет
            val suppressed = useCase(currentRates, skipDedup = false)
            suppressed.getOrThrow().shouldBeEmpty()

            // С skipDedup — показывает
            val shown = useCase(currentRates, skipDedup = true)
            (shown.getOrThrow().size > 0) shouldBe true
        }

        @Test
        fun `skipDedup=true не должен вызывать sentAlertRepository`() = runTest {
            val currentRates = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 488.0, sell = 488.0),
                rubToKzt = ExchangeRate(buy = 4.90, sell = 4.90)
            )

            val historicalRecord = CurrencyRateRecord(
                timestamp = Clock.System.now() - 2.hours,
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 485.0, sell = 485.0),
                    rubToKzt = ExchangeRateSnapshot(buy = 4.90, sell = 4.90)
                )
            )

            coEvery { historyRepository.getRecordBefore(any()) } returns Result.success(historicalRecord)

            useCase(currentRates, skipDedup = true)

            coVerify(exactly = 0) { sentAlertRepository.getLastSentAlert(any()) }
            coVerify(exactly = 0) { sentAlertRepository.clearSentAlert(any()) }
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
