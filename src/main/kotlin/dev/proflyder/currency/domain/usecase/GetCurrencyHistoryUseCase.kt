package dev.proflyder.currency.domain.usecase

import dev.proflyder.currency.domain.model.CurrencyRateRecord
import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import dev.proflyder.currency.util.logger

/**
 * UseCase для получения полной истории курсов валют
 */
class GetCurrencyHistoryUseCase(
    private val currencyHistoryRepository: CurrencyHistoryRepository
) {
    private val logger = logger()

    /**
     * Получает полную историю курсов валют
     * @return Список всех записей истории, отсортированный по времени (от новых к старым)
     */
    suspend operator fun invoke(): Result<List<CurrencyRateRecord>> {
        logger.info("Fetching currency history...")

        return currencyHistoryRepository.getAllRecords().also { result ->
            result.fold(
                onSuccess = { records ->
                    logger.info("Successfully fetched ${records.size} history records")
                },
                onFailure = { error ->
                    logger.error("Failed to fetch currency history", error)
                }
            )
        }
    }
}
