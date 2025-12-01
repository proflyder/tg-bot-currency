package dev.proflyder.currency.data.repository

import dev.proflyder.currency.data.database.CurrencyHistoryTable
import dev.proflyder.currency.domain.model.CurrencyRate
import dev.proflyder.currency.domain.model.CurrencyRateRecord
import dev.proflyder.currency.domain.model.CurrencyRateSnapshot
import dev.proflyder.currency.domain.model.ExchangeRateSnapshot
import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import dev.proflyder.currency.util.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Реализация репозитория истории курсов с сохранением в H2 Database
 * @param databasePath Путь к файлу БД (например, ./data/currency-history или /app/data/currency-history)
 */
class CurrencyHistoryRepositoryImpl(
    databasePath: String
) : CurrencyHistoryRepository {

    private val logger = logger()

    private val database: Database


    init {
        // Создаем директорию для БД если не существует (только для file mode)
        if (!databasePath.startsWith("mem:")) {
            val dbFile = File(databasePath)
            dbFile.parentFile?.mkdirs()
        }

        // Подключаемся к H2
        val jdbcUrl = if (databasePath.startsWith("mem:")) {
            "jdbc:h2:$databasePath;DB_CLOSE_DELAY=-1"
        } else {
            // H2 2.x требует явного указания относительного пути с ./
            val normalizedPath = when {
                databasePath.startsWith("/") -> databasePath  // Абсолютный Unix путь
                databasePath.startsWith("~") -> databasePath  // Домашняя директория
                databasePath.startsWith("./") -> databasePath // Уже явно относительный
                databasePath.matches(Regex("^[A-Za-z]:.*")) -> databasePath // Windows абсолютный путь
                else -> "./$databasePath" // Добавляем ./ к относительному пути
            }
            "jdbc:h2:file:$normalizedPath;DB_CLOSE_DELAY=-1"
        }

        database = Database.connect(
            url = jdbcUrl,
            driver = "org.h2.Driver"
        )

        // Создаем таблицу если не существует
        transaction(database) {
            SchemaUtils.create(CurrencyHistoryTable)
        }

        logger.info("H2 Database initialized at: $databasePath")
    }

    override suspend fun saveRecord(rates: CurrencyRate, timestamp: Instant): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            transaction(database) {
                logger.info("Saving currency rates to H2 database")

                CurrencyHistoryTable.insert {
                    it[CurrencyHistoryTable.timestamp] = timestamp
                    it[usdBuy] = rates.usdToKzt.buy
                    it[usdSell] = rates.usdToKzt.sell
                    it[rubBuy] = rates.rubToKzt.buy
                    it[rubSell] = rates.rubToKzt.sell
                }

                val totalRecords = CurrencyHistoryTable.selectAll().count()
                logger.info("Currency rates saved successfully. Total records: $totalRecords")
            }
        }
    }


    override suspend fun cleanOldRecords(olderThanDays: Int): Result<Int> = runCatching {
        withContext(Dispatchers.IO) {
            transaction(database) {
                logger.info("Cleaning records older than $olderThanDays days from H2 database")

                // Вычисляем граничную дату
                val cutoffTime = Clock.System.now() - olderThanDays.days

                // Удаляем старые записи
                val deletedCount = CurrencyHistoryTable.deleteWhere {
                    timestamp lessEq cutoffTime
                }

                val remainingCount = CurrencyHistoryTable.selectAll().count()

                if (deletedCount > 0) {
                    logger.info("Cleaned $deletedCount old records. Remaining: $remainingCount")
                } else {
                    logger.info("No old records to clean. Total records: $remainingCount")
                }

                deletedCount.toInt()
            }
        }
    }

    override suspend fun getRecordBefore(duration: Duration): Result<CurrencyRateRecord?> = runCatching {
        withContext(Dispatchers.IO) {
            transaction(database) {
                logger.debug("Looking for record before $duration from H2 database")

                // Вычисляем граничное время
                val targetTime = Clock.System.now() - duration

                // Ищем запись ближайшую к целевому времени
                val row = CurrencyHistoryTable
                    .select { CurrencyHistoryTable.timestamp lessEq targetTime }
                    .orderBy(CurrencyHistoryTable.timestamp, SortOrder.DESC)
                    .limit(1)
                    .firstOrNull()

                if (row != null) {
                    val record = CurrencyRateRecord(
                        timestamp = row[CurrencyHistoryTable.timestamp],
                        rates = CurrencyRateSnapshot(
                            usdToKzt = ExchangeRateSnapshot(
                                buy = row[CurrencyHistoryTable.usdBuy],
                                sell = row[CurrencyHistoryTable.usdSell]
                            ),
                            rubToKzt = ExchangeRateSnapshot(
                                buy = row[CurrencyHistoryTable.rubBuy],
                                sell = row[CurrencyHistoryTable.rubSell]
                            )
                        )
                    )
                    logger.debug("Found record at ${record.timestamp}")
                    record
                } else {
                    logger.debug("No record found before $duration")
                    null
                }
            }
        }
    }

    override suspend fun getAllRecords(): Result<List<CurrencyRateRecord>> = runCatching {
        withContext(Dispatchers.IO) {
            transaction(database) {
                logger.info("Fetching all currency history records from H2 database")

                val records = CurrencyHistoryTable
                    .selectAll()
                    .orderBy(CurrencyHistoryTable.timestamp, SortOrder.DESC)
                    .map { row ->
                        CurrencyRateRecord(
                            timestamp = row[CurrencyHistoryTable.timestamp],
                            rates = CurrencyRateSnapshot(
                                usdToKzt = ExchangeRateSnapshot(
                                    buy = row[CurrencyHistoryTable.usdBuy],
                                    sell = row[CurrencyHistoryTable.usdSell]
                                ),
                                rubToKzt = ExchangeRateSnapshot(
                                    buy = row[CurrencyHistoryTable.rubBuy],
                                    sell = row[CurrencyHistoryTable.rubSell]
                                )
                            )
                        )
                    }

                logger.info("Fetched ${records.size} currency history records")
                records
            }
        }
    }
}
