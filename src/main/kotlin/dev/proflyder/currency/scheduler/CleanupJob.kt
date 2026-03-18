package dev.proflyder.currency.scheduler

import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import dev.proflyder.currency.util.logger
import kotlinx.coroutines.runBlocking
import org.quartz.Job
import org.quartz.JobExecutionContext

/**
 * Quartz Job для еженедельной очистки старых записей из БД.
 * Запускается по воскресеньям в 00:00 UTC.
 */
class CleanupJob : Job {
    private val logger = logger()

    override fun execute(context: JobExecutionContext) {
        val jobDataMap = context.jobDetail.jobDataMap
        val historyRepository = jobDataMap["currencyHistoryRepository"] as CurrencyHistoryRepository

        try {
            logger.info("Cleanup job started")
            val startTime = System.currentTimeMillis()

            val result = runBlocking {
                historyRepository.cleanOldRecords(olderThanDays = 35)
            }

            val duration = System.currentTimeMillis() - startTime

            result.fold(
                onSuccess = { removedCount ->
                    logger.info("Cleanup job completed in ${duration}ms: removed $removedCount old records")
                },
                onFailure = { error ->
                    logger.error("Cleanup job failed after ${duration}ms: ${error.message}", error)
                }
            )
        } catch (e: Exception) {
            logger.error("Unexpected error in cleanup job", e)
        }
    }
}
