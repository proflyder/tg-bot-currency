package dev.proflyder.currency.domain.repository

import dev.proflyder.currency.domain.model.CurrencyRate
import dev.proflyder.currency.domain.model.CurrencyRateRecord
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * Репозиторий для работы с историей курсов валют
 * Хранит историю в JSON файле без использования базы данных
 */
interface CurrencyHistoryRepository {
    /**
     * Сохранить новую запись о курсах
     * @param rates Курсы валют
     * @param timestamp Время парсинга
     */
    suspend fun saveRecord(rates: CurrencyRate, timestamp: Instant): Result<Unit>

    /**
     * Очистить записи старше указанного количества дней
     * @param olderThanDays Количество дней (по умолчанию 30)
     */
    suspend fun cleanOldRecords(olderThanDays: Int = 30): Result<Int>

    /**
     * Получить запись о курсах за указанный период назад
     * @param duration Период назад (например, 1 час, 1 день)
     * @return Запись о курсах или null если не найдено
     */
    suspend fun getRecordBefore(duration: Duration): Result<CurrencyRateRecord?>

    /**
     * Получить все записи истории курсов
     * @return Список всех записей, отсортированный по времени (от новых к старым)
     */
    suspend fun getAllRecords(): Result<List<CurrencyRateRecord>>

    /**
     * Получить последнюю (самую свежую) запись курсов
     * @return Последняя запись или null если история пуста
     */
    suspend fun getLatestRecord(): Result<CurrencyRateRecord?>
}
