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

    suspend fun sendMessage(request: SendMessageRequest): Result<TelegramResponse<Message>> {
        return try {
            val response: TelegramResponse<Message> = httpClient.post("$baseUrl/sendMessage") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

            if (response.ok) {
                logger.info("Successfully sent message to chat ${request.chatId}, message_id: ${response.result?.messageId}")
                Result.success(response)
            } else {
                Result.failure(Exception("Telegram API error: ${response.description}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUpdates(): Result<List<TelegramUpdate>> {
        return try {
            val response: UpdatesResponse = httpClient.get("$baseUrl/getUpdates") {
                parameter("limit", 10)
                parameter("offset", -10)
            }.body()

            if (response.ok) {
                logger.info("Found ${response.result.size} unique chat(s) in recent updates")
                Result.success(response.result)
            } else {
                Result.failure(Exception("Telegram API error: ${response.description}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
