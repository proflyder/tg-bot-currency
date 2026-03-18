package dev.proflyder.currency.scheduler

import dev.proflyder.currency.di.AppConfig
import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import dev.proflyder.currency.domain.usecase.SendCurrencyRatesUseCase
import dev.proflyder.currency.util.logger
import org.quartz.CronScheduleBuilder
import org.quartz.JobBuilder
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory

/**
 * Менеджер Quartz Scheduler для управления расписанием задач
 */
class QuartzSchedulerManager(
    private val config: AppConfig,
    private val sendCurrencyRatesUseCase: SendCurrencyRatesUseCase,
    private val currencyHistoryRepository: CurrencyHistoryRepository
) {
    private val logger = logger()
    private val scheduler: Scheduler = StdSchedulerFactory.getDefaultScheduler()

    fun start() {
        logger.info("Starting Quartz Scheduler with cron: ${config.schedulerCron}")

        // Создаем JobDetail
        val jobDetail = JobBuilder.newJob(CurrencyRatesJob::class.java)
            .withIdentity("currencyRatesJob", "currency")
            .build()

        // Передаем зависимости через JobDataMap
        jobDetail.jobDataMap["sendCurrencyRatesUseCase"] = sendCurrencyRatesUseCase
        jobDetail.jobDataMap["config"] = config

        // Создаем Trigger с cron расписанием
        val trigger = TriggerBuilder.newTrigger()
            .withIdentity("currencyRatesTrigger", "currency")
            .startNow()
            .withSchedule(
                CronScheduleBuilder.cronSchedule(config.schedulerCron)
                    .withMisfireHandlingInstructionDoNothing() // Пропускаем пропущенные выполнения
            )
            .build()

        // Регистрируем Job и Trigger
        scheduler.scheduleJob(jobDetail, trigger)

        // Cleanup job — воскресенье 00:00 UTC
        val cleanupJob = JobBuilder.newJob(CleanupJob::class.java)
            .withIdentity("cleanupJob", "maintenance")
            .build()

        cleanupJob.jobDataMap["currencyHistoryRepository"] = currencyHistoryRepository

        val cleanupTrigger = TriggerBuilder.newTrigger()
            .withIdentity("cleanupTrigger", "maintenance")
            .withSchedule(
                CronScheduleBuilder.cronSchedule("0 0 0 ? * SUN")
                    .withMisfireHandlingInstructionDoNothing()
            )
            .build()

        scheduler.scheduleJob(cleanupJob, cleanupTrigger)

        // Запускаем scheduler
        scheduler.start()

        logger.info("Quartz Scheduler started successfully")
        logger.info(
            "Next execution scheduled at: ${
                scheduler.getTriggersOfJob(jobDetail.key).firstOrNull()?.nextFireTime
            }"
        )
    }

    fun stop() {
        logger.info("Stopping Quartz Scheduler")
        scheduler.shutdown(true) // Дожидаемся завершения текущих задач
        logger.info("Quartz Scheduler stopped")
    }
}
