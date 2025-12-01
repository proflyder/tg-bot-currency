package dev.proflyder.currency.api

import dev.proflyder.currency.TestFixtures
import dev.proflyder.currency.configureRouting
import dev.proflyder.currency.data.dto.CurrencyHistoryResponseDto
import dev.proflyder.currency.domain.model.CurrencyRateRecord
import dev.proflyder.currency.domain.model.CurrencyRateSnapshot
import dev.proflyder.currency.domain.model.ExchangeRateSnapshot
import dev.proflyder.currency.domain.usecase.GetCurrencyHistoryUseCase
import dev.proflyder.currency.presentation.controller.CurrencyHistoryController
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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

            val mockController = CurrencyHistoryController(mockUseCase)

            // Setup application with mock
            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                    })
                }
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
            response.status shouldBe HttpStatusCode.OK
            response.contentType()?.withoutParameters() shouldBe ContentType.Application.Json

            val body = response.body<CurrencyHistoryResponseDto>()
            body.success shouldBe true
            body.data.records.size shouldBe 2
            body.data.totalCount shouldBe 2
            body.message shouldBe "Currency history fetched successfully"
        }

        @Test
        fun `должен вернуть 200 и пустой список если история пуста`() = testApplication {
            // Arrange
            val mockUseCase = mockk<GetCurrencyHistoryUseCase>()
            coEvery { mockUseCase() } returns Result.success(emptyList())

            val mockController = CurrencyHistoryController(mockUseCase)

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                    })
                }
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
            response.status shouldBe HttpStatusCode.OK

            val body = response.body<CurrencyHistoryResponseDto>()
            body.success shouldBe true
            body.data.records.size shouldBe 0
            body.data.totalCount shouldBe 0
        }

        @Test
        fun `должен вернуть 500 если use case вернул ошибку`() = testApplication {
            // Arrange
            val mockUseCase = mockk<GetCurrencyHistoryUseCase>()
            coEvery { mockUseCase() } returns Result.failure(Exception("Database error"))

            val mockController = CurrencyHistoryController(mockUseCase)

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                    })
                }
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
            response.status shouldBe HttpStatusCode.InternalServerError

            val body = response.body<CurrencyHistoryResponseDto>()
            body.success shouldBe false
            body.data.records.size shouldBe 0
            body.data.totalCount shouldBe 0
            body.message shouldNotBe null
            body.message!! shouldBe "Failed to fetch currency history: Database error"
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

            val mockController = CurrencyHistoryController(mockUseCase)

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                    })
                }
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
            response.status shouldBe HttpStatusCode.OK

            val body = response.body<CurrencyHistoryResponseDto>()
            body.success shouldBe true
            body.data.records.size shouldBe 1

            val record = body.data.records.first()
            record.timestamp shouldBe TestFixtures.sampleTimestamp
            record.rates.usdToKzt.buy shouldBe TestFixtures.sampleExchangeRateUsd.buy
            record.rates.usdToKzt.sell shouldBe TestFixtures.sampleExchangeRateUsd.sell
            record.rates.rubToKzt.buy shouldBe TestFixtures.sampleExchangeRateRub.buy
            record.rates.rubToKzt.sell shouldBe TestFixtures.sampleExchangeRateRub.sell
        }
    }
}
