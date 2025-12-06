package dev.proflyder.currency.presentation.controller

import dev.proflyder.currency.data.dto.*
import dev.proflyder.currency.domain.usecase.DeleteCurrencyHistoryUseCase
import dev.proflyder.currency.domain.usecase.GetCurrencyHistoryUseCase
import dev.proflyder.currency.domain.usecase.GetLatestCurrencyRateUseCase
import dev.proflyder.currency.presentation.exception.DatabaseException
import dev.proflyder.currency.presentation.exception.NotFoundException
import dev.proflyder.currency.util.logger
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Controller для обработки HTTP запросов связанных с историей курсов валют
 *
 * Использует exceptions для обработки ошибок вместо Result.fold.
 * Все исключения обрабатываются глобальным exception handler.
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
        val records = getCurrencyHistoryUseCase().getOrElse { error ->
            logger.error("Failed to fetch currency history", error)
            throw DatabaseException(
                message = "Failed to fetch currency history: ${error.message}",
                service = "CurrencyHistoryController.getHistory",
                cause = error
            )
        }

        logger.info("Successfully fetched ${records.size} records")
        call.respond(
            HttpStatusCode.OK,
            CurrencyHistoryResponseDto(
                records = records.map { it.toDto() },
                totalCount = records.size
            )
        )
    }

    /**
     * Обработать GET запрос для получения последнего актуального курса
     */
    suspend fun getLatest(call: RoutingCall) {
        val record = getLatestCurrencyRateUseCase().getOrElse { error ->
            logger.error("Failed to fetch latest currency rate", error)
            throw DatabaseException(
                message = "Failed to fetch latest currency rate: ${error.message}",
                service = "CurrencyHistoryController.getLatest",
                cause = error
            )
        }

        if (record == null) {
            logger.warn("No currency rates found in database")
            throw NotFoundException(
                message = "No currency rates found in database",
                service = "CurrencyHistoryController.getLatest"
            )
        }

        logger.info("Successfully fetched latest rate: ${record.timestamp}")
        call.respond(
            HttpStatusCode.OK,
            LatestCurrencyRateResponseDto(
                timestamp = record.timestamp,
                rates = CurrencyRatesDto(
                    usdToKzt = ExchangeRateDto(
                        buy = record.rates.usdToKzt.buy,
                        sell = record.rates.usdToKzt.sell
                    ),
                    rubToKzt = ExchangeRateDto(
                        buy = record.rates.rubToKzt.buy,
                        sell = record.rates.rubToKzt.sell
                    )
                )
            )
        )
    }

    /**
     * Обработать DELETE запрос для удаления всей истории курсов валют
     */
    suspend fun deleteHistory(call: RoutingCall) {
        val deletedCount = deleteCurrencyHistoryUseCase().getOrElse { error ->
            logger.error("Failed to delete currency history", error)
            throw DatabaseException(
                message = "Failed to delete currency history: ${error.message}",
                service = "CurrencyHistoryController.deleteHistory",
                cause = error
            )
        }

        logger.info("Successfully deleted $deletedCount records")
        call.respond(
            HttpStatusCode.OK,
            DeleteHistoryResponseDto(
                deletedCount = deletedCount,
                message = "Successfully deleted $deletedCount currency history records"
            )
        )
    }
}
