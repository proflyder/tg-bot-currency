package dev.proflyder.currency.data.remote.unkey

import dev.proflyder.currency.util.logger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Клиент для работы с Unkey API
 * Документация: https://www.unkey.com/docs/api-reference/keys/verify
 */
class UnkeyClient(
    private val httpClient: HttpClient,
    private val rootKey: String
) {
    private val logger = logger()

    companion object {
        private const val UNKEY_API_URL = "https://api.unkey.com/v2/keys.verifyKey"
    }

    /**
     * Проверить валидность API ключа через Unkey
     * @param apiKey API ключ для проверки
     * @return true если ключ валидный, false если невалидный или произошла ошибка
     */
    suspend fun verifyKey(apiKey: String): Result<UnkeyVerifyResponse> = runCatching {
        logger.debug("Verifying API key with Unkey")

        val response = httpClient.post(UNKEY_API_URL) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $rootKey")
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
            setBody(UnkeyVerifyRequest(key = apiKey))
        }

        if (response.status == HttpStatusCode.OK) {
            val verifyResponse = response.body<UnkeyVerifyResponse>()
            if (verifyResponse.data.valid) {
                logger.info("API key verified successfully")
            } else {
                logger.warn("API key verification failed: ${verifyResponse.data.code}")
            }
            verifyResponse
        } else {
            logger.error("Unkey API returned status: ${response.status}")
            throw Exception("Unkey API error: ${response.status}")
        }
    }
}
