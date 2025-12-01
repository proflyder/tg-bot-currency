package dev.proflyder.currency.api

import dev.proflyder.currency.configureRouting
import dev.proflyder.currency.data.dto.CurrencyHistoryResponseDto
import dev.proflyder.currency.data.remote.unkey.UnkeyClient
import dev.proflyder.currency.data.remote.unkey.UnkeyVerifyData
import dev.proflyder.currency.data.remote.unkey.UnkeyVerifyResponse
import dev.proflyder.currency.domain.model.CurrencyRateRecord
import dev.proflyder.currency.domain.model.CurrencyRateSnapshot
import dev.proflyder.currency.domain.model.ExchangeRateSnapshot
import dev.proflyder.currency.domain.usecase.GetCurrencyHistoryUseCase
import dev.proflyder.currency.domain.usecase.GetLatestCurrencyRateUseCase
import dev.proflyder.currency.presentation.auth.configureAuthentication
import dev.proflyder.currency.presentation.controller.CurrencyHistoryController
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.test.KoinTest

@DisplayName("Authentication Integration Tests")
class AuthenticationTest : KoinTest {

    @Nested
    @DisplayName("GET /api/history with authentication")
    inner class HistoryEndpointAuth {

        @Test
        fun `должен вернуть 200 с валидным API ключом`() = testApplication {
            // Arrange
            val validApiKey = "valid_api_key_123"
            val mockRecords = listOf(
                CurrencyRateRecord(
                    timestamp = Instant.parse("2025-11-30T12:00:00Z"),
                    rates = CurrencyRateSnapshot(
                        usdToKzt = ExchangeRateSnapshot(buy = 485.50, sell = 487.20),
                        rubToKzt = ExchangeRateSnapshot(buy = 4.85, sell = 4.92)
                    )
                )
            )

            val mockUseCase = mockk<GetCurrencyHistoryUseCase>()
            coEvery { mockUseCase() } returns Result.success(mockRecords)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(validApiKey) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(
                        valid = true,
                        keyId = "key_123",
                        name = "test-key",
                        ownerId = "owner_123"
                    )
                )
            )

            val mockController = CurrencyHistoryController(mockUseCase, mockk())

            // Setup application with mocks
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
            val response = client.get("/api/history") {
                header(HttpHeaders.Authorization, "Bearer $validApiKey")
            }

            // Assert
            response.status shouldBe HttpStatusCode.OK
            val body = response.body<CurrencyHistoryResponseDto>()
            body.success shouldBe true
            body.data.records.size shouldBe 1
        }

        @Test
        fun `должен вернуть 401 без Authorization header`() = testApplication {
            // Arrange
            val mockUseCase = mockk<GetCurrencyHistoryUseCase>()
            val mockUnkeyClient = mockk<UnkeyClient>()
            val mockController = CurrencyHistoryController(mockUseCase, mockk())

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
            val response = client.get("/api/history")

            // Assert
            response.status shouldBe HttpStatusCode.Unauthorized
        }

        @Test
        fun `должен вернуть 401 с невалидным API ключом`() = testApplication {
            // Arrange
            val invalidApiKey = "invalid_api_key_456"

            val mockUseCase = mockk<GetCurrencyHistoryUseCase>()
            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(invalidApiKey) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(
                        valid = false,
                        code = "NOT_FOUND"
                    )
                )
            )

            val mockController = CurrencyHistoryController(mockUseCase, mockk())

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
            val response = client.get("/api/history") {
                header(HttpHeaders.Authorization, "Bearer $invalidApiKey")
            }

            // Assert
            response.status shouldBe HttpStatusCode.Unauthorized
        }

        @Test
        fun `должен вернуть 401 если Authorization header не содержит Bearer`() = testApplication {
            // Arrange
            val mockUseCase = mockk<GetCurrencyHistoryUseCase>()
            val mockUnkeyClient = mockk<UnkeyClient>()
            val mockController = CurrencyHistoryController(mockUseCase, mockk())

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
            val response = client.get("/api/history") {
                header(HttpHeaders.Authorization, "some_api_key_without_bearer")
            }

            // Assert
            response.status shouldBe HttpStatusCode.Unauthorized
        }

        @Test
        fun `должен вернуть 401 если Unkey API недоступен`() = testApplication {
            // Arrange
            val apiKey = "some_api_key"

            val mockUseCase = mockk<GetCurrencyHistoryUseCase>()
            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(apiKey) } returns Result.failure(
                Exception("Network error")
            )

            val mockController = CurrencyHistoryController(mockUseCase, mockk())

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
            val response = client.get("/api/history") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }

            // Assert
            response.status shouldBe HttpStatusCode.Unauthorized
        }

        @Test
        fun `должен вернуть 401 с пустым API ключом после Bearer`() = testApplication {
            // Arrange
            val mockUseCase = mockk<GetCurrencyHistoryUseCase>()
            val mockUnkeyClient = mockk<UnkeyClient>()
            val mockController = CurrencyHistoryController(mockUseCase, mockk())

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
            val response = client.get("/api/history") {
                header(HttpHeaders.Authorization, "Bearer ")
            }

            // Assert
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }
}
