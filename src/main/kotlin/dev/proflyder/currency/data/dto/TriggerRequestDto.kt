package dev.proflyder.currency.data.dto

import kotlinx.serialization.Serializable

/**
 * Request DTO для endpoint принудительного запуска обновления курсов
 */
@Serializable
data class TriggerRequestDto(
    /**
     * ID чата Telegram, куда отправить сообщение с курсами.
     * Если не указан, используется chatId из конфигурации.
     */
    val chatId: String? = null
)
