package dev.proflyder.currency.data.dto.telegram

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatMemberUpdate(
    @SerialName("chat") val chat: Chat,
    @SerialName("from") val from: JsonElement,
    @SerialName("date") val date: Long,
    @SerialName("old_chat_member") val oldChatMember: JsonElement,
    @SerialName("new_chat_member") val newChatMember: JsonElement
)
