package dev.proflyder.currency.domain.usecase

import dev.proflyder.currency.domain.model.AlertLevel
import dev.proflyder.currency.domain.model.ChangeDirection
import dev.proflyder.currency.domain.model.CurrencyAlert
import dev.proflyder.currency.domain.model.CurrencyRate
import kotlin.math.abs

/**
 * UseCase для форматирования сообщений о курсах с алертами
 */
class FormatCurrencyMessageUseCase {

    /**
     * Форматирует сообщение о текущих курсах с опциональными алертами
     * @param rates Текущие курсы
     * @param alerts Список алертов (может быть пустым)
     * @return Отформатированное сообщение
     */
    operator fun invoke(rates: CurrencyRate, alerts: List<CurrencyAlert> = emptyList()): String {
        return buildString {
            // Основная информация о курсах
            appendLine("💱 <b>Курсы валют на kurs.kz</b>")
            appendLine()
            appendLine("🇺🇸 <b>USD → KZT</b>")
            appendLine("💵 Покупка: <code>${"%.2f".format(rates.usdToKzt.sell)}</code> ₸")  // я покупаю USD (обменник продает)
            appendLine("💸 Продажа: <code>${"%.2f".format(rates.usdToKzt.buy)}</code> ₸")   // я продаю USD (обменник покупает)
            appendLine()
            appendLine("🇷🇺 <b>RUB → KZT</b>")
            appendLine("💵 Покупка: <code>${"%.2f".format(rates.rubToKzt.sell)}</code> ₸")  // я покупаю RUB (обменник продает)
            appendLine("💸 Продажа: <code>${"%.2f".format(rates.rubToKzt.buy)}</code> ₸")   // я продаю RUB (обменник покупает)

            // Если есть алерты - добавляем их
            if (alerts.isNotEmpty()) {
                // Разделяем алерты по уровням
                val warnings = alerts.filter { it.level == AlertLevel.WARNING }
                val critical = alerts.filter { it.level == AlertLevel.CRITICAL }

                // Предупреждения
                if (warnings.isNotEmpty()) {
                    appendLine()
                    appendLine("━━━━━━━━")
                    appendLine("⚠️ <b>ПРЕДУПРЕЖДЕНИЯ</b>")
                    appendLine()
                    warnings.forEach { alert ->
                        append(formatAlert(alert))
                    }
                }

                // Критические изменения
                if (critical.isNotEmpty()) {
                    appendLine()
                    appendLine("━━━━━━━━")
                    appendLine("🚨 <b>КРИТИЧЕСКИЕ ИЗМЕНЕНИЯ</b>")
                    appendLine()
                    critical.forEach { alert ->
                        append(formatAlert(alert))
                    }
                }
            }
        }
    }

    /**
     * Форматирует один алерт
     */
    private fun formatAlert(alert: CurrencyAlert): String {
        val directionEmoji = when (alert.direction) {
            ChangeDirection.UP -> "📈"
            ChangeDirection.DOWN -> "📉"
        }

        val changeVerb = when (alert.direction) {
            ChangeDirection.UP -> "вырос"
            ChangeDirection.DOWN -> "упал"
        }

        return buildString {
            append("$directionEmoji ${alert.pair.emoji} <b>${alert.pair.displayName}</b> (${alert.rateType.displayName}) ")
            append("$changeVerb на <code>${"%.2f".format(abs(alert.changePercent))}%</code> ")
            appendLine("за ${alert.period.displayName}")
            appendLine("   <code>${"%.2f".format(alert.oldRate)}</code> ₸ → <code>${"%.2f".format(alert.newRate)}</code> ₸")
            appendLine()
        }
    }
}
