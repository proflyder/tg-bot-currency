package dev.proflyder.currency.domain.usecase

import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import dev.proflyder.currency.domain.repository.CurrencyRepository
import dev.proflyder.currency.domain.repository.TelegramRepository
import dev.proflyder.currency.util.logger
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class SendCurrencyRatesUseCase(
    private val currencyRepository: CurrencyRepository,
    private val telegramRepository: TelegramRepository,
    private val currencyHistoryRepository: CurrencyHistoryRepository,
    private val checkThresholdsUseCase: CheckCurrencyThresholdsUseCase,
    private val formatMessageUseCase: FormatCurrencyMessageUseCase
) {
    private val logger = logger()
    private val businessLogger = LoggerFactory.getLogger("business.events")

    suspend operator fun invoke(
        chatId: String,
        forceNotification: Boolean = false,
        saveToHistory: Boolean = true
    ): Result<Unit> {
        return try {
            logger.info("Fetching currency rates...")
            val ratesResult = currencyRepository.getCurrentRates()

            ratesResult.fold(
                onSuccess = { rates ->
                    logger.info("Rates fetched successfully")

                    val timestamp = Clock.System.now()

                    // Структурированное логирование курсов для мониторинга (всегда)
                    logCurrencyRate("USD", "KZT", rates.usdToKzt.buy, rates.usdToKzt.sell, timestamp)
                    logCurrencyRate("RUB", "KZT", rates.rubToKzt.buy, rates.rubToKzt.sell, timestamp)

                    // 1. Сохраняем в историю только если saveToHistory=true
                    if (saveToHistory) {
                        logger.info("Saving to history...")
                        currencyHistoryRepository.saveRecord(rates, timestamp).fold(
                            onSuccess = {
                                logger.info("History saved successfully")
                            },
                            onFailure = { error ->
                                logger.error("Failed to save history", error)
                                // Не падаем если не удалось сохранить историю
                            }
                        )
                    } else {
                        logger.info("Skipping history save (manual trigger)")
                    }

                    // 2. Проверяем трешхолды
                    logger.info("Checking thresholds...")
                    val alertsResult = checkThresholdsUseCase(rates)

                    alertsResult.fold(
                        onSuccess = { alerts ->
                            // 3. Отправляем сообщение если есть алерты ИЛИ включен forceNotification
                            val shouldSendMessage = alerts.isNotEmpty() || forceNotification

                            if (shouldSendMessage) {
                                if (forceNotification && alerts.isEmpty()) {
                                    logger.info("Force notification enabled, sending message even without alerts")
                                } else {
                                    logger.info("Found ${alerts.size} alerts, sending message to Telegram")
                                }

                                val message = formatMessageUseCase(rates, alerts)
                                logger.debug("Message content:\n$message")

                                val sendResult = telegramRepository.sendMessage(chatId, message)
                                if (sendResult.isFailure) {
                                    logger.error("Failed to send message to Telegram", sendResult.exceptionOrNull())
                                    return Result.failure(sendResult.exceptionOrNull() ?: Exception("Unknown error"))
                                }
                                logger.info("Message sent successfully")
                            } else {
                                logger.info("No thresholds exceeded, skipping Telegram notification")
                            }
                        },
                        onFailure = { error ->
                            logger.error("Failed to check thresholds", error)
                            // Не падаем, продолжаем
                        }
                    )

                    // 4. Очищаем старые записи (старше 30 дней)
                    currencyHistoryRepository.cleanOldRecords(olderThanDays = 30).fold(
                        onSuccess = { removedCount ->
                            if (removedCount > 0) {
                                logger.info("Cleaned $removedCount old records from history")
                            }
                        },
                        onFailure = { error ->
                            logger.error("Failed to clean old records", error)
                            // Не падаем если не удалось очистить
                        }
                    )

                    Result.success(Unit)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Логирует курс валюты в структурированном формате для мониторинга
     */
    private fun logCurrencyRate(
        currencyFrom: String,
        currencyTo: String,
        buyRate: Double,
        sellRate: Double,
        timestamp: kotlinx.datetime.Instant
    ) {
        try {
            // Structured fields для JSON логирования
            MDC.put("event", "currency_rate_updated")
            MDC.put("currency_from", currencyFrom)
            MDC.put("currency_to", currencyTo)
            MDC.put("rate_buy", buyRate.toString())
            MDC.put("rate_sell", sellRate.toString())
            MDC.put("timestamp", timestamp.toEpochMilliseconds().toString())
            MDC.put("log_type", "business_metric")

            businessLogger.info("CURRENCY_RATE_UPDATED - $currencyFrom/$currencyTo buy=$buyRate sell=$sellRate")
        } finally {
            MDC.clear()
        }
    }
}
