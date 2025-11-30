package dev.proflyder.currency.domain.usecase

import dev.proflyder.currency.TestFixtures
import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import dev.proflyder.currency.domain.repository.CurrencyRepository
import dev.proflyder.currency.domain.repository.TelegramRepository
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested

@DisplayName("SendCurrencyRatesUseCase")
class SendCurrencyRatesUseCaseTest {

    private lateinit var currencyRepository: CurrencyRepository
    private lateinit var telegramRepository: TelegramRepository
    private lateinit var historyRepository: CurrencyHistoryRepository
    private lateinit var checkThresholdsUseCase: CheckCurrencyThresholdsUseCase
    private lateinit var formatMessageUseCase: FormatCurrencyMessageUseCase
    private lateinit var useCase: SendCurrencyRatesUseCase

    @BeforeEach
    fun setup() {
        // Создаем моки для всех зависимостей
        currencyRepository = mockk()
        telegramRepository = mockk()
        historyRepository = mockk()
        checkThresholdsUseCase = mockk()
        formatMessageUseCase = mockk()

        // Создаем UseCase с моками
        useCase = SendCurrencyRatesUseCase(
            currencyRepository = currencyRepository,
            telegramRepository = telegramRepository,
            currencyHistoryRepository = historyRepository,
            checkThresholdsUseCase = checkThresholdsUseCase,
            formatMessageUseCase = formatMessageUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Nested
    @DisplayName("Успешные сценарии")
    inner class SuccessScenarios {

        @Test
        fun `должен успешно получить курсы, отправить в Telegram и сохранить в историю если есть алерты`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val rates = TestFixtures.sampleCurrencyRate

            coEvery { currencyRepository.getCurrentRates() } returns Result.success(rates)
            coEvery { historyRepository.saveRecord(any(), any()) } returns Result.success(Unit)
            coEvery { checkThresholdsUseCase(any()) } returns Result.success(listOf(mockk())) // Есть алерты
            every { formatMessageUseCase(any(), any()) } returns "test message"
            coEvery { telegramRepository.sendMessage(any(), any()) } returns Result.success(Unit)
            coEvery { historyRepository.cleanOldRecords(any()) } returns Result.success(0)

            // Act
            val result = useCase(chatId)

            // Assert
            result.isSuccess shouldBe true

            // Проверяем что все методы были вызваны в правильном порядке
            coVerifyOrder {
                currencyRepository.getCurrentRates()
                historyRepository.saveRecord(rates, any())
                checkThresholdsUseCase(rates)
                formatMessageUseCase(rates, any())
                telegramRepository.sendMessage(chatId, any())
                historyRepository.cleanOldRecords(30)
            }
        }

        @Test
        fun `должен пропустить отправку в Telegram если нет алертов`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val rates = TestFixtures.sampleCurrencyRate

            coEvery { currencyRepository.getCurrentRates() } returns Result.success(rates)
            coEvery { historyRepository.saveRecord(any(), any()) } returns Result.success(Unit)
            coEvery { checkThresholdsUseCase(any()) } returns Result.success(emptyList()) // Нет алертов
            coEvery { historyRepository.cleanOldRecords(any()) } returns Result.success(0)

            // Act
            val result = useCase(chatId)

            // Assert
            result.isSuccess shouldBe true

            // Telegram НЕ должен вызываться
            coVerify(exactly = 0) { telegramRepository.sendMessage(any(), any()) }
            coVerify(exactly = 0) { formatMessageUseCase(any(), any()) }

            // Но история должна сохраниться
            coVerify(exactly = 1) { historyRepository.saveRecord(rates, any()) }
        }

        @Test
        fun `должен очистить старые записи после сохранения`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val rates = TestFixtures.sampleCurrencyRate
            val removedCount = 5

            coEvery { currencyRepository.getCurrentRates() } returns Result.success(rates)
            coEvery { historyRepository.saveRecord(any(), any()) } returns Result.success(Unit)
            coEvery { checkThresholdsUseCase(any()) } returns Result.success(emptyList())
            coEvery { historyRepository.cleanOldRecords(30) } returns Result.success(removedCount)

            // Act
            val result = useCase(chatId)

            // Assert
            result.isSuccess shouldBe true
            coVerify { historyRepository.cleanOldRecords(30) }
        }
    }

    @Nested
    @DisplayName("Сценарии с ошибками")
    inner class ErrorScenarios {

        @Test
        fun `должен вернуть ошибку если не удалось получить курсы`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val error = Exception("Network error")

            coEvery { currencyRepository.getCurrentRates() } returns Result.failure(error)

            // Act
            val result = useCase(chatId)

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull()?.message shouldBe "Network error"

            // Ничего не должно вызываться
            coVerify(exactly = 0) { telegramRepository.sendMessage(any(), any()) }
            coVerify(exactly = 0) { historyRepository.saveRecord(any(), any()) }
        }

        @Test
        fun `должен вернуть ошибку если не удалось отправить в Telegram`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val rates = TestFixtures.sampleCurrencyRate
            val error = Exception("Telegram API error")

            coEvery { currencyRepository.getCurrentRates() } returns Result.success(rates)
            coEvery { historyRepository.saveRecord(any(), any()) } returns Result.success(Unit)
            coEvery { checkThresholdsUseCase(any()) } returns Result.success(listOf(mockk())) // Есть алерты
            every { formatMessageUseCase(any(), any()) } returns "test message"
            coEvery { telegramRepository.sendMessage(any(), any()) } returns Result.failure(error)

            // Act
            val result = useCase(chatId)

            // Assert
            result.isFailure shouldBe true

            // История должна сохраниться несмотря на ошибку отправки
            coVerify(exactly = 1) { historyRepository.saveRecord(any(), any()) }
        }

        @Test
        fun `должен успешно завершиться даже если не удалось сохранить историю`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val rates = TestFixtures.sampleCurrencyRate
            val historyError = Exception("Failed to save history")

            coEvery { currencyRepository.getCurrentRates() } returns Result.success(rates)
            coEvery { historyRepository.saveRecord(any(), any()) } returns Result.failure(historyError)
            coEvery { checkThresholdsUseCase(any()) } returns Result.success(emptyList())
            coEvery { historyRepository.cleanOldRecords(any()) } returns Result.success(0)

            // Act
            val result = useCase(chatId)

            // Assert - UseCase должен завершиться успешно несмотря на ошибку в истории
            result.isSuccess shouldBe true

            coVerify { historyRepository.saveRecord(any(), any()) }
            // Очистка все равно должна вызваться
            coVerify { historyRepository.cleanOldRecords(30) }
        }

        @Test
        fun `должен успешно завершиться даже если не удалось очистить старые записи`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val rates = TestFixtures.sampleCurrencyRate
            val cleanError = Exception("Failed to clean old records")

            coEvery { currencyRepository.getCurrentRates() } returns Result.success(rates)
            coEvery { historyRepository.saveRecord(any(), any()) } returns Result.success(Unit)
            coEvery { checkThresholdsUseCase(any()) } returns Result.success(emptyList())
            coEvery { historyRepository.cleanOldRecords(any()) } returns Result.failure(cleanError)

            // Act
            val result = useCase(chatId)

            // Assert - UseCase должен завершиться успешно несмотря на ошибку очистки
            result.isSuccess shouldBe true
        }

        @Test
        fun `должен продолжить работу даже если проверка трешхолдов упала`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val rates = TestFixtures.sampleCurrencyRate
            val thresholdError = Exception("Failed to check thresholds")

            coEvery { currencyRepository.getCurrentRates() } returns Result.success(rates)
            coEvery { historyRepository.saveRecord(any(), any()) } returns Result.success(Unit)
            coEvery { checkThresholdsUseCase(any()) } returns Result.failure(thresholdError)
            coEvery { historyRepository.cleanOldRecords(any()) } returns Result.success(0)

            // Act
            val result = useCase(chatId)

            // Assert - UseCase должен завершиться успешно
            result.isSuccess shouldBe true

            // Telegram не должен вызываться
            coVerify(exactly = 0) { telegramRepository.sendMessage(any(), any()) }
        }
    }

    @Nested
    @DisplayName("Граничные случаи")
    inner class EdgeCases {

        @Test
        fun `должен корректно обработать пустой chat ID`() = runTest {
            // Arrange
            val chatId = ""
            val rates = TestFixtures.sampleCurrencyRate

            coEvery { currencyRepository.getCurrentRates() } returns Result.success(rates)
            coEvery { historyRepository.saveRecord(any(), any()) } returns Result.success(Unit)
            coEvery { checkThresholdsUseCase(any()) } returns Result.success(listOf(mockk()))
            every { formatMessageUseCase(any(), any()) } returns "test message"
            coEvery { telegramRepository.sendMessage(any(), any()) } returns Result.success(Unit)
            coEvery { historyRepository.cleanOldRecords(any()) } returns Result.success(0)

            // Act
            val result = useCase(chatId)

            // Assert
            result.isSuccess shouldBe true
            coVerify { telegramRepository.sendMessage("", any()) }
        }

        @Test
        fun `должен сохранить историю с текущим timestamp`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val rates = TestFixtures.sampleCurrencyRate

            coEvery { currencyRepository.getCurrentRates() } returns Result.success(rates)
            coEvery { historyRepository.saveRecord(any(), any()) } returns Result.success(Unit)
            coEvery { checkThresholdsUseCase(any()) } returns Result.success(emptyList())
            coEvery { historyRepository.cleanOldRecords(any()) } returns Result.success(0)

            // Act
            useCase(chatId)

            // Assert - проверяем что timestamp был передан
            coVerify { historyRepository.saveRecord(rates, any()) }
        }
    }
}
