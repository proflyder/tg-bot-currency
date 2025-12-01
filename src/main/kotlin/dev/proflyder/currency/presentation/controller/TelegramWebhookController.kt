package dev.proflyder.currency.presentation.controller

import dev.proflyder.currency.data.dto.telegram.TelegramUpdate
import dev.proflyder.currency.domain.telegram.TelegramCommandHandler
import dev.proflyder.currency.util.logger
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Controller для обработки webhook от Telegram
 */
class TelegramWebhookController(
    private val commandHandler: TelegramCommandHandler
) {
    private val logger = logger()

    /**
     * Обработать POST запрос от Telegram webhook
     */
    suspend fun handleWebhook(call: RoutingCall) {
        try {
            val update = call.receive<TelegramUpdate>()

            logger.info("Received Telegram update ${update.updateId}")

            // Обрабатываем сообщение если оно есть
            update.message?.let { message ->
                commandHandler.handleMessage(message)
            }

            // Telegram ожидает ответ OK
            call.respond(HttpStatusCode.OK, "OK")
        } catch (e: Exception) {
            logger.error("Failed to handle Telegram webhook", e)
            call.respond(HttpStatusCode.InternalServerError, "ERROR")
        }
    }
}
