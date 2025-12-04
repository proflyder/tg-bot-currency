package dev.proflyder.currency.domain.usecase

import dev.proflyder.currency.TestFixtures
import dev.proflyder.currency.domain.model.CurrencyRateRecord
import dev.proflyder.currency.domain.model.CurrencyRateSnapshot
import dev.proflyder.currency.domain.model.ExchangeRateSnapshot
import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.*

@DisplayName("GetLatestCurrencyRateUseCase")
class GetLatestCurrencyRateUseCaseTest {

    private lateinit var historyRepository: CurrencyHistoryRepository
    private lateinit var useCase: GetLatestCurrencyRateUseCase

    @BeforeEach
    fun setup() {
        historyRepository = mockk()
        useCase = GetLatestCurrencyRateUseCase(historyRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Nested
    @DisplayName("Успешные сценарии")
    inner class SuccessScenarios {

        @Test
        fun `должен успешно получить последнюю запись курса`() = runTest {
            // Arrange
            val latestRecord = CurrencyRateRecord(
                timestamp = Instant.parse("2025-11-30T12:00:00Z"),
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 485.50, sell = 487.20),
                    rubToKzt = ExchangeRateSnapshot(buy = 4.85, sell = 4.92)
                )
            )

            coEvery { historyRepository.getLatestRecord() } returns Result.success(latestRecord)

            // Act
            val result = useCase()

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe latestRecord
            result.getOrNull()?.timestamp shouldBe Instant.parse("2025-11-30T12:00:00Z")
            result.getOrNull()?.rates?.usdToKzt?.buy shouldBe 485.50

            // Проверяем что репозиторий был вызван
            coVerify(exactly = 1) { historyRepository.getLatestRecord() }
        }

        @Test
        fun `должен вернуть null если база данных пустая`() = runTest {
            // Arrange
            coEvery { historyRepository.getLatestRecord() } returns Result.success(null)

            // Act
            val result = useCase()

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe null

            coVerify(exactly = 1) { historyRepository.getLatestRecord() }
        }

        @Test
        fun `должен вернуть самую свежую запись с корректными данными`() = runTest {
            // Arrange
            val latestRecord = CurrencyRateRecord(
                timestamp = TestFixtures.sampleTimestamp,
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(
                        buy = TestFixtures.sampleExchangeRateUsd.buy,
                        sell = TestFixtures.sampleExchangeRateUsd.sell
                    ),
                    rubToKzt = ExchangeRateSnapshot(
                        buy = TestFixtures.sampleExchangeRateRub.buy,
                        sell = TestFixtures.sampleExchangeRateRub.sell
                    )
                )
            )

            coEvery { historyRepository.getLatestRecord() } returns Result.success(latestRecord)

            // Act
            val result = useCase()

            // Assert
            result.isSuccess shouldBe true
            val record = result.getOrNull()!!
            record.timestamp shouldBe TestFixtures.sampleTimestamp
            record.rates.usdToKzt shouldBe ExchangeRateSnapshot(
                buy = TestFixtures.sampleExchangeRateUsd.buy,
                sell = TestFixtures.sampleExchangeRateUsd.sell
            )
            record.rates.rubToKzt shouldBe ExchangeRateSnapshot(
                buy = TestFixtures.sampleExchangeRateRub.buy,
                sell = TestFixtures.sampleExchangeRateRub.sell
            )
        }
    }

    @Nested
    @DisplayName("Сценарии с ошибками")
    inner class ErrorScenarios {

        @Test
        fun `должен вернуть ошибку если репозиторий выбросил исключение`() = runTest {
            // Arrange
            val error = Exception("Database connection error")
            coEvery { historyRepository.getLatestRecord() } returns Result.failure(error)

            // Act
            val result = useCase()

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull()?.message shouldBe "Database connection error"

            coVerify(exactly = 1) { historyRepository.getLatestRecord() }
        }

        @Test
        fun `должен вернуть ошибку при недоступности базы данных`() = runTest {
            // Arrange
            val error = Exception("H2 Database is not available")
            coEvery { historyRepository.getLatestRecord() } returns Result.failure(error)

            // Act
            val result = useCase()

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull() shouldBe error
        }

        @Test
        fun `должен корректно обработать timeout исключение`() = runTest {
            // Arrange
            val error = Exception("Query timeout")
            coEvery { historyRepository.getLatestRecord() } returns Result.failure(error)

            // Act
            val result = useCase()

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull()?.message shouldBe "Query timeout"
        }

        @Test
        fun `должен вернуть ошибку при SQL exception`() = runTest {
            // Arrange
            val error = Exception("SQL syntax error")
            coEvery { historyRepository.getLatestRecord() } returns Result.failure(error)

            // Act
            val result = useCase()

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull()?.message shouldBe "SQL syntax error"
        }
    }

    @Nested
    @DisplayName("Граничные случаи")
    inner class EdgeCases {

        @Test
        fun `должен вызвать репозиторий только один раз`() = runTest {
            // Arrange
            coEvery { historyRepository.getLatestRecord() } returns Result.success(null)

            // Act
            useCase()

            // Assert
            coVerify(exactly = 1) { historyRepository.getLatestRecord() }
            confirmVerified(historyRepository)
        }

        @Test
        fun `должен корректно обработать запись с минимальными значениями курсов`() = runTest {
            // Arrange
            val recordWithMinValues = CurrencyRateRecord(
                timestamp = Instant.parse("2025-11-30T00:00:00Z"),
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 0.01, sell = 0.01),
                    rubToKzt = ExchangeRateSnapshot(buy = 0.01, sell = 0.01)
                )
            )

            coEvery { historyRepository.getLatestRecord() } returns Result.success(recordWithMinValues)

            // Act
            val result = useCase()

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe recordWithMinValues
            result.getOrNull()?.rates?.usdToKzt?.buy shouldBe 0.01
        }

        @Test
        fun `должен корректно обработать запись с очень большими значениями курсов`() = runTest {
            // Arrange
            val recordWithLargeValues = CurrencyRateRecord(
                timestamp = Instant.parse("2025-11-30T23:59:59Z"),
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 999999.99, sell = 999999.99),
                    rubToKzt = ExchangeRateSnapshot(buy = 999999.99, sell = 999999.99)
                )
            )

            coEvery { historyRepository.getLatestRecord() } returns Result.success(recordWithLargeValues)

            // Act
            val result = useCase()

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe recordWithLargeValues
        }
    }
}
