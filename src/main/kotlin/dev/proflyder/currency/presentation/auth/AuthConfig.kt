package dev.proflyder.currency.presentation.auth

import dev.proflyder.currency.data.remote.unkey.UnkeyClient
import io.ktor.server.application.*
import io.ktor.server.auth.*

/**
 * Настройка authentication для приложения
 */
fun Application.configureAuthentication(unkeyClient: UnkeyClient) {
    install(Authentication) {
        unkey("unkey-auth", unkeyClient)
    }
}
