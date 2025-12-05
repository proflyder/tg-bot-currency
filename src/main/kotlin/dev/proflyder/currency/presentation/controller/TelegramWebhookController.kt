package dev.proflyder.currency.presentation.controller

import dev.proflyder.currency.data.dto.telegram.TelegramUpdate
import dev.proflyder.currency.domain.telegram.TelegramCommandHandler
import dev.proflyder.currency.util.logger
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

/**
 * Controller для обработки webhook от Telegram
 */
class TelegramWebhookController(
    private val commandHandler: TelegramCommandHandler
) {
    private val logger = logger()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Обработать POST запрос от Telegram webhook
     */
    suspend fun handleWebhook(call: RoutingCall) {
        try {
            val rawBody = call.receiveText()
            val update = json.decodeFromString<TelegramUpdate>(rawBody)

            val messageToHandle = update.message
                ?: update.editedMessage
                ?: update.channelPost
                ?: update.editedChannelPost

            messageToHandle?.let { message ->
                logger.info("Processing Telegram message from ${message.chat.id}: ${message.text}")
                commandHandler.handleMessage(message)
            } ?: run {
                logger.debug("Received non-message update: ${update.updateId}")
            }

            call.respond(HttpStatusCode.OK, "OK")
        } catch (e: Exception) {
            logger.error("Failed to handle Telegram webhook", e)
            call.respond(HttpStatusCode.InternalServerError, "ERROR")
        }
    }
}
