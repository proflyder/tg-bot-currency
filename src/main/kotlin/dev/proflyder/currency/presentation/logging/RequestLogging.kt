package dev.proflyder.currency.presentation.logging

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.request.*
import io.ktor.util.*
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.*

private val incomingLogger = LoggerFactory.getLogger("http.incoming")

fun Application.configureRequestLogging() {
    install(CallId) {
        header(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }

    intercept(ApplicationCallPipeline.Setup) {
        call.attributes.put(startTimeKey, System.currentTimeMillis())
    }

    intercept(ApplicationCallPipeline.Monitoring) {
        val path = call.request.uri
        if (path.startsWith("/api/") || path.startsWith("/telegram/")) {
            try {
                proceed()
            } finally {
                logRequest(call)
            }
        } else {
            proceed()
        }
    }
}

private fun logRequest(call: ApplicationCall) {
    try {
        val method = call.request.httpMethod.value
        val path = call.request.uri
        val status = call.response.status()?.value ?: 0
        val duration = call.processingTimeMillis()
        val userAgent = call.request.headers["User-Agent"] ?: "unknown"
        val contentLength = call.request.headers["Content-Length"]?.toLongOrNull() ?: 0L
        val requestId = call.callId ?: "no-id"

        MDC.put("request_id", requestId)
        MDC.put("direction", "incoming")
        MDC.put("http_method", method)
        MDC.put("http_path", path)
        MDC.put("http_status", status.toString())
        MDC.put("duration_ms", duration.toString())
        MDC.put("user_agent", userAgent)
        MDC.put("content_length", contentLength.toString())

        val endpoint = extractEndpoint(path)
        MDC.put("endpoint", endpoint)

        if (status >= 400) {
            MDC.put("log_type", "error")
            incomingLogger.warn("INCOMING_ERROR - $method $endpoint - $status (${duration}ms)")
        } else {
            MDC.put("log_type", "request")
            incomingLogger.info("INCOMING_REQUEST - $method $endpoint - $status (${duration}ms)")
        }
    } finally {
        MDC.clear()
    }
}

private fun extractEndpoint(path: String): String {
    val pathWithoutQuery = path.substringBefore('?')
    return when {
        pathWithoutQuery.startsWith("/api/") -> pathWithoutQuery
        pathWithoutQuery.startsWith("/telegram/") -> pathWithoutQuery
        else -> pathWithoutQuery
    }
}

private fun ApplicationCall.processingTimeMillis(): Long {
    val startTime = attributes.getOrNull(startTimeKey) ?: return 0L
    return System.currentTimeMillis() - startTime
}

private val startTimeKey = AttributeKey<Long>("RequestStartTime")
