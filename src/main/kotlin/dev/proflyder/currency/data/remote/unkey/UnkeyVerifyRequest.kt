package dev.proflyder.currency.data.remote.unkey

import kotlinx.serialization.Serializable

/**
 * Request body для Unkey verifyKey endpoint
 */
@Serializable
data class UnkeyVerifyRequest(
    val key: String
)
