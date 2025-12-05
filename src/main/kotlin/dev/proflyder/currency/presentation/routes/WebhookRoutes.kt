package dev.proflyder.currency.presentation.routes

import dev.proflyder.currency.presentation.controller.TelegramWebhookController
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.*
import io.ktor.server.routing.*

fun Route.webhookRoutes(telegramWebhookController: TelegramWebhookController) {
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
}
