package dev.proflyder.currency.data.repository

import dev.proflyder.currency.TestFixtures
import dev.proflyder.currency.data.dto.telegram.Message
import dev.proflyder.currency.data.dto.telegram.SendMessageRequest
import dev.proflyder.currency.data.dto.telegram.TelegramResponse
import dev.proflyder.currency.data.remote.telegram.TelegramApi
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*

@DisplayName("TelegramRepositoryImpl")
class TelegramRepositoryImplTest {

    private lateinit var telegramApi: TelegramApi
    private lateinit var repository: TelegramRepositoryImpl

    @BeforeEach
    fun setup() {
        telegramApi = mockk()
        repository = TelegramRepositoryImpl(telegramApi)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Nested
    @DisplayName("Отправка сообщений")
    inner class SendMessage {

        @Test
        fun `должен успешно отправить сообщение через Telegram API`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val message = "Test message"
            val mockResponse = mockk<TelegramResponse<Message>>()

            coEvery { telegramApi.sendMessage(any()) } returns Result.success(mockResponse)

            // Act
            val result = repository.sendMessage(chatId, message)

            // Assert
            result.isSuccess shouldBe true

            coVerify(exactly = 1) {
                telegramApi.sendMessage(
                    match { request ->
                        request.chatId == chatId &&
                                request.text == message &&
                                request.parseMode == "Markdown"
                    }
                )
            }
        }

        @Test
        fun `должен передать правильный chatId в API`() = runTest {
            // Arrange
            val chatId = "987654321"
            val message = "Test"
            val mockResponse = mockk<TelegramResponse<Message>>()
            val requestSlot = slot<SendMessageRequest>()

            coEvery { telegramApi.sendMessage(capture(requestSlot)) } returns Result.success(mockResponse)

            // Act
            repository.sendMessage(chatId, message)

            // Assert
            requestSlot.captured.chatId shouldBe chatId
        }

        @Test
        fun `должен передать правильный текст сообщения в API`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val message = "Hello from tests!"
            val mockResponse = mockk<TelegramResponse<Message>>()
            val requestSlot = slot<SendMessageRequest>()

            coEvery { telegramApi.sendMessage(capture(requestSlot)) } returns Result.success(mockResponse)

            // Act
            repository.sendMessage(chatId, message)

            // Assert
            requestSlot.captured.text shouldBe message
        }

        @Test
        fun `должен использовать Markdown для форматирования`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val message = "*Bold* _Italic_"
            val mockResponse = mockk<TelegramResponse<Message>>()
            val requestSlot = slot<SendMessageRequest>()

            coEvery { telegramApi.sendMessage(capture(requestSlot)) } returns Result.success(mockResponse)

            // Act
            repository.sendMessage(chatId, message)

            // Assert
            requestSlot.captured.parseMode shouldBe "Markdown"
        }

        @Test
        fun `должен вернуть ошибку если API вернул ошибку`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val message = "Test"
            val error = Exception("Telegram API error: unauthorized")

            coEvery { telegramApi.sendMessage(any()) } returns Result.failure(error)

            // Act
            val result = repository.sendMessage(chatId, message)

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull()?.message shouldBe "Telegram API error: unauthorized"
        }

        @Test
        fun `должен корректно обработать пустое сообщение`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val emptyMessage = ""
            val mockResponse = mockk<TelegramResponse<Message>>()

            coEvery { telegramApi.sendMessage(any()) } returns Result.success(mockResponse)

            // Act
            val result = repository.sendMessage(chatId, emptyMessage)

            // Assert
            result.isSuccess shouldBe true
            coVerify { telegramApi.sendMessage(match { it.text == "" }) }
        }

        @Test
        fun `должен корректно обработать длинное сообщение`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val longMessage = "A".repeat(4096) // Telegram лимит ~4096 символов
            val mockResponse = mockk<TelegramResponse<Message>>()

            coEvery { telegramApi.sendMessage(any()) } returns Result.success(mockResponse)

            // Act
            val result = repository.sendMessage(chatId, longMessage)

            // Assert
            result.isSuccess shouldBe true
            coVerify { telegramApi.sendMessage(match { it.text == longMessage }) }
        }

        @Test
        fun `должен корректно обработать специальные символы в сообщении`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val messageWithSpecialChars = "Test \n\t\r \"quotes\" 'apostrophes' & <html>"
            val mockResponse = mockk<TelegramResponse<Message>>()

            coEvery { telegramApi.sendMessage(any()) } returns Result.success(mockResponse)

            // Act
            val result = repository.sendMessage(chatId, messageWithSpecialChars)

            // Assert
            result.isSuccess shouldBe true
            coVerify { telegramApi.sendMessage(match { it.text == messageWithSpecialChars }) }
        }

        @Test
        fun `должен корректно обработать unicode символы`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val unicodeMessage = "💱 Курсы: 🇺🇸 USD → 🇰🇿 KZT"
            val mockResponse = mockk<TelegramResponse<Message>>()

            coEvery { telegramApi.sendMessage(any()) } returns Result.success(mockResponse)

            // Act
            val result = repository.sendMessage(chatId, unicodeMessage)

            // Assert
            result.isSuccess shouldBe true
            coVerify { telegramApi.sendMessage(match { it.text == unicodeMessage }) }
        }
    }

    @Nested
    @DisplayName("Обработка ошибок")
    inner class ErrorHandling {

        @Test
        fun `должен пробросить network ошибку`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val message = "Test"
            val networkError = Exception("Network timeout")

            coEvery { telegramApi.sendMessage(any()) } returns Result.failure(networkError)

            // Act
            val result = repository.sendMessage(chatId, message)

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull() shouldBe networkError
        }

        @Test
        fun `должен пробросить authorization ошибку`() = runTest {
            // Arrange
            val chatId = TestFixtures.TEST_CHAT_ID
            val message = "Test"
            val authError = Exception("401 Unauthorized")

            coEvery { telegramApi.sendMessage(any()) } returns Result.failure(authError)

            // Act
            val result = repository.sendMessage(chatId, message)

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull()?.message shouldBe "401 Unauthorized"
        }
    }
}
