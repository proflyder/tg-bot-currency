package dev.proflyder.currency.domain.usecase

import dev.proflyder.currency.domain.model.*
import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import dev.proflyder.currency.domain.repository.SentAlertRepository
import dev.proflyder.currency.util.logger
import kotlin.math.abs

/**
 * UseCase для проверки превышения порогов изменения курсов
 */
class CheckCurrencyThresholdsUseCase(
    private val currencyHistoryRepository: CurrencyHistoryRepository,
    private val sentAlertRepository: SentAlertRepository
) {
    private val logger = logger()

    /**
     * Пороги повторного алерта (50% от warning threshold).
     * Если курс изменился меньше чем на этот процент с момента последнего алерта — подавляем.
     */
    private val reAlertThresholds = mapOf(
        AlertPeriod.HOUR to 0.25,
        AlertPeriod.DAY to 0.50,
        AlertPeriod.WEEK to 1.00,
        AlertPeriod.MONTH to 1.50
    )

    /**
     * Проверяет превышение порогов для текущих курсов
     * @param currentRates Текущие курсы валют
     * @return Список алертов (пустой если ничего не превышено)
     */
    suspend operator fun invoke(currentRates: CurrencyRate, skipDedup: Boolean = false): Result<List<CurrencyAlert>> = runCatching {
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
                        // Проверяем USD → KZT (SELL)
                        processAlert(
                            pair = CurrencyPair.USD_TO_KZT,
                            rateType = RateType.SELL,
                            currentRate = currentRates.usdToKzt.sell,
                            historicalRate = historicalRecord.rates.usdToKzt.sell,
                            thresholdConfig = thresholdConfig,
                            alerts = alerts,
                            skipDedup = skipDedup
                        )

                        // Проверяем USD → KZT (BUY)
                        processAlert(
                            pair = CurrencyPair.USD_TO_KZT,
                            rateType = RateType.BUY,
                            currentRate = currentRates.usdToKzt.buy,
                            historicalRate = historicalRecord.rates.usdToKzt.buy,
                            thresholdConfig = thresholdConfig,
                            alerts = alerts,
                            skipDedup = skipDedup
                        )

                        // Проверяем RUB → KZT (SELL)
                        processAlert(
                            pair = CurrencyPair.RUB_TO_KZT,
                            rateType = RateType.SELL,
                            currentRate = currentRates.rubToKzt.sell,
                            historicalRate = historicalRecord.rates.rubToKzt.sell,
                            thresholdConfig = thresholdConfig,
                            alerts = alerts,
                            skipDedup = skipDedup
                        )

                        // Проверяем RUB → KZT (BUY)
                        processAlert(
                            pair = CurrencyPair.RUB_TO_KZT,
                            rateType = RateType.BUY,
                            currentRate = currentRates.rubToKzt.buy,
                            historicalRate = historicalRecord.rates.rubToKzt.buy,
                            thresholdConfig = thresholdConfig,
                            alerts = alerts,
                            skipDedup = skipDedup
                        )
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
     * Обрабатывает одну проверку: вычисляет алерт, применяет дедупликацию, очищает при нормализации
     */
    private suspend fun processAlert(
        pair: CurrencyPair,
        rateType: RateType,
        currentRate: Double,
        historicalRate: Double,
        thresholdConfig: ThresholdConfig,
        alerts: MutableList<CurrencyAlert>,
        skipDedup: Boolean = false
    ) {
        val key = AlertKey(pair, thresholdConfig.period, rateType)
        val candidate = checkPairThresholds(pair, rateType, currentRate, historicalRate, thresholdConfig)

        if (candidate != null) {
            if (skipDedup) {
                alerts.add(candidate)
            } else {
                // Есть превышение порога — проверяем дедупликацию
                val lastSent = sentAlertRepository.getLastSentAlert(key).getOrNull()

                if (shouldSendAlert(candidate, lastSent, thresholdConfig)) {
                    alerts.add(candidate)
                } else {
                    logger.debug("Suppressing duplicate alert for ${pair.displayName} (${rateType.displayName}) ${thresholdConfig.period.displayName}")
                }
            }
        } else if (!skipDedup) {
            // Порог не превышен — очищаем запись если была
            sentAlertRepository.clearSentAlert(key).onFailure { error ->
                logger.error("Failed to clear sent alert for $key", error)
            }
        }
    }

    /**
     * Определяет, нужно ли отправлять алерт с учётом дедупликации.
     *
     * Правила:
     * 1. Нет предыдущего алерта → SEND
     * 2. Уровень эскалирован (WARNING → CRITICAL) → SEND
     * 3. Направление изменилось (UP → DOWN) → SEND
     * 4. Курс изменился >= re-alert threshold с момента последнего алерта → SEND
     * 5. Иначе → SUPPRESS
     */
    private fun shouldSendAlert(
        candidate: CurrencyAlert,
        lastSent: SentAlert?,
        thresholdConfig: ThresholdConfig
    ): Boolean {
        // Правило 1: нет предыдущего алерта
        if (lastSent == null) return true

        // Правило 2: эскалация уровня
        if (lastSent.level == AlertLevel.WARNING && candidate.level == AlertLevel.CRITICAL) return true

        // Правило 3: смена направления
        if (lastSent.direction != candidate.direction) return true

        // Правило 4: значительное изменение курса с момента последнего алерта
        val reAlertThreshold = reAlertThresholds[thresholdConfig.period] ?: return true
        val rateDiffPercent = abs((candidate.newRate - lastSent.rateAtAlert) / lastSent.rateAtAlert * 100.0)
        if (rateDiffPercent >= reAlertThreshold) return true

        // Правило 5: подавляем
        return false
    }

    /**
     * Проверяет пороги для одной валютной пары и типа курса
     */
    private fun checkPairThresholds(
        pair: CurrencyPair,
        rateType: RateType,
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
                "CRITICAL: ${pair.displayName} (${rateType.displayName}) changed by %.2f%% in ${thresholdConfig.period.displayName} (threshold: %.2f%%)".format(
                    changePercent,
                    thresholdConfig.criticalPercent
                )
            )
            return CurrencyAlert(
                level = AlertLevel.CRITICAL,
                period = thresholdConfig.period,
                pair = pair,
                rateType = rateType,
                direction = direction,
                changePercent = changePercent,
                oldRate = historicalRate,
                newRate = currentRate
            )
        }

        // Проверяем порог предупреждения
        if (absChangePercent >= thresholdConfig.warningPercent) {
            logger.info(
                "WARNING: ${pair.displayName} (${rateType.displayName}) changed by %.2f%% in ${thresholdConfig.period.displayName} (threshold: %.2f%%)".format(
                    changePercent,
                    thresholdConfig.warningPercent
                )
            )
            return CurrencyAlert(
                level = AlertLevel.WARNING,
                period = thresholdConfig.period,
                pair = pair,
                rateType = rateType,
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
