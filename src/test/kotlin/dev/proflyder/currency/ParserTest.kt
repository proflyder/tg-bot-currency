package dev.proflyder.currency

import dev.proflyder.currency.data.remote.parser.KursKzParser
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * Тестовый скрипт для проверки парсера
 * Запусти: ./gradlew test --tests ParserTest
 */
fun main() = runBlocking {
    println("=".repeat(80))
    println("Testing KursKzParser")
    println("=".repeat(80))

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    val parser = KursKzParser(httpClient)

    println("\n[1] Fetching currency rates from kurs.kz...\n")

    val result = parser.parseCurrencyRates()

    result.fold(
        onSuccess = { rates ->
            println("\n✓ SUCCESS! Parsed rates:\n")
            println("  USD → KZT:")
            println("    Buy:  ${String.format("%.2f", rates.usdToKzt.buy)} ₸")
            println("    Sell: ${String.format("%.2f", rates.usdToKzt.sell)} ₸")
            println()
            println("  RUB → KZT:")
            println("    Buy:  ${String.format("%.2f", rates.rubToKzt.buy)} ₸")
            println("    Sell: ${String.format("%.2f", rates.rubToKzt.sell)} ₸")
            println()
            println("=".repeat(80))
            println("✓ Parser is working correctly!")
            println("=".repeat(80))
        },
        onFailure = { error ->
            println("\n✗ FAILED!")
            println("Error: ${error.message}")
            error.printStackTrace()
            println()
            println("=".repeat(80))
        }
    )

    httpClient.close()
}
