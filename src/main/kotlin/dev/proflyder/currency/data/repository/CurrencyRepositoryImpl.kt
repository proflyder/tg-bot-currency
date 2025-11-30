package dev.proflyder.currency.data.repository

import dev.proflyder.currency.data.remote.parser.KursKzParser
import dev.proflyder.currency.domain.model.CurrencyRate
import dev.proflyder.currency.domain.repository.CurrencyRepository

class CurrencyRepositoryImpl(
    private val parser: KursKzParser
) : CurrencyRepository {
    override suspend fun getCurrentRates(): Result<CurrencyRate> {
        return parser.parseCurrencyRates()
    }
}
