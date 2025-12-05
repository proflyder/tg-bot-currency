package dev.proflyder.currency.di

import dev.proflyder.currency.data.remote.api.TriggerApiClient
import dev.proflyder.currency.data.remote.logging.OutgoingRequestLogging
import dev.proflyder.currency.data.remote.parser.KursKzParser
import dev.proflyder.currency.data.remote.telegram.TelegramApi
import dev.proflyder.currency.data.remote.unkey.UnkeyClient
import dev.proflyder.currency.data.repository.CurrencyHistoryRepositoryImpl
import dev.proflyder.currency.data.repository.CurrencyRepositoryImpl
import dev.proflyder.currency.data.repository.TelegramRepositoryImpl
import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import dev.proflyder.currency.domain.repository.CurrencyRepository
import dev.proflyder.currency.domain.repository.TelegramRepository
import dev.proflyder.currency.domain.telegram.TelegramCommandHandler
import dev.proflyder.currency.domain.usecase.*
import dev.proflyder.currency.presentation.controller.CurrencyHistoryController
import dev.proflyder.currency.presentation.controller.TelegramWebhookController
import dev.proflyder.currency.presentation.controller.TriggerController
import dev.proflyder.currency.scheduler.QuartzSchedulerManager
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val appModule = module {
    // HTTP Client
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(Logging) {
                level = LogLevel.INFO
            }
            // Структурированное логирование исходящих запросов
            install(OutgoingRequestLogging)
        }
    }

    // Data Layer - Remote
    single { TelegramApi(get(), get<AppConfig>().botToken) }
    single { KursKzParser(get()) }
    single { UnkeyClient(get(), get<AppConfig>().unkeyRootKey) }
    single { TriggerApiClient(get(), get<AppConfig>().internalApiKey) }

    // Data Layer - Repositories
    single<CurrencyRepository> { CurrencyRepositoryImpl(get()) }
    single<TelegramRepository> { TelegramRepositoryImpl(get()) }
    single<CurrencyHistoryRepository> {
        CurrencyHistoryRepositoryImpl(get<AppConfig>().databasePath)
    }

    // Domain Layer - Use Cases
    single { CheckCurrencyThresholdsUseCase(get()) }
    single { FormatCurrencyMessageUseCase() }
    single { SendCurrencyRatesUseCase(get(), get(), get(), get(), get()) }
    single { GetCurrencyHistoryUseCase(get()) }
    single { GetLatestCurrencyRateUseCase(get()) }
    single { DeleteCurrencyHistoryUseCase(get()) }

    // Presentation Layer - Controllers
    single { CurrencyHistoryController(get(), get(), get()) }
    single { TriggerController(get(), get()) }
    single { TelegramWebhookController(get()) }

    // Scheduler
    single { QuartzSchedulerManager(get(), get()) }

    // Telegram Bot
    single { TelegramCommandHandler(get(), get()) } // TelegramApi, TriggerApiClient
}
