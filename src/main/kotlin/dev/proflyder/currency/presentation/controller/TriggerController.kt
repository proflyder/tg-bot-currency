package dev.proflyder.currency.presentation.controller

import dev.proflyder.currency.data.dto.TriggerRequestDto
import dev.proflyder.currency.data.dto.TriggerResponseDto
import dev.proflyder.currency.di.AppConfig
import dev.proflyder.currency.domain.usecase.SendCurrencyRatesUseCase
import dev.proflyder.currency.presentation.exception.ExternalServiceException
import dev.proflyder.currency.util.logger
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Controller для обработки HTTP запросов принудительного запуска задач
 *
 * Использует exceptions для обработки ошибок вместо Result.fold.
 * Все исключения обрабатываются глобальным exception handler.
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
        val request = try {
            call.receive<TriggerRequestDto>()
        } catch (e: Exception) {
            TriggerRequestDto()
        }

        val targetChatId = request.chatId ?: config.chatId
        val startTime = System.currentTimeMillis()

        sendCurrencyRatesUseCase(
            chatId = targetChatId,
            forceNotification = true,
            saveToHistory = false
        ).getOrElse { error ->
            val duration = System.currentTimeMillis() - startTime
            logger.error("Failed to trigger currency update after ${duration}ms", error)
            throw ExternalServiceException(
                message = "Failed to update and send currency rates: ${error.message}",
                service = "TriggerController.triggerCurrencyUpdate",
                details = mapOf(
                    "chatId" to targetChatId,
                    "executionTimeMs" to duration.toString()
                ),
                cause = error
            )
        }

        val duration = System.currentTimeMillis() - startTime
        logger.info("Currency update triggered successfully in ${duration}ms")

        call.respond(
            HttpStatusCode.OK,
            TriggerResponseDto(
                message = "Currency rates updated and sent to Telegram successfully",
                executionTimeMs = duration
            )
        )
    }
}
