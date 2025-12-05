package dev.proflyder.currency.presentation.controller

import dev.proflyder.currency.data.dto.*
import dev.proflyder.currency.domain.usecase.DeleteCurrencyHistoryUseCase
import dev.proflyder.currency.domain.usecase.GetCurrencyHistoryUseCase
import dev.proflyder.currency.domain.usecase.GetLatestCurrencyRateUseCase
import dev.proflyder.currency.util.logger
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Controller для обработки HTTP запросов связанных с историей курсов валют
 */
class CurrencyHistoryController(
    private val getCurrencyHistoryUseCase: GetCurrencyHistoryUseCase,
    private val getLatestCurrencyRateUseCase: GetLatestCurrencyRateUseCase,
    private val deleteCurrencyHistoryUseCase: DeleteCurrencyHistoryUseCase
) {
    private val logger = logger()

    /**
     * Обработать GET запрос для получения истории курсов
     */
    suspend fun getHistory(call: RoutingCall) {
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

    /**
     * Обработать GET запрос для получения последнего актуального курса
     */
    suspend fun getLatest(call: RoutingCall) {
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

    /**
     * Обработать DELETE запрос для удаления всей истории курсов валют
     */
    suspend fun deleteHistory(call: RoutingCall) {
        deleteCurrencyHistoryUseCase().fold(
            onSuccess = { deletedCount ->
                logger.info("Successfully deleted $deletedCount records")
                call.respond(
                    HttpStatusCode.OK,
                    DeleteHistoryResponseDto(
                        success = true,
                        message = "Successfully deleted $deletedCount currency history records",
                        deletedCount = deletedCount
                    )
                )
            },
            onFailure = { error ->
                logger.error("Failed to delete currency history", error)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    DeleteHistoryResponseDto(
                        success = false,
                        message = "Failed to delete currency history: ${error.message}"
                    )
                )
            }
        )
    }
}
