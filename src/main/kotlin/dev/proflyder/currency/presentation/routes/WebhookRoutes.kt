package dev.proflyder.currency.presentation.routes

import dev.proflyder.currency.data.dto.telegram.Chat
import dev.proflyder.currency.data.dto.telegram.TelegramMessage
import dev.proflyder.currency.data.dto.telegram.TelegramUpdate
import dev.proflyder.currency.presentation.controller.TelegramWebhookController
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.*
import io.ktor.server.routing.*

fun Route.webhookRoutes(telegramWebhookController: TelegramWebhookController) {
    post("/telegram/webhook", {
        hidden = true
        tags = listOf("Webhooks")
        summary = "Webhook для Telegram Bot API"
        description = """
            Эндпоинт для приема обновлений (updates) от Telegram Bot API.

            Поддерживаемые команды бота:
            - /trigger - Принудительное обновление курсов валют
            - /start - Приветственное сообщение
            - /help - Справка по командам бота

            Этот эндпоинт настраивается через Telegram Bot API методом setWebhook.
            Telegram отправляет POST запросы на этот URL при каждом взаимодействии с ботом.
        """.trimIndent()
        request {
            body<TelegramUpdate> {
                description = "Объект Update от Telegram Bot API"
                example("Команда /trigger от пользователя") {
                    value = TelegramUpdate(
                        updateId = 123456789,
                        message = TelegramMessage(
                            messageId = 987654321,
                            chat = Chat(
                                id = -1001234567890,
                                type = "supergroup",
                                title = "Currency Bot Test"
                            ),
                            text = "/trigger"
                        )
                    )
                }
                example("Команда /start") {
                    value = TelegramUpdate(
                        updateId = 123456790,
                        message = TelegramMessage(
                            messageId = 987654322,
                            chat = Chat(
                                id = -1001234567890,
                                type = "supergroup",
                                title = "Currency Bot Test"
                            ),
                            text = "/start"
                        )
                    )
                }
                example("Текстовое сообщение (не команда)") {
                    value = TelegramUpdate(
                        updateId = 123456791,
                        message = TelegramMessage(
                            messageId = 987654323,
                            chat = Chat(
                                id = -1001234567890,
                                type = "supergroup",
                                title = "Currency Bot Test"
                            ),
                            text = "Привет, бот!"
                        )
                    )
                }
            }
        }
        response {
            HttpStatusCode.OK to {
                description = "Webhook обработан успешно. Telegram ожидает ответ 200 OK для подтверждения обработки."
                body<String> {
                    example("Успешная обработка") {
                        value = "OK"
                    }
                }
            }
            HttpStatusCode.InternalServerError to {
                description = "Ошибка при обработке webhook. Telegram может повторить запрос."
                body<String> {
                    example("Ошибка обработки") {
                        value = "ERROR"
                    }
                }
            }
        }
    }) {
        telegramWebhookController.handleWebhook(call)
    }
}
