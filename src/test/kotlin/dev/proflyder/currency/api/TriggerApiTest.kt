package dev.proflyder.currency.api

import dev.proflyder.currency.configureRouting
import dev.proflyder.currency.data.dto.TriggerResponseDto
import dev.proflyder.currency.data.remote.unkey.UnkeyClient
import dev.proflyder.currency.data.remote.unkey.UnkeyVerifyData
import dev.proflyder.currency.data.remote.unkey.UnkeyVerifyResponse
import dev.proflyder.currency.di.AppConfig
import dev.proflyder.currency.domain.usecase.SendCurrencyRatesUseCase
import dev.proflyder.currency.presentation.auth.configureAuthentication
import dev.proflyder.currency.presentation.controller.TriggerController
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.test.KoinTest

@DisplayName("Trigger API Integration Tests")
class TriggerApiTest : KoinTest {

    @Nested
    @DisplayName("POST /api/trigger")
    inner class TriggerEndpoint {

        @Test
        fun `должен вернуть 200 при успешном запуске обновления курсов`() = testApplication {
            // Arrange
            val testConfig = AppConfig(
                botToken = "test-token",
                chatId = "test-chat-id",
                schedulerCron = "0 0 * * * ?",
                databasePath = "mem:test",
                unkeyRootKey = "test-unkey-key",
                internalApiKey = "test-internal-key"
            )

            val mockSendCurrencyRatesUseCase = mockk<SendCurrencyRatesUseCase>()
            coEvery { mockSendCurrencyRatesUseCase(any(), any(), any()) } returns Result.success(Unit)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            val mockController = TriggerController(mockSendCurrencyRatesUseCase, testConfig)

            // Setup application with mock
            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                    })
                }
                configureAuthentication(mockUnkeyClient)
                configureRouting()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
            }

            // Act
            val response = client.post("/api/trigger") {
                header(HttpHeaders.Authorization, "Bearer test-api-key")
            }

            // Assert
            response.status shouldBe HttpStatusCode.OK
            response.contentType()?.withoutParameters() shouldBe ContentType.Application.Json

            val body = response.body<TriggerResponseDto>()
            body.success shouldBe true
            body.message shouldBe "Currency rates updated and sent to Telegram successfully"

            // Verify use case was called with correct chatId
            coVerify(exactly = 1) { mockSendCurrencyRatesUseCase("test-chat-id", true, false) }
        }

        @Test
        fun `должен вернуть 500 при ошибке выполнения use case`() = testApplication {
            // Arrange
            val testConfig = AppConfig(
                botToken = "test-token",
                chatId = "test-chat-id",
                schedulerCron = "0 0 * * * ?",
                databasePath = "mem:test",
                unkeyRootKey = "test-unkey-key",
                internalApiKey = "test-internal-key"
            )

            val mockSendCurrencyRatesUseCase = mockk<SendCurrencyRatesUseCase>()
            coEvery { mockSendCurrencyRatesUseCase(any(), any(), any()) } returns Result.failure(
                Exception("Failed to parse currency rates")
            )

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            val mockController = TriggerController(mockSendCurrencyRatesUseCase, testConfig)

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                    })
                }
                configureAuthentication(mockUnkeyClient)
                configureRouting()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
            }

            // Act
            val response = client.post("/api/trigger") {
                header(HttpHeaders.Authorization, "Bearer test-api-key")
            }

            // Assert
            response.status shouldBe HttpStatusCode.InternalServerError

            val body = response.body<TriggerResponseDto>()
            body.success shouldBe false
            body.message shouldNotBe null
            body.message shouldBe "Failed to update currency rates: Failed to parse currency rates"

            coVerify(exactly = 1) { mockSendCurrencyRatesUseCase("test-chat-id", true, false) }
        }

        @Test
        fun `должен требовать аутентификацию`() = testApplication {
            // Arrange
            val testConfig = AppConfig(
                botToken = "test-token",
                chatId = "test-chat-id",
                schedulerCron = "0 0 * * * ?",
                databasePath = "mem:test",
                unkeyRootKey = "test-unkey-key",
                internalApiKey = "test-internal-key"
            )

            val mockSendCurrencyRatesUseCase = mockk<SendCurrencyRatesUseCase>()
            val mockUnkeyClient = mockk<UnkeyClient>()

            val mockController = TriggerController(mockSendCurrencyRatesUseCase, testConfig)

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                    })
                }
                configureAuthentication(mockUnkeyClient)
                configureRouting()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
            }

            // Act
            val response = client.post("/api/trigger")

            // Assert
            response.status shouldBe HttpStatusCode.Unauthorized

            // Use case should not be called without authentication
            coVerify(exactly = 0) { mockSendCurrencyRatesUseCase(any(), any(), any()) }
        }

        @Test
        fun `должен вернуть 401 с невалидным API ключом`() = testApplication {
            // Arrange
            val testConfig = AppConfig(
                botToken = "test-token",
                chatId = "test-chat-id",
                schedulerCron = "0 0 * * * ?",
                databasePath = "mem:test",
                unkeyRootKey = "test-unkey-key",
                internalApiKey = "test-internal-key"
            )

            val mockSendCurrencyRatesUseCase = mockk<SendCurrencyRatesUseCase>()

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = false, code = "NOT_FOUND")
                )
            )

            val mockController = TriggerController(mockSendCurrencyRatesUseCase, testConfig)

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                    })
                }
                configureAuthentication(mockUnkeyClient)
                configureRouting()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
            }

            // Act
            val response = client.post("/api/trigger") {
                header(HttpHeaders.Authorization, "Bearer invalid-api-key")
            }

            // Assert
            response.status shouldBe HttpStatusCode.Unauthorized

            coVerify(exactly = 0) { mockSendCurrencyRatesUseCase(any(), any(), any()) }
        }

        @Test
        fun `должен корректно обработать корректный JSON response`() = testApplication {
            // Arrange
            val testConfig = AppConfig(
                botToken = "test-token",
                chatId = "test-chat-id",
                schedulerCron = "0 0 * * * ?",
                databasePath = "mem:test",
                unkeyRootKey = "test-unkey-key",
                internalApiKey = "test-internal-key"
            )

            val mockSendCurrencyRatesUseCase = mockk<SendCurrencyRatesUseCase>()
            coEvery { mockSendCurrencyRatesUseCase(any(), any(), any()) } returns Result.success(Unit)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            val mockController = TriggerController(mockSendCurrencyRatesUseCase, testConfig)

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                    })
                }
                configureAuthentication(mockUnkeyClient)
                configureRouting()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
            }

            // Act
            val response = client.post("/api/trigger") {
                header(HttpHeaders.Authorization, "Bearer test-api-key")
            }

            // Assert
            response.status shouldBe HttpStatusCode.OK

            val body = response.body<TriggerResponseDto>()
            body.success shouldBe true
            body.message shouldNotBe null
        }
    }
}
