package dev.proflyder.currency.domain.repository

import dev.proflyder.currency.domain.model.CurrencyRate

interface CurrencyRepository {
    suspend fun getCurrentRates(): Result<CurrencyRate>
}
