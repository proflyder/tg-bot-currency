package dev.proflyder.currency.domain.model

/**
 * Пороги изменения курса для одного периода
 */
data class ThresholdConfig(
    val period: AlertPeriod,
    val warningPercent: Double,  // Порог для предупреждения (%)
    val criticalPercent: Double  // Порог для критического алерта (%)
)

/**
 * Конфигурация всех порогов
 *
 * Пороги основаны на статистическом анализе волатильности валютных пар:
 * - Час: короткие колебания, требуют высокой чувствительности
 * - Сутки: дневная волатильность, учитывает новости и события
 * - Неделя: недельные тренды, более существенные изменения
 * - Месяц: долгосрочные тренды, значительные экономические сдвиги
 */
object CurrencyThresholds {

    val HOUR = ThresholdConfig(
        period = AlertPeriod.HOUR,
        warningPercent = 0.5,   // 0.5% за час - небольшие колебания
        criticalPercent = 1.0   // 1.0% за час - заметные изменения
    )

    val DAY = ThresholdConfig(
        period = AlertPeriod.DAY,
        warningPercent = 1.0,   // 1.0% за сутки - нормальная волатильность
        criticalPercent = 2.0   // 2.0% за сутки - значительные изменения
    )

    val WEEK = ThresholdConfig(
        period = AlertPeriod.WEEK,
        warningPercent = 2.0,   // 2.0% за неделю - недельный тренд
        criticalPercent = 4.0   // 4.0% за неделю - очень сильные изменения
    )

    val MONTH = ThresholdConfig(
        period = AlertPeriod.MONTH,
        warningPercent = 3.0,   // 3.0% за месяц - месячный тренд
        criticalPercent = 5.0   // 5.0% за месяц - критические изменения
    )

    /**
     * Все настроенные пороги
     */
    val ALL = listOf(HOUR, DAY, WEEK, MONTH)
}
