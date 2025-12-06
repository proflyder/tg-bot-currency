package dev.proflyder.currency.presentation.routes

import dev.proflyder.currency.data.dto.TriggerRequestDto
import dev.proflyder.currency.data.dto.TriggerResponseDto
import dev.proflyder.currency.presentation.controller.TriggerController
import dev.proflyder.currency.presentation.dto.error.ErrorResponse
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.*
import io.ktor.server.routing.*

fun Route.triggerRoutes(triggerController: TriggerController) {
    post("/api/trigger", {
        tags = listOf("Triggers")
        summary = "Принудительное обновление курсов валют"
        description = """
            Запускает немедленное обновление курсов валют:
            1. Парсинг актуальных курсов с kurs.kz
            2. Отправка уведомления в Telegram (независимо от порогов изменения)
            3. Опционально: сохранение в БД (по умолчанию отключено для ручных триггеров)

            Можно указать кастомный chat_id для отправки в конкретный чат.

            Ответ включает время выполнения операции в миллисекундах.
        """.trimIndent()
        securitySchemeNames = listOf("UnkeyAuth")
        request {
            body<TriggerRequestDto> {
                description = "Опциональные параметры запроса"
                required = false
                example("Без параметров (использовать дефолтный chat_id)") {
                    value = TriggerRequestDto()
                }
                example("С кастомным chat_id") {
                    value = TriggerRequestDto(
                        chatId = "-1001234567890"
                    )
                }
            }
        }
        response {
            HttpStatusCode.OK to {
                description = "Курсы успешно обновлены и отправлены в Telegram"
                body<TriggerResponseDto> {
                    example("Успешное обновление") {
                        value = TriggerResponseDto(
                            message = "Currency rates updated and sent to Telegram successfully",
                            executionTimeMs = 1234
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
                            path = "/api/trigger",
                            errorCode = "UNAUTHORIZED",
                            requestId = "req-123456"
                        )
                    }
                }
            }
            HttpStatusCode.BadGateway to {
                description = "Ошибка внешнего сервиса (парсинг или Telegram API)"
                body<ErrorResponse> {
                    example("Ошибка при парсинге kurs.kz") {
                        value = ErrorResponse.create(
                            status = 502,
                            error = "Bad Gateway",
                            message = "Failed to update and send currency rates: Connection timeout",
                            path = "/api/trigger",
                            errorCode = "EXTERNAL_SERVICE_ERROR",
                            service = "TriggerController.triggerCurrencyUpdate",
                            requestId = "req-123456",
                            details = mapOf(
                                "chatId" to "-1001234567890",
                                "executionTimeMs" to "5000"
                            )
                        )
                    }
                    example("Ошибка Telegram API") {
                        value = ErrorResponse.create(
                            status = 502,
                            error = "Bad Gateway",
                            message = "Failed to update and send currency rates: Invalid chat_id",
                            path = "/api/trigger",
                            errorCode = "EXTERNAL_SERVICE_ERROR",
                            service = "TriggerController.triggerCurrencyUpdate",
                            requestId = "req-123456"
                        )
                    }
                }
            }
        }
    }) {
        triggerController.triggerCurrencyUpdate(call)
    }
}
