package dev.proflyder.currency.domain.repository

interface TelegramRepository {
    suspend fun sendMessage(chatId: String, message: String): Result<Unit>
}
