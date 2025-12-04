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
            // Читаем RAW body как текст
            val rawBody = call.receiveText()

            logger.info("=== RAW Telegram webhook request ===")
            logger.info("URI: ${call.request.uri}")
            logger.info("Headers: ${call.request.headers.entries().joinToString { "${it.key}: ${it.value}" }}")
            logger.info("Body: $rawBody")
            logger.info("====================================")

            // Десериализуем вручную из прочитанного текста
            val update = json.decodeFromString<TelegramUpdate>(rawBody)

            logger.info("Received Telegram update ${update.updateId}")
            logger.info("Telegram webhook body: $update")

            // Обрабатываем все типы сообщений с текстом
            val messageToHandle = update.message
                ?: update.editedMessage
                ?: update.channelPost
                ?: update.editedChannelPost

            messageToHandle?.let { message ->
                val messageType = when {
                    update.editedMessage != null -> "edited message"
                    update.channelPost != null -> "channel post"
                    update.editedChannelPost != null -> "edited channel post"
                    else -> "message"
                }
                logger.info("Processing $messageType from ${message.chat.id} (${message.chat.title ?: "private"}): ${message.text}")
                commandHandler.handleMessage(message)
            } ?: run {
                // Если это другой тип обновления (callback, inline query и т.д.)
                logger.debug("Received non-message update: ${update.updateId}")
            }

            // Telegram ожидает ответ OK
            call.respond(HttpStatusCode.OK, "OK")
        } catch (e: Exception) {
            logger.error("Failed to handle Telegram webhook", e)
            call.respond(HttpStatusCode.InternalServerError, "ERROR")
        }
    }
}
