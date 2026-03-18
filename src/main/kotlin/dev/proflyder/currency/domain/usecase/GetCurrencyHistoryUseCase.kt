package dev.proflyder.currency.domain.usecase

import dev.proflyder.currency.domain.model.CurrencyRateRecord
import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import dev.proflyder.currency.util.logger
import kotlinx.datetime.Instant

/**
 * UseCase для получения истории курсов валют
 */
class GetCurrencyHistoryUseCase(
    private val currencyHistoryRepository: CurrencyHistoryRepository
) {
    private val logger = logger()

    /**
     * Получает историю курсов валют (все или за указанный интервал)
     * @param from Начало интервала (опционально)
     * @param to Конец интервала (опционально)
     * @return Список записей, отсортированный по времени (от новых к старым)
     */
    suspend operator fun invoke(from: Instant? = null, to: Instant? = null): Result<List<CurrencyRateRecord>> {
        if (from != null && to != null) {
            logger.info("Fetching currency history from $from to $to")
            return currencyHistoryRepository.getRecordsByDateRange(from, to)
        }

        logger.info("Fetching full currency history...")
        return currencyHistoryRepository.getAllRecords()
    }
}
