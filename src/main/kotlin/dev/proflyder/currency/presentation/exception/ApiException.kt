package dev.proflyder.currency.presentation.exception

import io.ktor.http.*

/**
 * Базовое исключение для всех API ошибок
 *
 * @property message Описание ошибки
 * @property statusCode HTTP статус код
 * @property errorCode Уникальный код ошибки для идентификации типа
 * @property service Название сервиса/контроллера, где произошла ошибка
 * @property details Дополнительные детали ошибки
 * @property cause Исходная причина ошибки (если есть)
 */
open class ApiException(
    override val message: String,
    val statusCode: HttpStatusCode = HttpStatusCode.InternalServerError,
    val errorCode: String? = null,
    val service: String? = null,
    val details: Map<String, String>? = null,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Ошибка "Ресурс не найден" (404)
 */
class NotFoundException(
    message: String,
    errorCode: String? = "NOT_FOUND",
    service: String? = null,
    details: Map<String, String>? = null
) : ApiException(
    message = message,
    statusCode = HttpStatusCode.NotFound,
    errorCode = errorCode,
    service = service,
    details = details
)

/**
 * Ошибка валидации данных (400)
 */
class ValidationException(
    message: String,
    errorCode: String? = "VALIDATION_ERROR",
    service: String? = null,
    details: Map<String, String>? = null
) : ApiException(
    message = message,
    statusCode = HttpStatusCode.BadRequest,
    errorCode = errorCode,
    service = service,
    details = details
)

/**
 * Ошибка внешнего сервиса (502 Bad Gateway)
 */
class ExternalServiceException(
    message: String,
    errorCode: String? = "EXTERNAL_SERVICE_ERROR",
    service: String? = null,
    details: Map<String, String>? = null,
    cause: Throwable? = null
) : ApiException(
    message = message,
    statusCode = HttpStatusCode.BadGateway,
    errorCode = errorCode,
    service = service,
    details = details,
    cause = cause
)

/**
 * Ошибка базы данных (500)
 */
class DatabaseException(
    message: String,
    errorCode: String? = "DATABASE_ERROR",
    service: String? = null,
    details: Map<String, String>? = null,
    cause: Throwable? = null
) : ApiException(
    message = message,
    statusCode = HttpStatusCode.InternalServerError,
    errorCode = errorCode,
    service = service,
    details = details,
    cause = cause
)

/**
 * Ошибка конфигурации (500)
 */
class ConfigurationException(
    message: String,
    errorCode: String? = "CONFIGURATION_ERROR",
    service: String? = null,
    details: Map<String, String>? = null
) : ApiException(
    message = message,
    statusCode = HttpStatusCode.InternalServerError,
    errorCode = errorCode,
    service = service,
    details = details
)
