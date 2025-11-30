package dev.proflyder.currency.data.remote.parser

import dev.proflyder.currency.domain.model.CurrencyRate
import dev.proflyder.currency.domain.model.ExchangeRate
import dev.proflyder.currency.util.logger
import dev.proflyder.currency.util.logWithTiming
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*

class KursKzParser(
    private val httpClient: HttpClient
) {
    private val baseUrl = "https://kurs.kz"
    private val json = Json { ignoreUnknownKeys = true }
    private val logger = logger()

    suspend fun parseCurrencyRates(): Result<CurrencyRate> {
        return try {
            logger.logWithTiming("Parsing currency rates from $baseUrl") {
                val html = httpClient.get(baseUrl).bodyAsText()
                logger.debug("Received HTML response, size: ${html.length} bytes")

                // Извлекаем JSON из JavaScript переменной: var punkts = [...]
                val punktsRegex = Regex("""var punkts = (\[.*?]);""", RegexOption.DOT_MATCHES_ALL)
                val matchResult = punktsRegex.find(html)
                    ?: throw Exception("Could not find 'var punkts' in HTML")

                val jsonString = matchResult.groupValues[1]
                logger.debug("Extracted JSON string, size: ${jsonString.length} bytes")

                val punktsArray = json.parseToJsonElement(jsonString).jsonArray
                logger.debug("Parsed ${punktsArray.size} exchange points")

                // Берём средние курсы из всех обменников
                val rates = calculateAverageRates(punktsArray)
                logger.info("✓ Successfully calculated average rates:")
                logger.info("  USD/KZT: buy=%.2f, sell=%.2f".format(rates.usdToKzt.buy, rates.usdToKzt.sell))
                logger.info("  RUB/KZT: buy=%.2f, sell=%.2f".format(rates.rubToKzt.buy, rates.rubToKzt.sell))

                Result.success(rates)
            }
        } catch (e: Exception) {
            logger.error("Failed to parse currency rates", e)
            Result.failure(e)
        }
    }

    private fun calculateAverageRates(punkts: JsonArray): CurrencyRate {
        val usdRates = mutableListOf<Pair<Double, Double>>()
        val rubRates = mutableListOf<Pair<Double, Double>>()

        // Собираем все курсы из обменников
        for ((index, punkt) in punkts.withIndex()) {
            val data = punkt.jsonObject["data"]?.jsonObject ?: continue
            val name = punkt.jsonObject["name"]?.jsonPrimitive?.contentOrNull ?: "Unknown"

            // USD курсы
            data["USD"]?.jsonArray?.let { usd ->
                if (usd.size >= 2) {
                    val buy = usd[0].jsonPrimitive.doubleOrNull ?: return@let
                    val sell = usd[1].jsonPrimitive.doubleOrNull ?: return@let

                    // Фильтруем нулевые и некорректные значения
                    if (buy > 0 && sell > 0) {
                        usdRates.add(buy to sell)

                        // Логируем первые 3 обменника для проверки
                        if (index < 3) {
                            logger.info("Exchange point '$name': USD buy=$buy, sell=$sell")
                        }
                    }
                }
            }

            // RUB курсы
            data["RUB"]?.jsonArray?.let { rub ->
                if (rub.size >= 2) {
                    val buy = rub[0].jsonPrimitive.doubleOrNull ?: return@let
                    val sell = rub[1].jsonPrimitive.doubleOrNull ?: return@let

                    // Фильтруем нулевые и некорректные значения
                    if (buy > 0 && sell > 0) {
                        rubRates.add(buy to sell)

                        // Логируем первые 3 обменника для проверки
                        if (index < 3) {
                            logger.info("Exchange point '$name': RUB buy=$buy, sell=$sell")
                        }
                    }
                }
            }
        }

        logger.info("Collected USD rates from ${usdRates.size} exchange points")
        logger.info("Collected RUB rates from ${rubRates.size} exchange points")

        if (usdRates.isEmpty()) {
            logger.error("No USD rates found in response")
            throw Exception("No USD rates found")
        }
        if (rubRates.isEmpty()) {
            logger.error("No RUB rates found in response")
            throw Exception("No RUB rates found")
        }

        // Вычисляем средние значения
        val avgUsdBuy = usdRates.map { it.first }.average()
        val avgUsdSell = usdRates.map { it.second }.average()
        val avgRubBuy = rubRates.map { it.first }.average()
        val avgRubSell = rubRates.map { it.second }.average()

        logger.info("Average USD rates: buy=%.2f, sell=%.2f".format(avgUsdBuy, avgUsdSell))
        logger.info("Average RUB rates: buy=%.2f, sell=%.2f".format(avgRubBuy, avgRubSell))

        return CurrencyRate(
            usdToKzt = ExchangeRate(buy = avgUsdBuy, sell = avgUsdSell),
            rubToKzt = ExchangeRate(buy = avgRubBuy, sell = avgRubSell)
        )
    }
}
