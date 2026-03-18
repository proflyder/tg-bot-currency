package dev.proflyder.currency.data.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Таблица для хранения отправленных алертов (дедупликация)
 */
object SentAlertTable : Table("sent_alerts") {
    val id = integer("id").autoIncrement()
    val pair = varchar("pair", 20)
    val period = varchar("period", 10)
    val rateType = varchar("rate_type", 10)
    val level = varchar("level", 10)
    val direction = varchar("direction", 10)
    val rateAtAlert = double("rate_at_alert")
    val changePercent = double("change_percent")
    val sentAt = timestamp("sent_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(pair, period, rateType)
    }
}
