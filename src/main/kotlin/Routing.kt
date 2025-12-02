package dev.proflyder.currency

import dev.proflyder.currency.data.dto.CurrencyHistoryResponseDto
import dev.proflyder.currency.data.dto.DeleteHistoryResponseDto
import dev.proflyder.currency.data.dto.LatestCurrencyRateResponseDto
import dev.proflyder.currency.data.dto.TriggerResponseDto
import dev.proflyder.currency.presentation.controller.CurrencyHistoryController
import dev.proflyder.currency.presentation.controller.TelegramWebhookController
import dev.proflyder.currency.presentation.controller.TriggerController
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    // Настройка Content Negotiation для JSON сериализации
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // Внедряем контроллеры через Koin
    val currencyHistoryController by inject<CurrencyHistoryController>()
    val triggerController by inject<TriggerController>()
    val telegramWebhookController by inject<TelegramWebhookController>()

    routing {
        get("/", {
            hidden = true // Скрываем из Swagger
        }) {
            call.respondText("Hello World!")
        }

        // Swagger UI и OpenAPI спецификация
        route("api.json") {
            openApiSpec()
        }
        route("swagger") {
            swaggerUI("/api.json")
        }

        // Telegram webhook - no authentication needed (Telegram sends updates)
        post("/telegram/webhook", {
            tags = listOf("Webhooks")
            description = "Эндпоинт для приема обновлений от Telegram Bot API. Обрабатывает команды бота: /trigger, /start, /help"
            response {
                HttpStatusCode.OK to {
                    description = "Webhook обработан успешно"
                    body<String>()
                }
                HttpStatusCode.InternalServerError to {
                    description = "Ошибка при обработке webhook"
                    body<String>()
                }
            }
        }) {
            telegramWebhookController.handleWebhook(call)
        }

        // Currency History API - protected with Unkey authentication
        authenticate("unkey-auth") {
            get("/api/history", {
                tags = listOf("Currency History")
                description = "Возвращает список исторических записей о курсах USD и RUB к KZT за указанный период"
                securitySchemeNames = listOf("UnkeyAuth")
                request {
                    queryParameter<Int>("limit") {
                        description = "Максимальное количество записей (по умолчанию 100)"
                        required = false
                    }
                    queryParameter<Int>("offset") {
                        description = "Смещение для пагинации (по умолчанию 0)"
                        required = false
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        description = "Успешно получена история курсов"
                        body<CurrencyHistoryResponseDto> {
                            example("Успешный ответ с историей") {
                                value = CurrencyHistoryResponseDto(
                                    success = true,
                                    data = dev.proflyder.currency.data.dto.CurrencyHistoryDataDto(
                                        records = listOf(
                                            dev.proflyder.currency.data.dto.CurrencyRateRecordDto(
                                                timestamp = kotlinx.datetime.Instant.parse("2025-12-03T00:00:00Z"),
                                                rates = dev.proflyder.currency.data.dto.CurrencyRatesDto(
                                                    usdToKzt = dev.proflyder.currency.data.dto.ExchangeRateDto(
                                                        buy = 498.50,
                                                        sell = 502.30
                                                    ),
                                                    rubToKzt = dev.proflyder.currency.data.dto.ExchangeRateDto(
                                                        buy = 4.85,
                                                        sell = 4.95
                                                    )
                                                )
                                            ),
                                            dev.proflyder.currency.data.dto.CurrencyRateRecordDto(
                                                timestamp = kotlinx.datetime.Instant.parse("2025-12-02T00:00:00Z"),
                                                rates = dev.proflyder.currency.data.dto.CurrencyRatesDto(
                                                    usdToKzt = dev.proflyder.currency.data.dto.ExchangeRateDto(
                                                        buy = 497.80,
                                                        sell = 501.50
                                                    ),
                                                    rubToKzt = dev.proflyder.currency.data.dto.ExchangeRateDto(
                                                        buy = 4.82,
                                                        sell = 4.92
                                                    )
                                                )
                                            )
                                        ),
                                        totalCount = 2
                                    ),
                                    message = "Currency history fetched successfully"
                                )
                            }
                        }
                    }
                    HttpStatusCode.Unauthorized to {
                        description = "Отсутствует или невалидный API ключ"
                    }
                    HttpStatusCode.InternalServerError to {
                        description = "Ошибка сервера"
                    }
                }
            }) {
                currencyHistoryController.getHistory(call)
            }

            get("/api/latest", {
                tags = listOf("Currency History")
                description = "Возвращает самую свежую запись о курсах USD и RUB к KZT"
                securitySchemeNames = listOf("UnkeyAuth")
                response {
                    HttpStatusCode.OK to {
                        description = "Успешно получен последний курс"
                        body<LatestCurrencyRateResponseDto> {
                            example("Успешный ответ с последним курсом") {
                                value = LatestCurrencyRateResponseDto(
                                    success = true,
                                    data = dev.proflyder.currency.data.dto.CurrencyRateRecordDto(
                                        timestamp = kotlinx.datetime.Instant.parse("2025-12-03T01:00:00Z"),
                                        rates = dev.proflyder.currency.data.dto.CurrencyRatesDto(
                                            usdToKzt = dev.proflyder.currency.data.dto.ExchangeRateDto(
                                                buy = 498.50,
                                                sell = 502.30
                                            ),
                                            rubToKzt = dev.proflyder.currency.data.dto.ExchangeRateDto(
                                                buy = 4.85,
                                                sell = 4.95
                                            )
                                        )
                                    ),
                                    message = "Latest currency rate fetched successfully"
                                )
                            }
                        }
                    }
                    HttpStatusCode.Unauthorized to {
                        description = "Отсутствует или невалидный API ключ"
                    }
                    HttpStatusCode.NotFound to {
                        description = "Нет данных в базе"
                    }
                }
            }) {
                currencyHistoryController.getLatest(call)
            }

            delete("/api/history", {
                tags = listOf("Currency History")
                description = "Удаляет все записи истории курсов валют из базы данных"
                securitySchemeNames = listOf("UnkeyAuth")
                response {
                    HttpStatusCode.OK to {
                        description = "Успешно удалена вся история курсов"
                        body<DeleteHistoryResponseDto> {
                            example("Успешное удаление") {
                                value = DeleteHistoryResponseDto(
                                    success = true,
                                    message = "Successfully deleted 50 currency history records",
                                    deletedCount = 50
                                )
                            }
                        }
                    }
                    HttpStatusCode.Unauthorized to {
                        description = "Отсутствует или невалидный API ключ"
                    }
                    HttpStatusCode.InternalServerError to {
                        description = "Ошибка при удалении истории"
                        body<DeleteHistoryResponseDto> {
                            example("Ошибка при удалении") {
                                value = DeleteHistoryResponseDto(
                                    success = false,
                                    message = "Failed to delete currency history: Database error"
                                )
                            }
                        }
                    }
                }
            }) {
                currencyHistoryController.deleteHistory(call)
            }

            // Manual trigger for currency update
            post("/api/trigger", {
                tags = listOf("Triggers")
                description = "Запускает немедленное обновление курсов: парсинг с kurs.kz, сохранение в БД и отправка в Telegram канал (независимо от порогов изменения)"
                securitySchemeNames = listOf("UnkeyAuth")
                response {
                    HttpStatusCode.OK to {
                        description = "Курсы успешно обновлены"
                        body<TriggerResponseDto> {
                            example("Успешное обновление") {
                                value = TriggerResponseDto(
                                    success = true,
                                    message = "Currency rates updated and sent to Telegram successfully"
                                )
                            }
                        }
                    }
                    HttpStatusCode.Unauthorized to {
                        description = "Отсутствует или невалидный API ключ"
                    }
                    HttpStatusCode.InternalServerError to {
                        description = "Ошибка при обновлении курсов"
                        body<TriggerResponseDto> {
                            example("Ошибка при парсинге") {
                                value = TriggerResponseDto(
                                    success = false,
                                    message = "Failed to update currency rates: Connection timeout"
                                )
                            }
                        }
                    }
                }
            }) {
                triggerController.triggerCurrencyUpdate(call)
            }
        }
    }
}
