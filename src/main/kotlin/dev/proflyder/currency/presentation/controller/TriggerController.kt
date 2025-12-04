package dev.proflyder.currency.presentation.controller

import dev.proflyder.currency.data.dto.TriggerRequestDto
import dev.proflyder.currency.data.dto.TriggerResponseDto
import dev.proflyder.currency.di.AppConfig
import dev.proflyder.currency.domain.usecase.SendCurrencyRatesUseCase
import dev.proflyder.currency.util.logger
import dev.proflyder.currency.util.withLoggingContext
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

/**
 * Controller для обработки HTTP запросов принудительного запуска задач
 */
class TriggerController(
    private val sendCurrencyRatesUseCase: SendCurrencyRatesUseCase,
    private val config: AppConfig
) {
    private val logger = logger()

    /**
     * Обработать POST запрос для принудительного запуска обновления курсов
     */
    suspend fun triggerCurrencyUpdate(call: RoutingCall) {
        withLoggingContext(mapOf("request_id" to UUID.randomUUID().toString())) {
            // Читаем тело запроса (может быть пустым или содержать chatId)
            val request = try {
                call.receive<TriggerRequestDto>()
            } catch (e: Exception) {
                // Если не удалось распарсить body, используем пустой request
                TriggerRequestDto()
            }

            // Используем chatId из запроса, если он есть, иначе из конфигурации
            val targetChatId = request.chatId ?: config.chatId

            logger.info("POST /api/trigger - Manual trigger for currency update to chat $targetChatId")

            val startTime = System.currentTimeMillis()

            sendCurrencyRatesUseCase(
                chatId = targetChatId,
                forceNotification = true,
                saveToHistory = false  // Не сохраняем при ручном триггере
            ).fold(
                onSuccess = {
                    val duration = System.currentTimeMillis() - startTime
                    logger.info("Currency update triggered successfully in ${duration}ms")
                    call.respond(
                        HttpStatusCode.OK,
                        TriggerResponseDto(
                            success = true,
                            message = "Currency rates updated and sent to Telegram successfully"
                        )
                    )
                },
                onFailure = { error ->
                    val duration = System.currentTimeMillis() - startTime
                    logger.error("Failed to trigger currency update after ${duration}ms", error)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        TriggerResponseDto(
                            success = false,
                            message = "Failed to update currency rates: ${error.message}"
                        )
                    )
                }
            )
        }
    }
}
