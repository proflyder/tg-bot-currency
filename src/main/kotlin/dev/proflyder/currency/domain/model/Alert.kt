package dev.proflyder.currency.domain.model

import kotlin.time.Duration

/**
 * Уровень критичности алерта
 */
enum class AlertLevel {
    WARNING,  // ⚠️ Предупреждение - небольшие изменения
    CRITICAL  // 🚨 Критическое - очень заметные изменения
}

/**
 * Период сравнения курсов
 */
enum class AlertPeriod(val duration: Duration, val displayName: String) {
    HOUR(Duration.parse("1h"), "час"),
    DAY(Duration.parse("24h"), "сутки"),
    WEEK(Duration.parse("168h"), "неделю"),
    MONTH(Duration.parse("720h"), "месяц")
}

/**
 * Тип валютной пары
 */
enum class CurrencyPair(val displayName: String, val emoji: String) {
    USD_TO_KZT("USD → KZT", "🇺🇸"),
    RUB_TO_KZT("RUB → KZT", "🇷🇺")
}

/**
 * Направление изменения курса
 */
enum class ChangeDirection {
    UP,    // Рост курса
    DOWN   // Падение курса
}

/**
 * Алерт об изменении курса
 */
data class CurrencyAlert(
    val level: AlertLevel,
    val period: AlertPeriod,
    val pair: CurrencyPair,
    val direction: ChangeDirection,
    val changePercent: Double,
    val oldRate: Double,
    val newRate: Double
)
