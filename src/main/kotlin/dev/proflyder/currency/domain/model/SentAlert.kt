package dev.proflyder.currency.domain.model

import kotlinx.datetime.Instant

/**
 * Ключ для идентификации уникального алерта (пара + период + тип курса)
 */
data class AlertKey(
    val pair: CurrencyPair,
    val period: AlertPeriod,
    val rateType: RateType
)

/**
 * Запись об отправленном алерте для дедупликации
 */
data class SentAlert(
    val key: AlertKey,
    val level: AlertLevel,
    val direction: ChangeDirection,
    val rateAtAlert: Double,
    val changePercent: Double,
    val sentAt: Instant
)
