package dev.proflyder.currency.data.dto.telegram

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelegramUpdate(
    @SerialName("update_id") val updateId: Long,

    // Сообщения
    @SerialName("message") val message: TelegramMessage? = null,
    @SerialName("edited_message") val editedMessage: TelegramMessage? = null,
    @SerialName("channel_post") val channelPost: TelegramMessage? = null,
    @SerialName("edited_channel_post") val editedChannelPost: TelegramMessage? = null,

    // Inline режим
    @SerialName("inline_query") val inlineQuery: Map<String, String>? = null,
    @SerialName("chosen_inline_result") val chosenInlineResult: Map<String, String>? = null,

    // Callback от inline кнопок
    @SerialName("callback_query") val callbackQuery: Map<String, String>? = null,

    // Платежи
    @SerialName("shipping_query") val shippingQuery: Map<String, String>? = null,
    @SerialName("pre_checkout_query") val preCheckoutQuery: Map<String, String>? = null,

    // Опросы
    @SerialName("poll") val poll: Map<String, String>? = null,
    @SerialName("poll_answer") val pollAnswer: Map<String, String>? = null,

    // Изменения в чате
    @SerialName("my_chat_member") val myChatMember: ChatMemberUpdate? = null,
    @SerialName("chat_member") val chatMember: Map<String, String>? = null,
    @SerialName("chat_join_request") val chatJoinRequest: Map<String, String>? = null
)
