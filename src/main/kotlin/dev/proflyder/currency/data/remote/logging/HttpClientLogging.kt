package dev.proflyder.currency.data.remote.logging

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.util.*
import org.slf4j.LoggerFactory
import org.slf4j.MDC

private val logger = LoggerFactory.getLogger("http.outgoing")

/**
 * HttpClient plugin для структурированного логирования исходящих HTTP запросов
 * Логирует ТОЛЬКО внешние API: kurs.kz, api.telegram.org, api.unkey.dev
 */
class OutgoingRequestLogging {

    companion object : HttpClientPlugin<Unit, OutgoingRequestLogging> {
        override val key = AttributeKey<OutgoingRequestLogging>("OutgoingRequestLogging")

        // Список внешних сервисов которые мы мониторим
        private val MONITORED_HOSTS = setOf(
            "kurs.kz",
            "api.telegram.org",
            "api.unkey.dev"
        )

        override fun prepare(block: Unit.() -> Unit) = OutgoingRequestLogging()

        override fun install(plugin: OutgoingRequestLogging, scope: HttpClient) {
            scope.plugin(HttpSend).intercept { request ->
                val url = request.url.toString()
                val host = request.url.host

                // Пробрасываем request_id из MDC в исходящие запросы
                val requestId = MDC.get("request_id") ?: request.headers["X-Request-ID"]
                if (requestId != null && !request.headers.contains("X-Request-ID")) {
                    request.headers.append("X-Request-ID", requestId)
                }

                // Логируем ТОЛЬКО если это один из наших внешних сервисов
                if (MONITORED_HOSTS.any { host.contains(it) }) {
                    val startTime = System.currentTimeMillis()
                    val method = request.method.value
                    val finalRequestId = requestId ?: "no-id"

                    // Определяем сервис по host
                    val service = when {
                        host.contains("kurs.kz") -> "kurs_kz"
                        host.contains("telegram") -> "telegram_api"
                        host.contains("unkey") -> "unkey_api"
                        else -> "unknown"
                    }

                    try {
                        val call = execute(request)
                        val duration = System.currentTimeMillis() - startTime
                        val status = call.response.status.value

                        // Structured log для успешных запросов
                        MDC.put("request_id", finalRequestId)
                        MDC.put("direction", "outgoing")
                        MDC.put("service", service)
                        MDC.put("http_method", method)
                        MDC.put("http_url", sanitizeUrl(url))
                        MDC.put("http_status", status.toString())
                        MDC.put("duration_ms", duration.toString())

                        if (status >= 400) {
                            MDC.put("log_type", "error")
                            logger.warn("OUTGOING_ERROR - $service $method - $status (${duration}ms)")
                        } else {
                            MDC.put("log_type", "request")
                            logger.info("OUTGOING_REQUEST - $service $method - $status (${duration}ms)")
                        }

                        MDC.clear()
                        call

                    } catch (e: Exception) {
                        val duration = System.currentTimeMillis() - startTime

                        // Structured log для failed запросов
                        MDC.put("request_id", finalRequestId)
                        MDC.put("direction", "outgoing")
                        MDC.put("service", service)
                        MDC.put("http_method", method)
                        MDC.put("http_url", sanitizeUrl(url))
                        MDC.put("http_status", "0")
                        MDC.put("duration_ms", duration.toString())
                        MDC.put("log_type", "failure")
                        MDC.put("error_type", e::class.simpleName ?: "Unknown")
                        MDC.put("error_message", e.message ?: "No message")

                        logger.error("OUTGOING_FAILED - $service $method - ${e::class.simpleName} (${duration}ms)")
                        MDC.clear()

                        throw e
                    }
                } else {
                    // Не наш сервис - просто выполняем без логирования
                    execute(request)
                }
            }
        }

        /**
         * Убираем query params из URL для безопасности (могут содержать токены)
         * Пример: https://api.telegram.org/botTOKEN/sendMessage?chat_id=123
         *      -> https://api.telegram.org/botTOKEN/sendMessage?...
         */
        private fun sanitizeUrl(url: String): String {
            val urlWithoutQuery = url.substringBefore('?')
            val hasQuery = url.contains('?')
            return if (hasQuery) "$urlWithoutQuery?..." else urlWithoutQuery
        }
    }
}
