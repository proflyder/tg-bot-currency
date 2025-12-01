package dev.proflyder.currency.data.remote.unkey

import kotlinx.serialization.Serializable

/**
 * Response от Unkey verifyKey endpoint
 */
@Serializable
data class UnkeyVerifyResponse(
    val data: UnkeyVerifyData,
    val meta: UnkeyVerifyMeta? = null
)
