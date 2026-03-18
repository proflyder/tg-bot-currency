package dev.proflyder.currency.data.repository

import dev.proflyder.currency.data.database.SentAlertTable
import dev.proflyder.currency.domain.model.*
import dev.proflyder.currency.domain.repository.SentAlertRepository
import dev.proflyder.currency.util.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

class SentAlertRepositoryImpl(
    databasePath: String
) : SentAlertRepository {

    private val logger = logger()
    internal val database: Database

    init {
        if (!databasePath.startsWith("mem:")) {
            val dbFile = File(databasePath)
            dbFile.parentFile?.mkdirs()
        }

        val jdbcUrl = if (databasePath.startsWith("mem:")) {
            "jdbc:h2:$databasePath;DB_CLOSE_DELAY=-1"
        } else {
            val normalizedPath = when {
                databasePath.startsWith("/") -> databasePath
                databasePath.startsWith("~") -> databasePath
                databasePath.startsWith("./") -> databasePath
                databasePath.matches(Regex("^[A-Za-z]:.*")) -> databasePath
                else -> "./$databasePath"
            }
            "jdbc:h2:file:$normalizedPath;DB_CLOSE_DELAY=-1"
        }

        database = Database.connect(
            url = jdbcUrl,
            driver = "org.h2.Driver"
        )

        transaction(database) {
            SchemaUtils.create(SentAlertTable)
        }

        logger.info("SentAlert database initialized at: $databasePath")
    }

    override suspend fun getLastSentAlert(key: AlertKey): Result<SentAlert?> = runCatching {
        withContext(Dispatchers.IO) {
            transaction(database) {
                SentAlertTable
                    .select {
                        (SentAlertTable.pair eq key.pair.name) and
                                (SentAlertTable.period eq key.period.name) and
                                (SentAlertTable.rateType eq key.rateType.name)
                    }
                    .firstOrNull()
                    ?.let { row ->
                        SentAlert(
                            key = key,
                            level = AlertLevel.valueOf(row[SentAlertTable.level]),
                            direction = ChangeDirection.valueOf(row[SentAlertTable.direction]),
                            rateAtAlert = row[SentAlertTable.rateAtAlert],
                            changePercent = row[SentAlertTable.changePercent],
                            sentAt = row[SentAlertTable.sentAt]
                        )
                    }
            }
        }
    }

    override suspend fun recordSentAlert(alert: SentAlert): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            transaction(database) {
                // Upsert: delete existing + insert
                SentAlertTable.deleteWhere {
                    (pair eq alert.key.pair.name) and
                            (period eq alert.key.period.name) and
                            (rateType eq alert.key.rateType.name)
                }

                SentAlertTable.insert {
                    it[pair] = alert.key.pair.name
                    it[period] = alert.key.period.name
                    it[rateType] = alert.key.rateType.name
                    it[level] = alert.level.name
                    it[direction] = alert.direction.name
                    it[rateAtAlert] = alert.rateAtAlert
                    it[changePercent] = alert.changePercent
                    it[sentAt] = alert.sentAt
                }
            }
        }
    }

    override suspend fun clearSentAlert(key: AlertKey): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            transaction(database) {
                SentAlertTable.deleteWhere {
                    (pair eq key.pair.name) and
                            (period eq key.period.name) and
                            (rateType eq key.rateType.name)
                }
            }
        }
    }

    override suspend fun deleteAll(): Result<Int> = runCatching {
        withContext(Dispatchers.IO) {
            transaction(database) {
                SentAlertTable.deleteAll()
            }
        }
    }
}
