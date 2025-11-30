package dev.proflyder.currency.data.dto.telegram

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelegramMessage(
    @SerialName("message_id") val messageId: Long,
    @SerialName("chat") val chat: Chat,
    @SerialName("text") val text: String? = null
)
