package dev.proflyder.currency.data.remote.api

import dev.proflyder.currency.data.dto.TriggerRequestDto
import dev.proflyder.currency.data.dto.TriggerResponseDto
import dev.proflyder.currency.util.logger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * HTTP клиент для вызова внутреннего API endpoint POST /api/trigger
 */
class TriggerApiClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val baseUrl: String = "http://localhost:8080"
) {
    private val logger = logger()
    private val maskedApiKey = apiKey.take(8) + "***"

    /**
     * Вызвать endpoint POST /api/trigger для принудительного обновления курсов
     *
     * @param chatId ID чата Telegram, куда отправить сообщение. Если null, используется chatId из конфигурации.
     */
    suspend fun triggerCurrencyUpdate(chatId: String? = null): Result<TriggerResponseDto> {
        return try {
            val logMessage = if (chatId != null) {
                "Calling POST /api/trigger for chat $chatId, API key: $maskedApiKey"
            } else {
                "Calling POST /api/trigger (default chat), API key: $maskedApiKey"
            }
            logger.info(logMessage)

            val response: TriggerResponseDto = httpClient.post("$baseUrl/api/trigger") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(TriggerRequestDto(chatId = chatId))
            }.body()

            if (response.success) {
                logger.info("Successfully triggered currency update: ${response.message}")
                Result.success(response)
            } else {
                logger.error("Failed to trigger currency update: ${response.message}")
                Result.failure(Exception("API error: ${response.message}"))
            }
        } catch (e: Exception) {
            logger.error("Failed to call POST /api/trigger", e)
            Result.failure(e)
        }
    }
}
