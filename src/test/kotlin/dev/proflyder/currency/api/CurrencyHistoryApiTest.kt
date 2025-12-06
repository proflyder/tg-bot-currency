package dev.proflyder.currency.api

import dev.proflyder.currency.TestFixtures
import dev.proflyder.currency.configureRouting
import dev.proflyder.currency.data.dto.CurrencyHistoryResponseDto
import dev.proflyder.currency.data.dto.DeleteHistoryResponseDto
import dev.proflyder.currency.data.dto.LatestCurrencyRateResponseDto
import dev.proflyder.currency.data.remote.unkey.UnkeyClient
import dev.proflyder.currency.data.remote.unkey.UnkeyVerifyData
import dev.proflyder.currency.data.remote.unkey.UnkeyVerifyResponse
import dev.proflyder.currency.domain.model.CurrencyRateRecord
import dev.proflyder.currency.domain.model.CurrencyRateSnapshot
import dev.proflyder.currency.domain.model.ExchangeRateSnapshot
import dev.proflyder.currency.domain.usecase.DeleteCurrencyHistoryUseCase
import dev.proflyder.currency.domain.usecase.GetCurrencyHistoryUseCase
import dev.proflyder.currency.domain.usecase.GetLatestCurrencyRateUseCase
import dev.proflyder.currency.presentation.auth.configureAuthentication
import dev.proflyder.currency.presentation.controller.CurrencyHistoryController
import dev.proflyder.currency.presentation.exception.configureExceptionHandling
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
import io.mockk.mockk
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.test.KoinTest

@DisplayName("Currency History API Integration Tests")
class CurrencyHistoryApiTest : KoinTest {

    @Nested
    @DisplayName("GET /api/history")
    inner class GetHistoryEndpoint {

        @Test
        fun `должен вернуть 200 и список записей истории`() = testApplication {
            // Arrange
            val mockRecords = listOf(
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

            val mockUseCase = mockk<GetCurrencyHistoryUseCase>()
            coEvery { mockUseCase() } returns Result.success(mockRecords)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            val mockController = CurrencyHistoryController(mockUseCase, mockk(), mockk())

            // Setup application with mock
            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                        single { mockk<dev.proflyder.currency.presentation.controller.TriggerController>(relaxed = true) }
                        single { mockk<dev.proflyder.currency.presentation.controller.TelegramWebhookController>(relaxed = true) }
                    })
                }
                configureExceptionHandling()
                configureAuthentication(mockUnkeyClient)
                configureRouting(io.micrometer.prometheus.PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT))
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
                header(HttpHeaders.Authorization, "Bearer test-api-key")
            }

            // Assert
            response.status shouldBe HttpStatusCode.OK
            response.contentType()?.withoutParameters() shouldBe ContentType.Application.Json

            val body = response.body<CurrencyHistoryResponseDto>()
            body.records.size shouldBe 2
            body.totalCount shouldBe 2
        }

        @Test
        fun `должен вернуть 200 и пустой список если история пуста`() = testApplication {
            // Arrange
            val mockUseCase = mockk<GetCurrencyHistoryUseCase>()
            coEvery { mockUseCase() } returns Result.success(emptyList())

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            val mockController = CurrencyHistoryController(mockUseCase, mockk(), mockk())

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                        single { mockk<dev.proflyder.currency.presentation.controller.TriggerController>(relaxed = true) }
                        single { mockk<dev.proflyder.currency.presentation.controller.TelegramWebhookController>(relaxed = true) }
                    })
                }
                configureExceptionHandling()
                configureAuthentication(mockUnkeyClient)
                configureRouting(io.micrometer.prometheus.PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT))
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
                header(HttpHeaders.Authorization, "Bearer test-api-key")
            }

            // Assert
            response.status shouldBe HttpStatusCode.OK

            val body = response.body<CurrencyHistoryResponseDto>()
            body.records.size shouldBe 0
            body.totalCount shouldBe 0
        }

        @Test
        fun `должен вернуть 500 если use case вернул ошибку`() = testApplication {
            // Arrange
            val mockUseCase = mockk<GetCurrencyHistoryUseCase>()
            coEvery { mockUseCase() } returns Result.failure(Exception("Database error"))

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            val mockController = CurrencyHistoryController(mockUseCase, mockk(), mockk())

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                        single { mockk<dev.proflyder.currency.presentation.controller.TriggerController>(relaxed = true) }
                        single { mockk<dev.proflyder.currency.presentation.controller.TelegramWebhookController>(relaxed = true) }
                    })
                }
                configureExceptionHandling()
                configureAuthentication(mockUnkeyClient)
                configureRouting(io.micrometer.prometheus.PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT))
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
                header(HttpHeaders.Authorization, "Bearer test-api-key")
            }

            // Assert
            response.status shouldBe HttpStatusCode.InternalServerError
        }

        @Test
        fun `должен вернуть корректный JSON формат`() = testApplication {
            // Arrange
            val mockRecord = CurrencyRateRecord(
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

            val mockUseCase = mockk<GetCurrencyHistoryUseCase>()
            coEvery { mockUseCase() } returns Result.success(listOf(mockRecord))

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            val mockController = CurrencyHistoryController(mockUseCase, mockk(), mockk())

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                        single { mockk<dev.proflyder.currency.presentation.controller.TriggerController>(relaxed = true) }
                        single { mockk<dev.proflyder.currency.presentation.controller.TelegramWebhookController>(relaxed = true) }
                    })
                }
                configureExceptionHandling()
                configureAuthentication(mockUnkeyClient)
                configureRouting(io.micrometer.prometheus.PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT))
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
                header(HttpHeaders.Authorization, "Bearer test-api-key")
            }

            // Assert
            response.status shouldBe HttpStatusCode.OK

            val body = response.body<CurrencyHistoryResponseDto>()
            body.records.size shouldBe 1

            val record = body.records.first()
            record.timestamp shouldBe TestFixtures.sampleTimestamp
            record.rates.usdToKzt.buy shouldBe TestFixtures.sampleExchangeRateUsd.buy
            record.rates.usdToKzt.sell shouldBe TestFixtures.sampleExchangeRateUsd.sell
            record.rates.rubToKzt.buy shouldBe TestFixtures.sampleExchangeRateRub.buy
            record.rates.rubToKzt.sell shouldBe TestFixtures.sampleExchangeRateRub.sell
        }
    }

    @Nested
    @DisplayName("GET /api/latest")
    inner class GetLatestEndpoint {

        @Test
        fun `должен вернуть 200 и последнюю запись курса`() = testApplication {
            // Arrange
            val latestRecord = CurrencyRateRecord(
                timestamp = Instant.parse("2025-11-30T12:00:00Z"),
                rates = CurrencyRateSnapshot(
                    usdToKzt = ExchangeRateSnapshot(buy = 485.50, sell = 487.20),
                    rubToKzt = ExchangeRateSnapshot(buy = 4.85, sell = 4.92)
                )
            )

            val mockHistoryUseCase = mockk<GetCurrencyHistoryUseCase>()
            val mockLatestUseCase = mockk<GetLatestCurrencyRateUseCase>()
            coEvery { mockLatestUseCase() } returns Result.success(latestRecord)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            val mockController = CurrencyHistoryController(mockHistoryUseCase, mockLatestUseCase, mockk())

            // Setup application with mock
            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                        single { mockk<dev.proflyder.currency.presentation.controller.TriggerController>(relaxed = true) }
                        single { mockk<dev.proflyder.currency.presentation.controller.TelegramWebhookController>(relaxed = true) }
                    })
                }
                configureExceptionHandling()
                configureAuthentication(mockUnkeyClient)
                configureRouting(io.micrometer.prometheus.PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT))
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
            val response = client.get("/api/latest") {
                header(HttpHeaders.Authorization, "Bearer test-api-key")
            }

            // Assert
            response.status shouldBe HttpStatusCode.OK
            response.contentType()?.withoutParameters() shouldBe ContentType.Application.Json

            val body = response.body<LatestCurrencyRateResponseDto>()
            body.timestamp shouldBe Instant.parse("2025-11-30T12:00:00Z")
            body.rates.usdToKzt.buy shouldBe 485.50
            body.rates.usdToKzt.sell shouldBe 487.20
        }

        @Test
        fun `должен вернуть 404 если база данных пустая`() = testApplication {
            // Arrange
            val mockHistoryUseCase = mockk<GetCurrencyHistoryUseCase>()
            val mockLatestUseCase = mockk<GetLatestCurrencyRateUseCase>()
            coEvery { mockLatestUseCase() } returns Result.success(null)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            val mockController = CurrencyHistoryController(mockHistoryUseCase, mockLatestUseCase, mockk())

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                        single { mockk<dev.proflyder.currency.presentation.controller.TriggerController>(relaxed = true) }
                        single { mockk<dev.proflyder.currency.presentation.controller.TelegramWebhookController>(relaxed = true) }
                    })
                }
                configureExceptionHandling()
                configureAuthentication(mockUnkeyClient)
                configureRouting(io.micrometer.prometheus.PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT))
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
            val response = client.get("/api/latest") {
                header(HttpHeaders.Authorization, "Bearer test-api-key")
            }

            // Assert
            response.status shouldBe HttpStatusCode.NotFound
        }

        @Test
        fun `должен вернуть 500 если use case вернул ошибку`() = testApplication {
            // Arrange
            val mockHistoryUseCase = mockk<GetCurrencyHistoryUseCase>()
            val mockLatestUseCase = mockk<GetLatestCurrencyRateUseCase>()
            coEvery { mockLatestUseCase() } returns Result.failure(Exception("Database connection error"))

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            val mockController = CurrencyHistoryController(mockHistoryUseCase, mockLatestUseCase, mockk())

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                        single { mockk<dev.proflyder.currency.presentation.controller.TriggerController>(relaxed = true) }
                        single { mockk<dev.proflyder.currency.presentation.controller.TelegramWebhookController>(relaxed = true) }
                    })
                }
                configureExceptionHandling()
                configureAuthentication(mockUnkeyClient)
                configureRouting(io.micrometer.prometheus.PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT))
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
            val response = client.get("/api/latest") {
                header(HttpHeaders.Authorization, "Bearer test-api-key")
            }

            // Assert
            response.status shouldBe HttpStatusCode.InternalServerError
        }

        @Test
        fun `должен вернуть корректный JSON формат`() = testApplication {
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

            val mockHistoryUseCase = mockk<GetCurrencyHistoryUseCase>()
            val mockLatestUseCase = mockk<GetLatestCurrencyRateUseCase>()
            coEvery { mockLatestUseCase() } returns Result.success(latestRecord)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            val mockController = CurrencyHistoryController(mockHistoryUseCase, mockLatestUseCase, mockk())

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                        single { mockk<dev.proflyder.currency.presentation.controller.TriggerController>(relaxed = true) }
                        single { mockk<dev.proflyder.currency.presentation.controller.TelegramWebhookController>(relaxed = true) }
                    })
                }
                configureExceptionHandling()
                configureAuthentication(mockUnkeyClient)
                configureRouting(io.micrometer.prometheus.PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT))
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
            val response = client.get("/api/latest") {
                header(HttpHeaders.Authorization, "Bearer test-api-key")
            }

            // Assert
            response.status shouldBe HttpStatusCode.OK

            val body = response.body<LatestCurrencyRateResponseDto>()
            val record = body
            record.timestamp shouldBe TestFixtures.sampleTimestamp
            record.rates.usdToKzt.buy shouldBe TestFixtures.sampleExchangeRateUsd.buy
            record.rates.usdToKzt.sell shouldBe TestFixtures.sampleExchangeRateUsd.sell
            record.rates.rubToKzt.buy shouldBe TestFixtures.sampleExchangeRateRub.buy
            record.rates.rubToKzt.sell shouldBe TestFixtures.sampleExchangeRateRub.sell
        }

        @Test
        fun `должен требовать аутентификацию`() = testApplication {
            // Arrange
            val mockHistoryUseCase = mockk<GetCurrencyHistoryUseCase>()
            val mockLatestUseCase = mockk<GetLatestCurrencyRateUseCase>()

            val mockUnkeyClient = mockk<UnkeyClient>()

            val mockController = CurrencyHistoryController(mockHistoryUseCase, mockLatestUseCase, mockk())

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                        single { mockk<dev.proflyder.currency.presentation.controller.TriggerController>(relaxed = true) }
                        single { mockk<dev.proflyder.currency.presentation.controller.TelegramWebhookController>(relaxed = true) }
                    })
                }
                configureExceptionHandling()
                configureAuthentication(mockUnkeyClient)
                configureRouting(io.micrometer.prometheus.PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT))
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
            val response = client.get("/api/latest")

            // Assert
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Nested
    @DisplayName("DELETE /api/history")
    inner class DeleteHistoryEndpoint {

        @Test
        fun `должен вернуть 200 и количество удаленных записей`() = testApplication {
            // Arrange
            val deletedCount = 50
            val mockHistoryUseCase = mockk<GetCurrencyHistoryUseCase>()
            val mockLatestUseCase = mockk<GetLatestCurrencyRateUseCase>()
            val mockDeleteUseCase = mockk<DeleteCurrencyHistoryUseCase>()
            coEvery { mockDeleteUseCase() } returns Result.success(deletedCount)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            val mockController = CurrencyHistoryController(mockHistoryUseCase, mockLatestUseCase, mockDeleteUseCase)

            // Setup application with mock
            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                        single { mockk<dev.proflyder.currency.presentation.controller.TriggerController>(relaxed = true) }
                        single { mockk<dev.proflyder.currency.presentation.controller.TelegramWebhookController>(relaxed = true) }
                    })
                }
                configureExceptionHandling()
                configureAuthentication(mockUnkeyClient)
                configureRouting(io.micrometer.prometheus.PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT))
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
            val response = client.delete("/api/history") {
                header(HttpHeaders.Authorization, "Bearer test-api-key")
            }

            // Assert
            response.status shouldBe HttpStatusCode.OK
            response.contentType()?.withoutParameters() shouldBe ContentType.Application.Json

            val body = response.body<DeleteHistoryResponseDto>()
            body.deletedCount shouldBe deletedCount
            body.message shouldBe "Successfully deleted 50 currency history records"
        }

        @Test
        fun `должен вернуть 200 с 0 удаленных записей если база была пустая`() = testApplication {
            // Arrange
            val mockHistoryUseCase = mockk<GetCurrencyHistoryUseCase>()
            val mockLatestUseCase = mockk<GetLatestCurrencyRateUseCase>()
            val mockDeleteUseCase = mockk<DeleteCurrencyHistoryUseCase>()
            coEvery { mockDeleteUseCase() } returns Result.success(0)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            val mockController = CurrencyHistoryController(mockHistoryUseCase, mockLatestUseCase, mockDeleteUseCase)

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                        single { mockk<dev.proflyder.currency.presentation.controller.TriggerController>(relaxed = true) }
                        single { mockk<dev.proflyder.currency.presentation.controller.TelegramWebhookController>(relaxed = true) }
                    })
                }
                configureExceptionHandling()
                configureAuthentication(mockUnkeyClient)
                configureRouting(io.micrometer.prometheus.PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT))
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
            val response = client.delete("/api/history") {
                header(HttpHeaders.Authorization, "Bearer test-api-key")
            }

            // Assert
            response.status shouldBe HttpStatusCode.OK

            val body = response.body<DeleteHistoryResponseDto>()
            body.deletedCount shouldBe 0
            body.message shouldBe "Successfully deleted 0 currency history records"
        }

        @Test
        fun `должен вернуть 500 если use case вернул ошибку`() = testApplication {
            // Arrange
            val mockHistoryUseCase = mockk<GetCurrencyHistoryUseCase>()
            val mockLatestUseCase = mockk<GetLatestCurrencyRateUseCase>()
            val mockDeleteUseCase = mockk<DeleteCurrencyHistoryUseCase>()
            coEvery { mockDeleteUseCase() } returns Result.failure(Exception("Database error"))

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            val mockController = CurrencyHistoryController(mockHistoryUseCase, mockLatestUseCase, mockDeleteUseCase)

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                        single { mockk<dev.proflyder.currency.presentation.controller.TriggerController>(relaxed = true) }
                        single { mockk<dev.proflyder.currency.presentation.controller.TelegramWebhookController>(relaxed = true) }
                    })
                }
                configureExceptionHandling()
                configureAuthentication(mockUnkeyClient)
                configureRouting(io.micrometer.prometheus.PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT))
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
            val response = client.delete("/api/history") {
                header(HttpHeaders.Authorization, "Bearer test-api-key")
            }

            // Assert
            response.status shouldBe HttpStatusCode.InternalServerError
        }

        @Test
        fun `должен требовать аутентификацию`() = testApplication {
            // Arrange
            val mockHistoryUseCase = mockk<GetCurrencyHistoryUseCase>()
            val mockLatestUseCase = mockk<GetLatestCurrencyRateUseCase>()
            val mockDeleteUseCase = mockk<DeleteCurrencyHistoryUseCase>()

            val mockUnkeyClient = mockk<UnkeyClient>()

            val mockController = CurrencyHistoryController(mockHistoryUseCase, mockLatestUseCase, mockDeleteUseCase)

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                        single { mockk<dev.proflyder.currency.presentation.controller.TriggerController>(relaxed = true) }
                        single { mockk<dev.proflyder.currency.presentation.controller.TelegramWebhookController>(relaxed = true) }
                    })
                }
                configureExceptionHandling()
                configureAuthentication(mockUnkeyClient)
                configureRouting(io.micrometer.prometheus.PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT))
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
            val response = client.delete("/api/history")

            // Assert
            response.status shouldBe HttpStatusCode.Unauthorized
        }

        @Test
        fun `должен вернуть корректный JSON формат`() = testApplication {
            // Arrange
            val deletedCount = 100
            val mockHistoryUseCase = mockk<GetCurrencyHistoryUseCase>()
            val mockLatestUseCase = mockk<GetLatestCurrencyRateUseCase>()
            val mockDeleteUseCase = mockk<DeleteCurrencyHistoryUseCase>()
            coEvery { mockDeleteUseCase() } returns Result.success(deletedCount)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            val mockController = CurrencyHistoryController(mockHistoryUseCase, mockLatestUseCase, mockDeleteUseCase)

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                        single { mockk<dev.proflyder.currency.presentation.controller.TriggerController>(relaxed = true) }
                        single { mockk<dev.proflyder.currency.presentation.controller.TelegramWebhookController>(relaxed = true) }
                    })
                }
                configureExceptionHandling()
                configureAuthentication(mockUnkeyClient)
                configureRouting(io.micrometer.prometheus.PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT))
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
            val response = client.delete("/api/history") {
                header(HttpHeaders.Authorization, "Bearer test-api-key")
            }

            // Assert
            response.status shouldBe HttpStatusCode.OK

            val body = response.body<DeleteHistoryResponseDto>()
            body.deletedCount shouldBe deletedCount
            body.message shouldBe "Successfully deleted 100 currency history records"
        }
    }
}
