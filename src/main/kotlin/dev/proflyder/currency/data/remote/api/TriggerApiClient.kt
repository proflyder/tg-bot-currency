package dev.proflyder.currency.data.remote.api

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
     */
    suspend fun triggerCurrencyUpdate(): Result<TriggerResponseDto> {
        return try {
            logger.info("Calling POST /api/trigger, API key: $maskedApiKey")

            val response: TriggerResponseDto = httpClient.post("$baseUrl/api/trigger") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
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
