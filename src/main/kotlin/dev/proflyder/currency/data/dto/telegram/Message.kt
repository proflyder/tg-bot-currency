package dev.proflyder.currency.data.dto.telegram

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    @SerialName("message_id") val messageId: Int,
    @SerialName("text") val text: String? = null
)
