package dev.proflyder.currency.data.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Таблица для хранения истории курсов валют
 */
object CurrencyHistoryTable : Table("currency_history") {
    val id = integer("id").autoIncrement()
    val timestamp = timestamp("timestamp")
    val usdBuy = double("usd_buy")
    val usdSell = double("usd_sell")
    val rubBuy = double("rub_buy")
    val rubSell = double("rub_sell")

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, timestamp) // Индекс для быстрого поиска по времени
    }
}
