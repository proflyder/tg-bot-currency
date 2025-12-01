package dev.proflyder.currency.di

import dev.proflyder.currency.data.remote.parser.KursKzParser
import dev.proflyder.currency.data.remote.telegram.TelegramApi
import dev.proflyder.currency.data.remote.unkey.UnkeyClient
import dev.proflyder.currency.data.repository.CurrencyHistoryRepositoryImpl
import dev.proflyder.currency.data.repository.CurrencyRepositoryImpl
import dev.proflyder.currency.data.repository.TelegramRepositoryImpl
import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import dev.proflyder.currency.domain.repository.CurrencyRepository
import dev.proflyder.currency.domain.repository.TelegramRepository
import dev.proflyder.currency.domain.usecase.CheckCurrencyThresholdsUseCase
import dev.proflyder.currency.domain.usecase.FormatCurrencyMessageUseCase
import dev.proflyder.currency.domain.usecase.GetCurrencyHistoryUseCase
import dev.proflyder.currency.domain.usecase.GetLatestCurrencyRateUseCase
import dev.proflyder.currency.domain.usecase.SendCurrencyRatesUseCase
import dev.proflyder.currency.presentation.controller.CurrencyHistoryController
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
                level = LogLevel.ALL
            }
        }
    }

    // Data Layer - Remote
    single { TelegramApi(get(), get<AppConfig>().botToken) }
    single { KursKzParser(get()) }
    single { UnkeyClient(get(), get<AppConfig>().unkeyRootKey) }

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

    // Presentation Layer - Controllers
    single { CurrencyHistoryController(get(), get()) }

    // Scheduler
    single { QuartzSchedulerManager(get(), get()) }
}
