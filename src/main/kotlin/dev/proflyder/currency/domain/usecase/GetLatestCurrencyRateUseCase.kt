package dev.proflyder.currency.domain.usecase

import dev.proflyder.currency.domain.model.CurrencyRateRecord
import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import dev.proflyder.currency.util.logger

/**
 * Use case для получения последнего актуального курса валют
 */
class GetLatestCurrencyRateUseCase(
    private val currencyHistoryRepository: CurrencyHistoryRepository
) {
    private val logger = logger()

    /**
     * Получить последнюю запись курсов валют
     * @return Последняя запись или null если история пуста
     */
    suspend operator fun invoke(): Result<CurrencyRateRecord?> {
        logger.info("Fetching latest currency rate...")
        return currencyHistoryRepository.getLatestRecord()
    }
}
