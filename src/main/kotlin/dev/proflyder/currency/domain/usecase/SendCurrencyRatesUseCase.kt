package dev.proflyder.currency.domain.usecase

import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import dev.proflyder.currency.domain.repository.CurrencyRepository
import dev.proflyder.currency.domain.repository.TelegramRepository
import dev.proflyder.currency.util.logger
import kotlinx.datetime.Clock

class SendCurrencyRatesUseCase(
    private val currencyRepository: CurrencyRepository,
    private val telegramRepository: TelegramRepository,
    private val currencyHistoryRepository: CurrencyHistoryRepository,
    private val checkThresholdsUseCase: CheckCurrencyThresholdsUseCase,
    private val formatMessageUseCase: FormatCurrencyMessageUseCase
) {
    private val logger = logger()

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
}
