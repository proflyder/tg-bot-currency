package dev.proflyder.currency.data.remote.unkey

import kotlinx.serialization.Serializable

/**
 * Data объект из Unkey API response
 */
@Serializable
data class UnkeyVerifyData(
    val valid: Boolean,
    val code: String? = null,
    val enabled: Boolean? = null,
    val keyId: String? = null,
    val name: String? = null,
    val ownerId: String? = null,
    val meta: Map<String, String>? = null,
    val expires: Long? = null,
    val remaining: Int? = null
)
