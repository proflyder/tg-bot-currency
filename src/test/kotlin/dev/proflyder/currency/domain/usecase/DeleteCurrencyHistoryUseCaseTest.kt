package dev.proflyder.currency.domain.usecase

import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested

@DisplayName("DeleteCurrencyHistoryUseCase")
class DeleteCurrencyHistoryUseCaseTest {

    private lateinit var historyRepository: CurrencyHistoryRepository
    private lateinit var useCase: DeleteCurrencyHistoryUseCase

    @BeforeEach
    fun setup() {
        historyRepository = mockk()
        useCase = DeleteCurrencyHistoryUseCase(historyRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Nested
    @DisplayName("Успешные сценарии")
    inner class SuccessScenarios {

        @Test
        fun `должен успешно удалить все записи истории`() = runTest {
            // Arrange
            val deletedCount = 50
            coEvery { historyRepository.deleteAll() } returns Result.success(deletedCount)

            // Act
            val result = useCase()

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe deletedCount

            // Проверяем что репозиторий был вызван
            coVerify(exactly = 1) { historyRepository.deleteAll() }
        }

        @Test
        fun `должен вернуть 0 если база данных была пустая`() = runTest {
            // Arrange
            coEvery { historyRepository.deleteAll() } returns Result.success(0)

            // Act
            val result = useCase()

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe 0

            coVerify(exactly = 1) { historyRepository.deleteAll() }
        }

        @Test
        fun `должен успешно удалить одну запись`() = runTest {
            // Arrange
            coEvery { historyRepository.deleteAll() } returns Result.success(1)

            // Act
            val result = useCase()

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe 1
        }

        @Test
        fun `должен успешно удалить большое количество записей`() = runTest {
            // Arrange
            val deletedCount = 10000
            coEvery { historyRepository.deleteAll() } returns Result.success(deletedCount)

            // Act
            val result = useCase()

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe deletedCount
        }
    }

    @Nested
    @DisplayName("Сценарии с ошибками")
    inner class ErrorScenarios {

        @Test
        fun `должен вернуть ошибку если репозиторий выбросил исключение`() = runTest {
            // Arrange
            val error = Exception("Database connection error")
            coEvery { historyRepository.deleteAll() } returns Result.failure(error)

            // Act
            val result = useCase()

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull()?.message shouldBe "Database connection error"

            coVerify(exactly = 1) { historyRepository.deleteAll() }
        }

        @Test
        fun `должен вернуть ошибку при недоступности базы данных`() = runTest {
            // Arrange
            val error = Exception("H2 Database is not available")
            coEvery { historyRepository.deleteAll() } returns Result.failure(error)

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
            coEvery { historyRepository.deleteAll() } returns Result.failure(error)

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
            coEvery { historyRepository.deleteAll() } returns Result.failure(error)

            // Act
            val result = useCase()

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull()?.message shouldBe "SQL syntax error"
        }

        @Test
        fun `должен вернуть ошибку при нарушении constraint`() = runTest {
            // Arrange
            val error = Exception("Foreign key constraint violation")
            coEvery { historyRepository.deleteAll() } returns Result.failure(error)

            // Act
            val result = useCase()

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull()?.message shouldBe "Foreign key constraint violation"
        }
    }

    @Nested
    @DisplayName("Граничные случаи")
    inner class EdgeCases {

        @Test
        fun `должен вызвать репозиторий только один раз`() = runTest {
            // Arrange
            coEvery { historyRepository.deleteAll() } returns Result.success(0)

            // Act
            useCase()

            // Assert
            coVerify(exactly = 1) { historyRepository.deleteAll() }
            confirmVerified(historyRepository)
        }

        @Test
        fun `должен корректно обработать повторный вызов после удаления`() = runTest {
            // Arrange - первый вызов удалил 50 записей
            coEvery { historyRepository.deleteAll() } returns Result.success(50)

            // Act - первый вызов
            val result1 = useCase()

            // Assert первый вызов
            result1.isSuccess shouldBe true
            result1.getOrNull() shouldBe 50

            // Arrange - второй вызов должен вернуть 0 (все уже удалено)
            coEvery { historyRepository.deleteAll() } returns Result.success(0)

            // Act - второй вызов
            val result2 = useCase()

            // Assert второй вызов
            result2.isSuccess shouldBe true
            result2.getOrNull() shouldBe 0

            coVerify(exactly = 2) { historyRepository.deleteAll() }
        }
    }
}
