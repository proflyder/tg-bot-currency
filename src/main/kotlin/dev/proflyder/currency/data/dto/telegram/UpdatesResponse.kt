package dev.proflyder.currency.data.dto.telegram

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdatesResponse(
    @SerialName("ok") val ok: Boolean,
    @SerialName("result") val result: List<TelegramUpdate> = emptyList(),
    @SerialName("description") val description: String? = null
)
