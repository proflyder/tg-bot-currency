package dev.proflyder.currency.scheduler

import dev.proflyder.currency.di.AppConfig
import dev.proflyder.currency.domain.usecase.SendCurrencyRatesUseCase
import dev.proflyder.currency.util.generateRequestId
import dev.proflyder.currency.util.logger
import dev.proflyder.currency.util.withLoggingContext
import kotlinx.coroutines.runBlocking
import org.quartz.Job
import org.quartz.JobExecutionContext

/**
 * Quartz Job для периодической отправки курсов валют в Telegram
 */
class CurrencyRatesJob : Job {
    private val logger = logger()

    override fun execute(context: JobExecutionContext) {
        val jobDataMap = context.jobDetail.jobDataMap
        val sendCurrencyRatesUseCase = jobDataMap.get("sendCurrencyRatesUseCase") as SendCurrencyRatesUseCase
        val config = jobDataMap.get("config") as AppConfig
        val executionCount = context.fireTime?.time ?: 0

        val requestId = generateRequestId()

        withLoggingContext(mapOf("request_id" to requestId, "execution" to executionCount.toString())) {
            try {
                logger.info("Quartz job execution started")
                val startTime = System.currentTimeMillis()

                val result = runBlocking {
                    sendCurrencyRatesUseCase(config.chatId)
                }

                val duration = System.currentTimeMillis() - startTime

                result.fold(
                    onSuccess = {
                        logger.info("Quartz job execution completed successfully in ${duration}ms")
                    },
                    onFailure = { error ->
                        logger.error(
                            "Quartz job execution failed after ${duration}ms: ${error.message}",
                            error
                        )
                    }
                )
            } catch (e: Exception) {
                logger.error("Unexpected error in Quartz job execution", e)
            }
        }
    }
}
