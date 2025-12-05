package dev.proflyder.currency.presentation.routes

import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.swaggerRoutes() {
    get("/", {
        hidden = true
    }) {
        call.respondText("Hello World!")
    }

    route("api.json") {
        openApiSpec()
    }

    route("swagger") {
        swaggerUI("/api.json")
    }
}
