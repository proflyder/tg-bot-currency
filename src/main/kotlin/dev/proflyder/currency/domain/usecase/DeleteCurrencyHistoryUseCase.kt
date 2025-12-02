package dev.proflyder.currency.domain.usecase

import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import dev.proflyder.currency.util.logger

/**
 * UseCase для удаления всей истории курсов валют из базы данных
 */
class DeleteCurrencyHistoryUseCase(
    private val currencyHistoryRepository: CurrencyHistoryRepository
) {
    private val logger = logger()

    /**
     * Удаляет все записи истории курсов валют из базы данных
     * @return Result с количеством удаленных записей или ошибкой
     */
    suspend operator fun invoke(): Result<Int> {
        logger.info("Deleting all currency history records...")

        return currencyHistoryRepository.deleteAll().also { result ->
            result.fold(
                onSuccess = { deletedCount ->
                    logger.info("Successfully deleted $deletedCount history records")
                },
                onFailure = { error ->
                    logger.error("Failed to delete currency history", error)
                }
            )
        }
    }
}
