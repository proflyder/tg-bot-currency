package dev.proflyder.currency.scheduler

import dev.proflyder.currency.di.AppConfig
import dev.proflyder.currency.domain.usecase.SendCurrencyRatesUseCase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.quartz.CronTrigger
import org.quartz.Scheduler
import org.quartz.impl.StdSchedulerFactory

@DisplayName("QuartzSchedulerManager - Интеграционные тесты")
class QuartzSchedulerManagerTest {

    private lateinit var sendCurrencyRatesUseCase: SendCurrencyRatesUseCase
    private lateinit var config: AppConfig
    private lateinit var schedulerManager: QuartzSchedulerManager
    private lateinit var quartz: Scheduler

    @BeforeEach
    fun setup() {
        sendCurrencyRatesUseCase = mockk(relaxed = true)
        config = AppConfig(
            botToken = "test-token",
            chatId = "test-chat-id",
            schedulerCron = "0 0 * * * ?", // Каждый час
            databasePath = "mem:test-db",
            unkeyRootKey = "test-unkey-root-key"
        )
    }

    @AfterEach
    fun tearDown() {
        if (::schedulerManager.isInitialized) {
            schedulerManager.stop()
        }
        if (::quartz.isInitialized && quartz.isStarted) {
            quartz.shutdown(true)
        }
        unmockkAll()
    }

    @Nested
    @DisplayName("Запуск Scheduler")
    inner class SchedulerStart {

        @Test
        fun `должен успешно запустить Quartz Scheduler`() {
            // Arrange
            schedulerManager = QuartzSchedulerManager(config, sendCurrencyRatesUseCase)

            // Act
            schedulerManager.start()

            // Assert - не должно быть исключений
            // Если дошли до этой точки, значит scheduler запустился
        }

        @Test
        fun `scheduler должен быть в состоянии started после запуска`() {
            // Arrange
            schedulerManager = QuartzSchedulerManager(config, sendCurrencyRatesUseCase)

            // Act
            schedulerManager.start()

            // Assert
            val scheduler = StdSchedulerFactory.getDefaultScheduler()
            scheduler.isStarted shouldBe true
            scheduler.isShutdown shouldBe false
        }

        @Test
        fun `должен зарегистрировать CurrencyRatesJob`() {
            // Arrange
            schedulerManager = QuartzSchedulerManager(config, sendCurrencyRatesUseCase)

            // Act
            schedulerManager.start()

            // Assert
            val scheduler = StdSchedulerFactory.getDefaultScheduler()
            val jobKey = org.quartz.JobKey.jobKey("currencyRatesJob", "currency")
            val jobDetail = scheduler.getJobDetail(jobKey)

            jobDetail shouldNotBe null
            jobDetail.jobClass shouldBe CurrencyRatesJob::class.java
        }

        @Test
        fun `должен создать trigger с правильным cron выражением`() {
            // Arrange
            schedulerManager = QuartzSchedulerManager(config, sendCurrencyRatesUseCase)

            // Act
            schedulerManager.start()

            // Assert
            val scheduler = StdSchedulerFactory.getDefaultScheduler()
            val triggerKey = org.quartz.TriggerKey.triggerKey("currencyRatesTrigger", "currency")
            val trigger = scheduler.getTrigger(triggerKey) as CronTrigger

            trigger shouldNotBe null
            trigger.cronExpression shouldBe "0 0 * * * ?"
        }

        @Test
        fun `должен передать зависимости в JobDataMap`() {
            // Arrange
            schedulerManager = QuartzSchedulerManager(config, sendCurrencyRatesUseCase)

            // Act
            schedulerManager.start()

            // Assert
            val scheduler = StdSchedulerFactory.getDefaultScheduler()
            val jobKey = org.quartz.JobKey.jobKey("currencyRatesJob", "currency")
            val jobDetail = scheduler.getJobDetail(jobKey)

            val jobDataMap = jobDetail.jobDataMap
            jobDataMap["sendCurrencyRatesUseCase"] shouldBe sendCurrencyRatesUseCase
            jobDataMap["config"] shouldBe config
        }

        @Test
        fun `должен запланировать следующее выполнение`() {
            // Arrange
            schedulerManager = QuartzSchedulerManager(config, sendCurrencyRatesUseCase)

            // Act
            schedulerManager.start()

            // Assert
            val scheduler = StdSchedulerFactory.getDefaultScheduler()
            val triggerKey = org.quartz.TriggerKey.triggerKey("currencyRatesTrigger", "currency")
            val trigger = scheduler.getTrigger(triggerKey)

            trigger.nextFireTime shouldNotBe null
        }
    }

    @Nested
    @DisplayName("Остановка Scheduler")
    inner class SchedulerStop {

        @Test
        fun `должен успешно остановить scheduler`() {
            // Arrange
            schedulerManager = QuartzSchedulerManager(config, sendCurrencyRatesUseCase)
            schedulerManager.start()
            val scheduler = StdSchedulerFactory.getDefaultScheduler()

            // Act
            schedulerManager.stop()

            // Assert - не должно быть исключений
            scheduler.isShutdown shouldBe true
        }

        @Test
        fun `должен дождаться завершения текущих задач при остановке`() {
            // Arrange
            schedulerManager = QuartzSchedulerManager(config, sendCurrencyRatesUseCase)
            schedulerManager.start()

            // Act
            val startTime = System.currentTimeMillis()
            schedulerManager.stop()
            val duration = System.currentTimeMillis() - startTime

            // Assert - остановка должна быть быстрой так как нет выполняющихся задач
            (duration < 5000) shouldBe true // Меньше 5 секунд
        }

        @Test
        fun `должен корректно обработать повторный вызов stop`() {
            // Arrange
            schedulerManager = QuartzSchedulerManager(config, sendCurrencyRatesUseCase)
            schedulerManager.start()

            // Act & Assert - не должно быть исключений
            schedulerManager.stop()
            schedulerManager.stop() // Второй вызов не должен падать
        }
    }

    @Nested
    @DisplayName("Конфигурация Cron")
    inner class CronConfiguration {

        @Test
        fun `должен поддерживать разные cron выражения`() {
            // Arrange - каждые 2 часа
            val customConfig = config.copy(schedulerCron = "0 0 */2 * * ?")
            schedulerManager = QuartzSchedulerManager(customConfig, sendCurrencyRatesUseCase)

            // Act
            schedulerManager.start()

            // Assert
            val scheduler = StdSchedulerFactory.getDefaultScheduler()
            val triggerKey = org.quartz.TriggerKey.triggerKey("currencyRatesTrigger", "currency")
            val trigger = scheduler.getTrigger(triggerKey) as CronTrigger

            trigger.cronExpression shouldBe "0 0 */2 * * ?"
        }

        @Test
        fun `должен корректно работать с cron для запуска раз в день`() {
            // Arrange - каждый день в 9:00
            val customConfig = config.copy(schedulerCron = "0 0 9 * * ?")
            schedulerManager = QuartzSchedulerManager(customConfig, sendCurrencyRatesUseCase)

            // Act
            schedulerManager.start()

            // Assert
            val scheduler = StdSchedulerFactory.getDefaultScheduler()
            val triggerKey = org.quartz.TriggerKey.triggerKey("currencyRatesTrigger", "currency")
            val trigger = scheduler.getTrigger(triggerKey) as CronTrigger

            trigger.cronExpression shouldBe "0 0 9 * * ?"
        }

        @Test
        fun `должен корректно работать с cron для запуска раз в минуту (для тестов)`() {
            // Arrange - каждую минуту
            val customConfig = config.copy(schedulerCron = "0 * * * * ?")
            schedulerManager = QuartzSchedulerManager(customConfig, sendCurrencyRatesUseCase)

            // Act
            schedulerManager.start()

            // Assert
            val scheduler = StdSchedulerFactory.getDefaultScheduler()
            val triggerKey = org.quartz.TriggerKey.triggerKey("currencyRatesTrigger", "currency")
            val trigger = scheduler.getTrigger(triggerKey) as CronTrigger

            trigger.cronExpression shouldBe "0 * * * * ?"
        }
    }

    @Nested
    @DisplayName("Жизненный цикл")
    inner class Lifecycle {

        @Test
        fun `должен поддерживать полный цикл start-stop-start`() {
            // Arrange
            schedulerManager = QuartzSchedulerManager(config, sendCurrencyRatesUseCase)

            // Act & Assert
            schedulerManager.start()
            var scheduler = StdSchedulerFactory.getDefaultScheduler()
            scheduler.isStarted shouldBe true

            schedulerManager.stop()
            scheduler.isShutdown shouldBe true

            // После shutdown нужно создать новый instance
            schedulerManager = QuartzSchedulerManager(config, sendCurrencyRatesUseCase)
            schedulerManager.start()
            scheduler = StdSchedulerFactory.getDefaultScheduler()
            scheduler.isStarted shouldBe true
        }
    }

    @Nested
    @DisplayName("Misfire поведение")
    inner class MisfireBehavior {

        @Test
        fun `должен использовать misfire policy DoNothing`() {
            // Arrange
            schedulerManager = QuartzSchedulerManager(config, sendCurrencyRatesUseCase)

            // Act
            schedulerManager.start()

            // Assert
            val scheduler = StdSchedulerFactory.getDefaultScheduler()
            val triggerKey = org.quartz.TriggerKey.triggerKey("currencyRatesTrigger", "currency")
            val trigger = scheduler.getTrigger(triggerKey) as CronTrigger

            // CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING = 2
            trigger.misfireInstruction shouldBe 2
        }
    }
}
