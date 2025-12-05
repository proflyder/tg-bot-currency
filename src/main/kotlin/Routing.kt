package dev.proflyder.currency

import dev.proflyder.currency.presentation.controller.CurrencyHistoryController
import dev.proflyder.currency.presentation.controller.TelegramWebhookController
import dev.proflyder.currency.presentation.controller.TriggerController
import dev.proflyder.currency.presentation.routes.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Application.configureRouting(prometheusRegistry: PrometheusMeterRegistry) {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    val currencyHistoryController by inject<CurrencyHistoryController>()
    val triggerController by inject<TriggerController>()
    val telegramWebhookController by inject<TelegramWebhookController>()

    routing {
        metricsRoutes(prometheusRegistry)
        swaggerRoutes()
        webhookRoutes(telegramWebhookController)

        authenticate("unkey-auth") {
            currencyHistoryRoutes(currencyHistoryController)
            triggerRoutes(triggerController)
        }
    }
}
