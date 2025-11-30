package dev.proflyder.currency.data.repository

import dev.proflyder.currency.data.dto.telegram.SendMessageRequest
import dev.proflyder.currency.data.remote.telegram.TelegramApi
import dev.proflyder.currency.domain.repository.TelegramRepository

class TelegramRepositoryImpl(
    private val telegramApi: TelegramApi
) : TelegramRepository {
    override suspend fun sendMessage(chatId: String, message: String): Result<Unit> {
        val request = SendMessageRequest(
            chatId = chatId,
            text = message,
            parseMode = "Markdown"
        )

        return telegramApi.sendMessage(request).map { }
    }
}
