package dev.proflyder.currency.data.dto.telegram

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelegramUpdate(
    @SerialName("update_id") val updateId: Long,
    @SerialName("message") val message: TelegramMessage? = null,
    @SerialName("my_chat_member") val myChatMember: ChatMemberUpdate? = null
)
