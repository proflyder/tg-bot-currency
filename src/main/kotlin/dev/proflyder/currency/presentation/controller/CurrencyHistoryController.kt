package dev.proflyder.currency.presentation.controller

import dev.proflyder.currency.data.dto.CurrencyHistoryDataDto
import dev.proflyder.currency.data.dto.CurrencyHistoryResponseDto
import dev.proflyder.currency.data.dto.toDto
import dev.proflyder.currency.domain.usecase.GetCurrencyHistoryUseCase
import dev.proflyder.currency.util.logger
import dev.proflyder.currency.util.withLoggingContext
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

/**
 * Controller для обработки HTTP запросов связанных с историей курсов валют
 */
class CurrencyHistoryController(
    private val getCurrencyHistoryUseCase: GetCurrencyHistoryUseCase
) {
    private val logger = logger()

    /**
     * Обработать GET запрос для получения истории курсов
     */
    suspend fun getHistory(call: RoutingCall) {
        withLoggingContext(mapOf("request_id" to UUID.randomUUID().toString())) {
            logger.info("GET /api/history - Fetching currency history")

            getCurrencyHistoryUseCase().fold(
                onSuccess = { records ->
                    logger.info("Successfully fetched ${records.size} records")
                    call.respond(
                        HttpStatusCode.OK,
                        CurrencyHistoryResponseDto(
                            success = true,
                            data = CurrencyHistoryDataDto(
                                records = records.map { it.toDto() },
                                totalCount = records.size
                            ),
                            message = "Currency history fetched successfully"
                        )
                    )
                },
                onFailure = { error ->
                    logger.error("Failed to fetch currency history", error)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        CurrencyHistoryResponseDto(
                            success = false,
                            data = CurrencyHistoryDataDto(
                                records = emptyList(),
                                totalCount = 0
                            ),
                            message = "Failed to fetch currency history: ${error.message}"
                        )
                    )
                }
            )
        }
    }
}
