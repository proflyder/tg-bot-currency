package dev.proflyder.currency.presentation.controller

import dev.proflyder.currency.data.dto.CurrencyHistoryDataDto
import dev.proflyder.currency.data.dto.CurrencyHistoryResponseDto
import dev.proflyder.currency.data.dto.LatestCurrencyRateResponseDto
import dev.proflyder.currency.data.dto.toDto
import dev.proflyder.currency.domain.usecase.GetCurrencyHistoryUseCase
import dev.proflyder.currency.domain.usecase.GetLatestCurrencyRateUseCase
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
    private val getCurrencyHistoryUseCase: GetCurrencyHistoryUseCase,
    private val getLatestCurrencyRateUseCase: GetLatestCurrencyRateUseCase
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

    /**
     * Обработать GET запрос для получения последнего актуального курса
     */
    suspend fun getLatest(call: RoutingCall) {
        withLoggingContext(mapOf("request_id" to UUID.randomUUID().toString())) {
            logger.info("GET /api/latest - Fetching latest currency rate")

            getLatestCurrencyRateUseCase().fold(
                onSuccess = { record ->
                    if (record != null) {
                        logger.info("Successfully fetched latest rate: ${record.timestamp}")
                        call.respond(
                            HttpStatusCode.OK,
                            LatestCurrencyRateResponseDto(
                                success = true,
                                data = record.toDto(),
                                message = "Latest currency rate fetched successfully"
                            )
                        )
                    } else {
                        logger.warn("No currency rates found in database")
                        call.respond(
                            HttpStatusCode.NotFound,
                            LatestCurrencyRateResponseDto(
                                success = false,
                                data = null,
                                message = "No currency rates found"
                            )
                        )
                    }
                },
                onFailure = { error ->
                    logger.error("Failed to fetch latest currency rate", error)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        LatestCurrencyRateResponseDto(
                            success = false,
                            data = null,
                            message = "Failed to fetch latest currency rate: ${error.message}"
                        )
                    )
                }
            )
        }
    }
}
