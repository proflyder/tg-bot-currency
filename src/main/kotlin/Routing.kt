package dev.proflyder.currency

import dev.proflyder.currency.presentation.controller.CurrencyHistoryController
import dev.proflyder.currency.presentation.controller.TelegramWebhookController
import dev.proflyder.currency.presentation.controller.TriggerController
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    // Настройка Content Negotiation для JSON сериализации
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // Внедряем контроллеры через Koin
    val currencyHistoryController by inject<CurrencyHistoryController>()
    val triggerController by inject<TriggerController>()
    val telegramWebhookController by inject<TelegramWebhookController>()

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        // Telegram webhook - no authentication needed (Telegram sends updates)
        post("/telegram/webhook") {
            telegramWebhookController.handleWebhook(call)
        }

        // Currency History API - protected with Unkey authentication
        authenticate("unkey-auth") {
            get("/api/history") {
                currencyHistoryController.getHistory(call)
            }

            get("/api/latest") {
                currencyHistoryController.getLatest(call)
            }

            // Manual trigger for currency update
            post("/api/trigger") {
                triggerController.triggerCurrencyUpdate(call)
            }
        }
    }
}
