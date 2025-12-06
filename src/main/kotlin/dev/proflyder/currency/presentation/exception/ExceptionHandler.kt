package dev.proflyder.currency.presentation.exception

import dev.proflyder.currency.presentation.dto.error.ErrorResponse
import dev.proflyder.currency.util.logger
import io.ktor.http.*
import io.ktor.serialization.ContentConvertException
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.slf4j.MDC

/**
 * Конфигурация глобального обработчика исключений
 *
 * Использует StatusPages plugin для перехвата всех исключений
 * и преобразования их в унифицированный ErrorResponse формат.
 */
fun Application.configureExceptionHandling() {
    val logger = logger()

    install(StatusPages) {
        // Обработка кастомных API исключений
        exception<ApiException> { call, cause ->
            val requestId = MDC.get("request_id") ?: "unknown"
            val path = call.request.path()

            logger.error(
                "API Exception: {} - {} (requestId: {}, path: {}, errorCode: {})",
                cause.statusCode.value,
                cause.message,
                requestId,
                path,
                cause.errorCode,
                cause
            )

            val errorResponse = ErrorResponse.create(
                status = cause.statusCode.value,
                error = cause.statusCode.description,
                message = cause.message,
                path = path,
                errorCode = cause.errorCode,
                service = cause.service,
                requestId = requestId,
                details = cause.details
            )

            call.respond(cause.statusCode, errorResponse)
        }

        // Обработка ошибок валидации Content Negotiation (400)
        exception<ContentConvertException> { call, cause ->
            val requestId = MDC.get("request_id") ?: "unknown"
            val path = call.request.path()

            logger.error(
                "Content Conversion Error: {} (requestId: {}, path: {})",
                cause.message,
                requestId,
                path,
                cause
            )

            val errorResponse = ErrorResponse.create(
                status = HttpStatusCode.BadRequest.value,
                error = HttpStatusCode.BadRequest.description,
                message = "Invalid request body format: ${cause.message}",
                path = path,
                errorCode = "INVALID_REQUEST_BODY",
                requestId = requestId
            )

            call.respond(HttpStatusCode.BadRequest, errorResponse)
        }

        // Обработка всех остальных необработанных исключений (500)
        exception<Throwable> { call, cause ->
            val requestId = MDC.get("request_id") ?: "unknown"
            val path = call.request.path()

            logger.error(
                "Unhandled Exception: {} (requestId: {}, path: {})",
                cause.message,
                requestId,
                path,
                cause
            )

            val errorResponse = ErrorResponse.create(
                status = HttpStatusCode.InternalServerError.value,
                error = HttpStatusCode.InternalServerError.description,
                message = "An unexpected error occurred: ${cause.message ?: "Unknown error"}",
                path = path,
                errorCode = "INTERNAL_ERROR",
                requestId = requestId
            )

            call.respond(HttpStatusCode.InternalServerError, errorResponse)
        }

        // Обработка 404 Not Found (когда роут не найден)
        status(HttpStatusCode.NotFound) { call, status ->
            val requestId = MDC.get("request_id") ?: "unknown"
            val path = call.request.path()

            logger.warn(
                "Route Not Found: {} (requestId: {}, method: {})",
                path,
                requestId,
                call.request.httpMethod.value
            )

            val errorResponse = ErrorResponse.create(
                status = status.value,
                error = status.description,
                message = "The requested endpoint '${call.request.httpMethod.value} $path' does not exist",
                path = path,
                errorCode = "ROUTE_NOT_FOUND",
                requestId = requestId
            )

            call.respond(status, errorResponse)
        }

        // Обработка 401 Unauthorized (от Unkey auth plugin)
        status(HttpStatusCode.Unauthorized) { call, status ->
            val requestId = MDC.get("request_id") ?: "unknown"
            val path = call.request.path()

            logger.warn(
                "Unauthorized Access: {} (requestId: {})",
                path,
                requestId
            )

            val errorResponse = ErrorResponse.create(
                status = status.value,
                error = status.description,
                message = "Authentication required: Invalid or missing API key",
                path = path,
                errorCode = "UNAUTHORIZED",
                requestId = requestId,
                details = mapOf("hint" to "Provide valid Bearer token in Authorization header")
            )

            call.respond(status, errorResponse)
        }
    }
}
