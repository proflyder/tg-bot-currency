package dev.proflyder.currency.data.remote.telegram

import dev.proflyder.currency.data.dto.telegram.*
import dev.proflyder.currency.util.logger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class TelegramApi(
    private val httpClient: HttpClient,
    private val botToken: String
) {
    private val baseUrl = "https://api.telegram.org/bot$botToken"
    private val logger = logger()
    private val maskedToken = botToken.take(8) + "***" // Показываем только первые 8 символов

    suspend fun sendMessage(request: SendMessageRequest): Result<TelegramResponse<Message>> {
        return try {
            logger.debug("Sending message to Telegram chat ${request.chatId}, token: $maskedToken")

            val response: TelegramResponse<Message> = httpClient.post("$baseUrl/sendMessage") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

            if (response.ok) {
                logger.info("Successfully sent message to chat ${request.chatId}, message_id: ${response.result?.messageId}")
                Result.success(response)
            } else {
                logger.error("Telegram API error: ${response.description}")
                Result.failure(Exception("Telegram API error: ${response.description}"))
            }
        } catch (e: Exception) {
            logger.error("Failed to send message to Telegram", e)
            Result.failure(e)
        }
    }

    suspend fun getUpdates(): Result<List<TelegramUpdate>> {
        return try {
            logger.debug("Fetching updates from Telegram, token: $maskedToken")

            val response: UpdatesResponse = httpClient.get("$baseUrl/getUpdates") {
                parameter("limit", 10)
                parameter("offset", -10)
            }.body()

            if (response.ok) {
                val chatIds = response.result

                logger.info("Found ${chatIds.size} unique chat(s) in recent updates")
                Result.success(chatIds)
            } else {
                logger.error("Telegram API error: ${response.description}")
                Result.failure(Exception("Telegram API error: ${response.description}"))
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch updates from Telegram", e)
            Result.failure(e)
        }
    }
}
