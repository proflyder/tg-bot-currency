package dev.proflyder.currency.di

data class AppConfig(
    val botToken: String,
    val chatId: String,
    val schedulerCron: String,
    val databasePath: String,
    val unkeyRootKey: String,
    val internalApiKey: String  // API ключ для вызова внутренних endpoints
)
