package dev.proflyder.currency.domain.repository

import dev.proflyder.currency.domain.model.AlertKey
import dev.proflyder.currency.domain.model.SentAlert

/**
 * Репозиторий для хранения информации об отправленных алертах.
 * Используется для дедупликации повторных уведомлений.
 */
interface SentAlertRepository {
    suspend fun getLastSentAlert(key: AlertKey): Result<SentAlert?>
    suspend fun recordSentAlert(alert: SentAlert): Result<Unit>
    suspend fun clearSentAlert(key: AlertKey): Result<Unit>
    suspend fun deleteAll(): Result<Int>
}
