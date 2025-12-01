package dev.proflyder.currency.data.remote.unkey

import kotlinx.serialization.Serializable

/**
 * Meta объект из Unkey API response
 */
@Serializable
data class UnkeyVerifyMeta(
    val requestId: String? = null
)
