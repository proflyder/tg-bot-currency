package dev.proflyder.currency.data.dto.telegram

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id") val chatId: String,
    @SerialName("text") val text: String,
    @SerialName("parse_mode") val parseMode: String = "HTML"
)
