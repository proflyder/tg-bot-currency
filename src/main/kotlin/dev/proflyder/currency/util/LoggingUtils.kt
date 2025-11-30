package dev.proflyder.currency.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

/**
 * Получить логгер для класса
 * Использование: private val logger = logger()
 */
inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)

/**
 * Безопасное логирование с маскированием sensitive данных
 */
fun String.maskSensitive(): String {
    return this
        .replace(Regex("(token[\"']?\\s*[:=]\\s*[\"']?)([^\"'\\s]+)"), "$1***")
        .replace(Regex("(password[\"']?\\s*[:=]\\s*[\"']?)([^\"'\\s]+)"), "$1***")
        .replace(Regex("(api[_-]?key[\"']?\\s*[:=]\\s*[\"']?)([^\"'\\s]+)"), "$1***")
}

/**
 * Добавить контекст в MDC для трейсинга
 */
inline fun <T> withLoggingContext(context: Map<String, String>, block: () -> T): T {
    context.forEach { (key, value) -> MDC.put(key, value) }
    try {
        return block()
    } finally {
        context.keys.forEach { MDC.remove(it) }
    }
}

/**
 * Генерировать уникальный request ID
 */
fun generateRequestId(): String = java.util.UUID.randomUUID().toString().take(8)

/**
 * Логировать с таймингом выполнения
 */
inline fun <T> Logger.logWithTiming(message: String, block: () -> T): T {
    val start = System.currentTimeMillis()
    debug("$message - started")
    return try {
        val result = block()
        val duration = System.currentTimeMillis() - start
        info("$message - completed in ${duration}ms")
        result
    } catch (e: Exception) {
        val duration = System.currentTimeMillis() - start
        error("$message - failed after ${duration}ms", e)
        throw e
    }
}
