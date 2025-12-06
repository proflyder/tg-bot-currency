package dev.proflyder.currency.presentation.routes

import dev.proflyder.currency.data.dto.*
import dev.proflyder.currency.presentation.controller.CurrencyHistoryController
import dev.proflyder.currency.presentation.dto.error.ErrorResponse
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.ktor.http.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock

fun Route.currencyHistoryRoutes(currencyHistoryController: CurrencyHistoryController) {
    get("/api/history", {
        tags = listOf("Currency History")
        summary = "Получить историю курсов валют"
        description = """
            Возвращает полную историю записей о курсах USD и RUB к тенге (KZT).

            Данные включают:
            - Временную метку записи (ISO 8601 format)
            - Курсы покупки и продажи для USD → KZT
            - Курсы покупки и продажи для RUB → KZT

            Записи отсортированы по времени (от новых к старым).
            Данные обновляются автоматически по расписанию через Quartz Scheduler.
        """.trimIndent()
        securitySchemeNames = listOf("UnkeyAuth")
        response {
            HttpStatusCode.OK to {
                description = "Успешно получена история курсов"
                body<CurrencyHistoryResponseDto> {
                    example("Успешный ответ с историей курсов") {
                        value = CurrencyHistoryResponseDto(
                            records = listOf(
                                CurrencyRateRecordDto(
                                    timestamp = kotlinx.datetime.Instant.parse("2025-12-06T10:00:00Z"),
                                    rates = CurrencyRatesDto(
                                        usdToKzt = ExchangeRateDto(buy = 498.50, sell = 502.30),
                                        rubToKzt = ExchangeRateDto(buy = 4.85, sell = 4.95)
                                    )
                                )
                            ),
                            totalCount = 1
                        )
                    }
                }
            }
            HttpStatusCode.Unauthorized to {
                description = "Отсутствует или невалидный API ключ"
                body<ErrorResponse> {
                    example("Ошибка аутентификации") {
                        value = ErrorResponse.create(
                            status = 401,
                            error = "Unauthorized",
                            message = "Authentication required: Invalid or missing API key",
                            path = "/api/history",
                            errorCode = "UNAUTHORIZED",
                            requestId = "req-123456"
                        )
                    }
                }
            }
            HttpStatusCode.InternalServerError to {
                description = "Ошибка сервера при получении данных"
                body<ErrorResponse> {
                    example("Ошибка базы данных") {
                        value = ErrorResponse.create(
                            status = 500,
                            error = "Internal Server Error",
                            message = "Failed to fetch currency history: Database connection error",
                            path = "/api/history",
                            errorCode = "DATABASE_ERROR",
                            service = "CurrencyHistoryController.getHistory",
                            requestId = "req-123456"
                        )
                    }
                }
            }
        }
    }) {
        currencyHistoryController.getHistory(call)
    }

    get("/api/latest", {
        tags = listOf("Currency History")
        summary = "Получить актуальный курс валют"
        description = """
            Возвращает самую свежую (последнюю по времени) запись о курсах USD и RUB к тенге (KZT).

            Используется для получения актуального курса без загрузки всей истории.
            Если база данных пуста, возвращается статус 404 Not Found.

            Данные включают:
            - Временную метку последней записи
            - Актуальные курсы покупки и продажи для обеих валют
        """.trimIndent()
        securitySchemeNames = listOf("UnkeyAuth")
        response {
            HttpStatusCode.OK to {
                description = "Успешно получен актуальный курс"
                body<LatestCurrencyRateResponseDto> {
                    example("Успешный ответ с актуальным курсом") {
                        value = LatestCurrencyRateResponseDto(
                            timestamp = kotlinx.datetime.Instant.parse("2025-12-06T10:30:00Z"),
                            rates = CurrencyRatesDto(
                                usdToKzt = ExchangeRateDto(buy = 498.50, sell = 502.30),
                                rubToKzt = ExchangeRateDto(buy = 4.85, sell = 4.95)
                            )
                        )
                    }
                }
            }
            HttpStatusCode.Unauthorized to {
                description = "Отсутствует или невалидный API ключ"
                body<ErrorResponse> {
                    example("Ошибка аутентификации") {
                        value = ErrorResponse.create(
                            status = 401,
                            error = "Unauthorized",
                            message = "Authentication required: Invalid or missing API key",
                            path = "/api/latest",
                            errorCode = "UNAUTHORIZED",
                            requestId = "req-123456"
                        )
                    }
                }
            }
            HttpStatusCode.NotFound to {
                description = "Нет записей в базе данных"
                body<ErrorResponse> {
                    example("База данных пуста") {
                        value = ErrorResponse.create(
                            status = 404,
                            error = "Not Found",
                            message = "No currency rates found in database",
                            path = "/api/latest",
                            errorCode = "NOT_FOUND",
                            service = "CurrencyHistoryController.getLatest",
                            requestId = "req-123456"
                        )
                    }
                }
            }
            HttpStatusCode.InternalServerError to {
                description = "Ошибка сервера при получении данных"
                body<ErrorResponse> {
                    example("Ошибка базы данных") {
                        value = ErrorResponse.create(
                            status = 500,
                            error = "Internal Server Error",
                            message = "Failed to fetch latest currency rate: Database connection error",
                            path = "/api/latest",
                            errorCode = "DATABASE_ERROR",
                            service = "CurrencyHistoryController.getLatest",
                            requestId = "req-123456"
                        )
                    }
                }
            }
        }
    }) {
        currencyHistoryController.getLatest(call)
    }

    delete("/api/history", {
        tags = listOf("Currency History")
        summary = "Удалить всю историю курсов"
        description = """
            Удаляет все записи истории курсов валют из базы данных H2.

            ⚠️ ВНИМАНИЕ: Это необратимая операция!
            Все исторические данные будут безвозвратно удалены.

            Используется для:
            - Очистки базы данных при тестировании
            - Полного сброса данных перед миграцией
            - Освобождения места на диске

            Возвращает количество удаленных записей.
        """.trimIndent()
        securitySchemeNames = listOf("UnkeyAuth")
        response {
            HttpStatusCode.OK to {
                description = "Успешно удалена вся история курсов"
                body<DeleteHistoryResponseDto> {
                    example("Успешное удаление 50 записей") {
                        value = DeleteHistoryResponseDto(
                            deletedCount = 50,
                            message = "Successfully deleted 50 currency history records"
                        )
                    }
                }
            }
            HttpStatusCode.Unauthorized to {
                description = "Отсутствует или невалидный API ключ"
                body<ErrorResponse> {
                    example("Ошибка аутентификации") {
                        value = ErrorResponse.create(
                            status = 401,
                            error = "Unauthorized",
                            message = "Authentication required: Invalid or missing API key",
                            path = "/api/history",
                            errorCode = "UNAUTHORIZED",
                            requestId = "req-123456"
                        )
                    }
                }
            }
            HttpStatusCode.InternalServerError to {
                description = "Ошибка сервера при удалении истории"
                body<ErrorResponse> {
                    example("Ошибка базы данных") {
                        value = ErrorResponse.create(
                            status = 500,
                            error = "Internal Server Error",
                            message = "Failed to delete currency history: Database connection error",
                            path = "/api/history",
                            errorCode = "DATABASE_ERROR",
                            service = "CurrencyHistoryController.deleteHistory",
                            requestId = "req-123456"
                        )
                    }
                }
            }
        }
    }) {
        currencyHistoryController.deleteHistory(call)
    }
}
