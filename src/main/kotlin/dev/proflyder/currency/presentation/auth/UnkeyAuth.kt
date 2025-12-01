package dev.proflyder.currency.presentation.auth

import dev.proflyder.currency.data.remote.unkey.UnkeyClient
import dev.proflyder.currency.util.logger
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*

/**
 * Principal для Unkey authentication
 * Содержит информацию о верифицированном API ключе
 */
data class UnkeyPrincipal(
    val keyId: String?,
    val name: String?,
    val ownerId: String?
)

/**
 * Unkey authentication provider
 */
class UnkeyAuthProvider(config: Configuration) : AuthenticationProvider(config) {
    private val logger = logger()
    private val authenticationFunction: suspend (String) -> UnkeyPrincipal? = config.authenticationFunction

    class Configuration(
        name: String?,
        val unkeyClient: UnkeyClient
    ) : Config(name) {
        var authenticationFunction: suspend (String) -> UnkeyPrincipal? = { apiKey ->
            unkeyClient.verifyKey(apiKey).fold(
                onSuccess = { response ->
                    if (response.data.valid) {
                        UnkeyPrincipal(
                            keyId = response.data.keyId,
                            name = response.data.name,
                            ownerId = response.data.ownerId
                        )
                    } else {
                        null
                    }
                },
                onFailure = {
                    null
                }
            )
        }

        fun build() = UnkeyAuthProvider(this)
    }

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        // Извлекаем API ключ из заголовка Authorization
        val apiKey = context.call.request.headers[HttpHeaders.Authorization]
            ?.removePrefix("Bearer ")
            ?.trim()

        if (apiKey.isNullOrBlank()) {
            logger.warn("Missing or empty Authorization header")
            context.challenge("UnkeyAuth", AuthenticationFailedCause.NoCredentials) { challenge, call ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing API key"))
                challenge.complete()
            }
            return
        }

        logger.debug("Verifying API key")

        val principal = try {
            authenticationFunction(apiKey)
        } catch (e: Exception) {
            logger.error("Error verifying API key", e)
            null
        }

        if (principal != null) {
            logger.info("API key verified successfully for owner: ${principal.ownerId}")
            context.principal(principal)
        } else {
            logger.warn("Invalid API key")
            context.challenge("UnkeyAuth", AuthenticationFailedCause.InvalidCredentials) { challenge, call ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid API key"))
                challenge.complete()
            }
        }
    }
}

/**
 * Extension function для настройки Unkey authentication
 */
fun AuthenticationConfig.unkey(
    name: String? = null,
    unkeyClient: UnkeyClient,
    configure: UnkeyAuthProvider.Configuration.() -> Unit = {}
) {
    val provider = UnkeyAuthProvider.Configuration(name, unkeyClient).apply(configure).build()
    register(provider)
}
