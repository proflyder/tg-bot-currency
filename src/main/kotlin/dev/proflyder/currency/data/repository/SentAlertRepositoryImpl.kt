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

class SentAlertRepositoryImpl(
    internal val database: Database
) : SentAlertRepository {

    private val logger = logger()

    init {
        transaction(database) {
            SchemaUtils.create(SentAlertTable)
        }
        logger.info("SentAlertTable initialized")
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
