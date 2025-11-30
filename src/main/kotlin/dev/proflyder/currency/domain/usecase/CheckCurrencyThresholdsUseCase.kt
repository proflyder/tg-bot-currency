package dev.proflyder.currency.domain.usecase

import dev.proflyder.currency.domain.model.*
import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import dev.proflyder.currency.util.logger
import kotlin.math.abs

/**
 * UseCase для проверки превышения порогов изменения курсов
 */
class CheckCurrencyThresholdsUseCase(
    private val currencyHistoryRepository: CurrencyHistoryRepository
) {
    private val logger = logger()

    /**
     * Проверяет превышение порогов для текущих курсов
     * @param currentRates Текущие курсы валют
     * @return Список алертов (пустой если ничего не превышено)
     */
    suspend operator fun invoke(currentRates: CurrencyRate): Result<List<CurrencyAlert>> = runCatching {
        logger.info("Checking currency thresholds...")

        val alerts = mutableListOf<CurrencyAlert>()

        // Проверяем каждый период
        for (thresholdConfig in CurrencyThresholds.ALL) {
            logger.debug("Checking ${thresholdConfig.period.displayName} thresholds...")

            // Получаем исторические данные
            val historicalRecordResult = currencyHistoryRepository.getRecordBefore(thresholdConfig.period.duration)

            historicalRecordResult.fold(
                onSuccess = { historicalRecord ->
                    if (historicalRecord == null) {
                        logger.debug("No historical data for ${thresholdConfig.period.displayName}, skipping")
                    } else {
                        // Проверяем USD → KZT
                        checkPairThresholds(
                            pair = CurrencyPair.USD_TO_KZT,
                            currentRate = currentRates.usdToKzt.sell, // Используем sell как базовый курс
                            historicalRate = historicalRecord.rates.usdToKzt.sell,
                            thresholdConfig = thresholdConfig
                        )?.let { alerts.add(it) }

                        // Проверяем RUB → KZT
                        checkPairThresholds(
                            pair = CurrencyPair.RUB_TO_KZT,
                            currentRate = currentRates.rubToKzt.sell, // Используем sell как базовый курс
                            historicalRate = historicalRecord.rates.rubToKzt.sell,
                            thresholdConfig = thresholdConfig
                        )?.let { alerts.add(it) }
                    }
                },
                onFailure = { error ->
                    logger.error("Failed to get historical data for ${thresholdConfig.period.displayName}", error)
                }
            )
        }

        if (alerts.isEmpty()) {
            logger.info("No thresholds exceeded")
        } else {
            logger.info("Found ${alerts.size} threshold alerts")
        }

        alerts
    }

    /**
     * Проверяет пороги для одной валютной пары
     */
    private fun checkPairThresholds(
        pair: CurrencyPair,
        currentRate: Double,
        historicalRate: Double,
        thresholdConfig: ThresholdConfig
    ): CurrencyAlert? {
        // Вычисляем процент изменения
        val changePercent = ((currentRate - historicalRate) / historicalRate) * 100.0
        val absChangePercent = abs(changePercent)

        // Определяем направление изменения
        val direction = if (changePercent > 0) ChangeDirection.UP else ChangeDirection.DOWN

        // Проверяем критический порог
        if (absChangePercent >= thresholdConfig.criticalPercent) {
            logger.info(
                "🚨 CRITICAL: ${pair.displayName} changed by %.2f%% in ${thresholdConfig.period.displayName} (threshold: %.2f%%)".format(
                    changePercent,
                    thresholdConfig.criticalPercent
                )
            )
            return CurrencyAlert(
                level = AlertLevel.CRITICAL,
                period = thresholdConfig.period,
                pair = pair,
                direction = direction,
                changePercent = changePercent,
                oldRate = historicalRate,
                newRate = currentRate
            )
        }

        // Проверяем порог предупреждения
        if (absChangePercent >= thresholdConfig.warningPercent) {
            logger.info(
                "⚠️ WARNING: ${pair.displayName} changed by %.2f%% in ${thresholdConfig.period.displayName} (threshold: %.2f%%)".format(
                    changePercent,
                    thresholdConfig.warningPercent
                )
            )
            return CurrencyAlert(
                level = AlertLevel.WARNING,
                period = thresholdConfig.period,
                pair = pair,
                direction = direction,
                changePercent = changePercent,
                oldRate = historicalRate,
                newRate = currentRate
            )
        }

        // Порог не превышен
        return null
    }
}
