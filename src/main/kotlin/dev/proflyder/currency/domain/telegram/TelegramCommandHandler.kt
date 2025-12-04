package dev.proflyder.currency.domain.telegram

import dev.proflyder.currency.data.dto.telegram.SendMessageRequest
import dev.proflyder.currency.data.dto.telegram.TelegramMessage
import dev.proflyder.currency.data.remote.api.TriggerApiClient
import dev.proflyder.currency.data.remote.telegram.TelegramApi
import dev.proflyder.currency.util.logger

/**
 * Обработчик команд Telegram бота
 */
class TelegramCommandHandler(
    private val telegramApi: TelegramApi,
    private val triggerApiClient: TriggerApiClient
) {
    private val logger = logger()

    /**
     * Обработать сообщение от Telegram
     */
    suspend fun handleMessage(message: TelegramMessage) {
        val text = message.text?.trim() ?: return
        val chatId = message.chat.id.toString()

        logger.info("Received message from chat $chatId: $text")

        when {
            text.startsWith("/trigger") -> handleTriggerUpdateCommand(chatId, "/trigger")
            text.startsWith("/start") -> handleStartCommand(chatId)
            text.startsWith("/help") -> handleHelpCommand(chatId)
            else -> {
                logger.debug("Ignoring non-command message: $text")
            }
        }
    }

    /**
     * Обработать команду /trigger - принудительно обновить курсы
     */
    private suspend fun handleTriggerUpdateCommand(chatId: String, command: String) {
        logger.info("Handling $command command for chat $chatId")

        // Вызываем внутренний API endpoint для обновления курсов, передавая chatId
        triggerApiClient.triggerCurrencyUpdate(chatId = chatId).fold(
            onSuccess = { response ->
                logger.info("Successfully triggered currency update via $command command: ${response.message}")
            },
            onFailure = { error ->
                logger.error("Failed to trigger currency update via $command command", error)
                // Отправляем сообщение об ошибке в тот же чат
                telegramApi.sendMessage(
                    SendMessageRequest(
                        chatId = chatId,
                        text = "❌ Не удалось обновить курсы: ${error.message}",
                        parseMode = "HTML"
                    )
                )
            }
        )
    }

    /**
     * Обработать команду /start - приветственное сообщение
     */
    private suspend fun handleStartCommand(chatId: String) {
        logger.info("Handling /start command for chat $chatId")

        val message = """
            👋 Привет! Я бот для отслеживания курсов валют.

            Доступные команды:
            /trigger - Обновить курсы валют
            /help - Показать справку
        """.trimIndent()

        telegramApi.sendMessage(
            SendMessageRequest(
                chatId = chatId,
                text = message,
                parseMode = "HTML"
            )
        )
    }

    /**
     * Обработать команду /help - справка
     */
    private suspend fun handleHelpCommand(chatId: String) {
        logger.info("Handling /help command for chat $chatId")

        val message = """
            📖 Справка по командам бота:

            /trigger - Принудительно обновить и получить актуальные курсы USD→KZT и RUB→KZT
            /start - Показать приветственное сообщение
            /help - Показать эту справку

            ℹ️ Бот автоматически отправляет курсы валют каждый час.
        """.trimIndent()

        telegramApi.sendMessage(
            SendMessageRequest(
                chatId = chatId,
                text = message,
                parseMode = "HTML"
            )
        )
    }
}
