package dev.proflyder.currency.scheduler

import dev.proflyder.currency.TestFixtures
import dev.proflyder.currency.di.AppConfig
import dev.proflyder.currency.domain.usecase.SendCurrencyRatesUseCase
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobExecutionContext

@DisplayName("CurrencyRatesJob")
class CurrencyRatesJobTest {

    private lateinit var job: CurrencyRatesJob
    private lateinit var sendCurrencyRatesUseCase: SendCurrencyRatesUseCase
    private lateinit var config: AppConfig
    private lateinit var jobExecutionContext: JobExecutionContext
    private lateinit var jobDetail: JobDetail
    private lateinit var jobDataMap: JobDataMap

    @BeforeEach
    fun setup() {
        job = CurrencyRatesJob()
        sendCurrencyRatesUseCase = mockk()
        config = AppConfig(
            botToken = "test-token",
            chatId = "test-chat-id",
            schedulerCron = "0 0 * * * ?",
            databasePath = "mem:test-db",
            unkeyRootKey = "test-unkey-root-key",
            internalApiKey = "test-internal-key"
        )

        // Мокаем Quartz контекст
        jobDataMap = JobDataMap()
        jobDataMap["sendCurrencyRatesUseCase"] = sendCurrencyRatesUseCase
        jobDataMap["config"] = config

        jobDetail = mockk {
            every { jobDataMap } returns this@CurrencyRatesJobTest.jobDataMap
        }

        jobExecutionContext = mockk {
            every { jobDetail } returns this@CurrencyRatesJobTest.jobDetail
            every { fireTime } returns java.util.Date()
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Nested
    @DisplayName("Успешное выполнение")
    inner class SuccessfulExecution {

        @Test
        fun `должен вызвать SendCurrencyRatesUseCase с правильным chatId`() = runTest {
            // Arrange
            coEvery { sendCurrencyRatesUseCase(any(), any()) } returns Result.success(Unit)

            // Act
            job.execute(jobExecutionContext)

            // Assert
            coVerify(exactly = 1) { sendCurrencyRatesUseCase("test-chat-id", false) }
        }

        @Test
        fun `должен успешно выполниться когда use case возвращает success`() = runTest {
            // Arrange
            coEvery { sendCurrencyRatesUseCase(any(), any()) } returns Result.success(Unit)

            // Act & Assert - не должно быть исключений
            job.execute(jobExecutionContext)

            // Verify
            coVerify(exactly = 1) { sendCurrencyRatesUseCase(any(), any()) }
        }

        @Test
        fun `должен получить зависимости из JobDataMap`() = runTest {
            // Arrange
            coEvery { sendCurrencyRatesUseCase(any(), any()) } returns Result.success(Unit)

            // Act
            job.execute(jobExecutionContext)

            // Assert
            verify { jobExecutionContext.jobDetail }
            verify { jobDetail.jobDataMap }
        }
    }

    @Nested
    @DisplayName("Обработка ошибок")
    inner class ErrorHandling {

        @Test
        fun `должен продолжить выполнение если use case возвращает failure`() = runTest {
            // Arrange
            val error = Exception("Test error")
            coEvery { sendCurrencyRatesUseCase(any(), any()) } returns Result.failure(error)

            // Act & Assert - не должно падать
            job.execute(jobExecutionContext)

            // Verify
            coVerify(exactly = 1) { sendCurrencyRatesUseCase(any(), any()) }
        }

        @Test
        fun `должен обработать исключение при выполнении use case`() = runTest {
            // Arrange
            coEvery { sendCurrencyRatesUseCase(any(), any()) } throws RuntimeException("Unexpected error")

            // Act & Assert - не должно падать
            job.execute(jobExecutionContext)

            // Verify что use case был вызван
            coVerify(exactly = 1) { sendCurrencyRatesUseCase(any(), any()) }
        }

        @Test
        fun `должен обработать null fireTime`() = runTest {
            // Arrange
            every { jobExecutionContext.fireTime } returns null
            coEvery { sendCurrencyRatesUseCase(any(), any()) } returns Result.success(Unit)

            // Act & Assert - не должно падать
            job.execute(jobExecutionContext)

            // Verify
            coVerify(exactly = 1) { sendCurrencyRatesUseCase(any(), any()) }
        }
    }

    @Nested
    @DisplayName("Конфигурация")
    inner class Configuration {

        @Test
        fun `должен использовать chatId из AppConfig`() = runTest {
            // Arrange
            val customConfig = AppConfig(
                botToken = "custom-token",
                chatId = "custom-chat-123",
                schedulerCron = "0 0 * * * ?",
                databasePath = "mem:custom-db",
                unkeyRootKey = "test-unkey-root-key",
                internalApiKey = "test-internal-key"
            )
            jobDataMap["config"] = customConfig
            coEvery { sendCurrencyRatesUseCase(any(), any()) } returns Result.success(Unit)

            // Act
            job.execute(jobExecutionContext)

            // Assert
            coVerify { sendCurrencyRatesUseCase("custom-chat-123", false) }
        }

        @Test
        fun `должен работать с разными экземплярами SendCurrencyRatesUseCase`() = runTest {
            // Arrange
            val anotherUseCase = mockk<SendCurrencyRatesUseCase>()
            coEvery { anotherUseCase(any()) } returns Result.success(Unit)
            jobDataMap["sendCurrencyRatesUseCase"] = anotherUseCase

            // Act
            job.execute(jobExecutionContext)

            // Assert
            coVerify(exactly = 1) { anotherUseCase(any()) }
            coVerify(exactly = 0) { sendCurrencyRatesUseCase(any(), any()) }
        }
    }
}
