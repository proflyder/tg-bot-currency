package dev.proflyder.currency.presentation.dto.error

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Унифицированный формат ошибок API (RFC 7807 Problem Details)
 *
 * Используется для всех типов ошибок в API для обеспечения
 * единообразного формата и упрощения обработки на клиенте.
 *
 * @property timestamp Временная метка возникновения ошибки (ISO 8601)
 * @property status HTTP статус код (дублируется для удобства клиента)
 * @property error Краткое название ошибки (например, "Not Found", "Internal Server Error")
 * @property message Детальное описание ошибки для разработчиков
 * @property path URL path, на котором произошла ошибка
 * @property errorCode Уникальный код ошибки для идентификации типа (например, "CURRENCY_001")
 * @property service Название сервиса/контроллера, где произошла ошибка
 * @property requestId Уникальный идентификатор запроса для трейсинга (из MDC)
 * @property details Дополнительные детали ошибки (опционально)
 */
@Serializable
data class ErrorResponse(
    val timestamp: Instant,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val errorCode: String? = null,
    val service: String? = null,
    val requestId: String? = null,
    val details: Map<String, String>? = null
) {
    companion object {
        /**
         * Создать ErrorResponse с текущим временем
         */
        fun create(
            status: Int,
            error: String,
            message: String,
            path: String,
            errorCode: String? = null,
            service: String? = null,
            requestId: String? = null,
            details: Map<String, String>? = null
        ): ErrorResponse {
            return ErrorResponse(
                timestamp = Clock.System.now(),
                status = status,
                error = error,
                message = message,
                path = path,
                errorCode = errorCode,
                service = service,
                requestId = requestId,
                details = details
            )
        }
    }
}
