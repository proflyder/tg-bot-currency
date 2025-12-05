package dev.proflyder.currency.presentation.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Route.metricsRoutes(prometheusRegistry: PrometheusMeterRegistry) {
    get("/metrics") {
        call.respondText(
            text = prometheusRegistry.scrape(),
            contentType = ContentType.parse("text/plain; version=0.0.4")
        )
    }
}
