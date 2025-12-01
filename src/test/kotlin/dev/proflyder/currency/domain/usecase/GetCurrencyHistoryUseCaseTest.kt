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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested

@DisplayName("GetCurrencyHistoryUseCase")
class GetCurrencyHistoryUseCaseTest {

    private lateinit var historyRepository: CurrencyHistoryRepository
    private lateinit var useCase: GetCurrencyHistoryUseCase

    @BeforeEach
    fun setup() {
        historyRepository = mockk()
        useCase = GetCurrencyHistoryUseCase(historyRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Nested
    @DisplayName("Успешные сценарии")
    inner class SuccessScenarios {

        @Test
        fun `должен успешно получить полную историю курсов`() = runTest {
            // Arrange
            val records = listOf(
                CurrencyRateRecord(
                    timestamp = Instant.parse("2025-11-30T12:00:00Z"),
                    rates = CurrencyRateSnapshot(
                        usdToKzt = ExchangeRateSnapshot(buy = 485.50, sell = 487.20),
                        rubToKzt = ExchangeRateSnapshot(buy = 4.85, sell = 4.92)
                    )
                ),
                CurrencyRateRecord(
                    timestamp = Instant.parse("2025-11-30T11:00:00Z"),
                    rates = CurrencyRateSnapshot(
                        usdToKzt = ExchangeRateSnapshot(buy = 485.00, sell = 486.70),
                        rubToKzt = ExchangeRateSnapshot(buy = 4.83, sell = 4.90)
                    )
                )
            )

            coEvery { historyRepository.getAllRecords() } returns Result.success(records)

            // Act
            val result = useCase()

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe records
            result.getOrNull()?.size shouldBe 2

            // Проверяем что репозиторий был вызван
            coVerify(exactly = 1) { historyRepository.getAllRecords() }
        }

        @Test
        fun `должен вернуть пустой список если история пустая`() = runTest {
            // Arrange
            coEvery { historyRepository.getAllRecords() } returns Result.success(emptyList())

            // Act
            val result = useCase()

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe emptyList()
            result.getOrNull()?.size shouldBe 0
        }

        @Test
        fun `должен вернуть записи отсортированные по времени (от новых к старым)`() = runTest {
            // Arrange
            val newerRecord = CurrencyRateRecord(
                timestamp = Instant.parse("2025-11-30T12:00:00Z"),
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 485.50, sell = 487.20),
                    rubToKzt = ExchangeRateSnapshot(buy = 4.85, sell = 4.92)
                )
            )
            val olderRecord = CurrencyRateRecord(
                timestamp = Instant.parse("2025-11-30T10:00:00Z"),
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 484.00, sell = 485.70),
                    rubToKzt = ExchangeRateSnapshot(buy = 4.80, sell = 4.87)
                )
            )

            val records = listOf(newerRecord, olderRecord)
            coEvery { historyRepository.getAllRecords() } returns Result.success(records)

            // Act
            val result = useCase()

            // Assert
            result.isSuccess shouldBe true
            val resultRecords = result.getOrNull()!!
            resultRecords.size shouldBe 2
            resultRecords[0].timestamp shouldBe newerRecord.timestamp
            resultRecords[1].timestamp shouldBe olderRecord.timestamp
        }

        @Test
        fun `должен вернуть большое количество записей`() = runTest {
            // Arrange
            val largeRecordList = List(1000) { index ->
                CurrencyRateRecord(
                    timestamp = Instant.fromEpochMilliseconds(1700000000000L - (index * 3600000L)),
                    rates = CurrencyRateSnapshot(
                        usdToKzt = ExchangeRateSnapshot(buy = 485.0 + index * 0.1, sell = 487.0 + index * 0.1),
                        rubToKzt = ExchangeRateSnapshot(buy = 4.85 + index * 0.01, sell = 4.92 + index * 0.01)
                    )
                )
            }

            coEvery { historyRepository.getAllRecords() } returns Result.success(largeRecordList)

            // Act
            val result = useCase()

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull()?.size shouldBe 1000
        }
    }

    @Nested
    @DisplayName("Сценарии с ошибками")
    inner class ErrorScenarios {

        @Test
        fun `должен вернуть ошибку если репозиторий выбросил исключение`() = runTest {
            // Arrange
            val error = Exception("Database connection error")
            coEvery { historyRepository.getAllRecords() } returns Result.failure(error)

            // Act
            val result = useCase()

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull()?.message shouldBe "Database connection error"

            coVerify(exactly = 1) { historyRepository.getAllRecords() }
        }

        @Test
        fun `должен вернуть ошибку при недоступности базы данных`() = runTest {
            // Arrange
            val error = Exception("H2 Database is not available")
            coEvery { historyRepository.getAllRecords() } returns Result.failure(error)

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
            coEvery { historyRepository.getAllRecords() } returns Result.failure(error)

            // Act
            val result = useCase()

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull()?.message shouldBe "Query timeout"
        }
    }

    @Nested
    @DisplayName("Граничные случаи")
    inner class EdgeCases {

        @Test
        fun `должен корректно обработать одну запись в истории`() = runTest {
            // Arrange
            val singleRecord = listOf(
                CurrencyRateRecord(
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
            )

            coEvery { historyRepository.getAllRecords() } returns Result.success(singleRecord)

            // Act
            val result = useCase()

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull()?.size shouldBe 1
            result.getOrNull()?.first() shouldBe singleRecord.first()
        }

        @Test
        fun `должен вызвать репозиторий только один раз`() = runTest {
            // Arrange
            coEvery { historyRepository.getAllRecords() } returns Result.success(emptyList())

            // Act
            useCase()

            // Assert
            coVerify(exactly = 1) { historyRepository.getAllRecords() }
            confirmVerified(historyRepository)
        }
    }
}
