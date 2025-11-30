package dev.proflyder.currency

import dev.proflyder.currency.di.AppConfig
import dev.proflyder.currency.di.appModule
import dev.proflyder.currency.scheduler.QuartzSchedulerManager
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.server.application.*
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    // КРИТИЧНО: загружаем .env ДО запуска Ktor,
    // чтобы переменные были доступны при чтении application.yaml
    loadDotEnv()

    io.ktor.server.cio.EngineMain.main(args)
}

fun Application.module() {

    // Создаем конфигурацию из application.yaml
    val config = AppConfig(
        botToken = environment.config.property("bot.token").getString(),
        chatId = environment.config.property("bot.chatId").getString(),
        schedulerCron = environment.config.property("scheduler.cron").getString(),
        databasePath = environment.config.property("database.path").getString()
    )

    // Настраиваем Koin
    install(Koin) {
        slf4jLogger()
        modules(
            module {
                single { config }
            },
            appModule
        )
    }

    // Получаем зависимости через Koin
    val scheduler = get<QuartzSchedulerManager>()
    val httpClient by inject<HttpClient>()

    // Запускаем Quartz Scheduler
    scheduler.start()

    // Настраиваем роутинг
    configureRouting()

    // Останавливаем scheduler и закрываем ресурсы при остановке приложения
    monitor.subscribe(ApplicationStopping) {
        scheduler.stop()
        httpClient.close()
        stopKoin()
    }
}

/**
 * Загружает переменные из .env файла и устанавливает их в System properties.
 * Это позволяет использовать переменные в application.yaml через ${VAR_NAME}.
 */
private fun loadDotEnv() {
    System.err.println("[INIT] loadDotEnv() called")

    try {
        val dotenv = dotenv {
            ignoreIfMissing = true // Не падать если .env файла нет
            systemProperties = false // Не устанавливать в System.properties автоматически
        }

        // Устанавливаем переменные в System properties вручную
        var loaded = 0
        dotenv.entries().forEach { entry ->
            System.setProperty(entry.key, entry.value)
            loaded++
        }

        System.err.println("[INIT] ✓ Loaded $loaded variables from .env file")
        System.err.println("[INIT] BOT_TOKEN = ${System.getProperty("BOT_TOKEN", "NOT_SET")?.take(20)}***")
        System.err.println("[INIT] CHAT_ID = ${System.getProperty("CHAT_ID", "NOT_SET")}")
    } catch (e: Exception) {
        System.err.println("[INIT] ⚠ Could not load .env file: ${e.message}")
        e.printStackTrace()
    }
}
