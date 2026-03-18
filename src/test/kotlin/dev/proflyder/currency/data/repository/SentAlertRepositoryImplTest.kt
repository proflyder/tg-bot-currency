package dev.proflyder.currency.data.repository

import dev.proflyder.currency.domain.model.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours

@DisplayName("SentAlertRepositoryImpl - Интеграционные тесты с H2")
class SentAlertRepositoryImplTest {

    private lateinit var repository: SentAlertRepositoryImpl

    @BeforeEach
    fun setup() {
        val dbName = "mem:sent-alert-test-${System.nanoTime()}-${Thread.currentThread().id}"
        val database = org.jetbrains.exposed.sql.Database.connect("jdbc:h2:$dbName;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        repository = SentAlertRepositoryImpl(database)
    }

    private fun createKey(
        pair: CurrencyPair = CurrencyPair.USD_TO_KZT,
        period: AlertPeriod = AlertPeriod.HOUR,
        rateType: RateType = RateType.SELL
    ) = AlertKey(pair, period, rateType)

    private fun createSentAlert(
        key: AlertKey = createKey(),
        level: AlertLevel = AlertLevel.WARNING,
        direction: ChangeDirection = ChangeDirection.UP,
        rateAtAlert: Double = 495.0,
        changePercent: Double = 2.06
    ) = SentAlert(
        key = key,
        level = level,
        direction = direction,
        rateAtAlert = rateAtAlert,
        changePercent = changePercent,
        sentAt = Clock.System.now()
    )

    @Nested
    @DisplayName("Запись и чтение алертов")
    inner class RecordAndRead {

        @Test
        fun `должен записать и прочитать алерт`() = runTest {
            val alert = createSentAlert()

            repository.recordSentAlert(alert)

            val result = repository.getLastSentAlert(alert.key)
            result.isSuccess shouldBe true
            val stored = result.getOrThrow()
            stored shouldNotBe null
            stored!!.key shouldBe alert.key
            stored.level shouldBe AlertLevel.WARNING
            stored.direction shouldBe ChangeDirection.UP
            stored.rateAtAlert shouldBe 495.0
            stored.changePercent shouldBe 2.06
        }

        @Test
        fun `должен вернуть null если алерт не найден`() = runTest {
            val result = repository.getLastSentAlert(createKey())
            result.isSuccess shouldBe true
            result.getOrThrow() shouldBe null
        }

        @Test
        fun `должен обновить существующий алерт (upsert)`() = runTest {
            val key = createKey()

            // Первая запись
            repository.recordSentAlert(createSentAlert(key = key, rateAtAlert = 490.0))

            // Обновление
            repository.recordSentAlert(createSentAlert(key = key, rateAtAlert = 498.0, level = AlertLevel.CRITICAL))

            val result = repository.getLastSentAlert(key)
            val stored = result.getOrThrow()
            stored shouldNotBe null
            stored!!.rateAtAlert shouldBe 498.0
            stored.level shouldBe AlertLevel.CRITICAL
        }

        @Test
        fun `должен хранить разные ключи независимо`() = runTest {
            val keySell = createKey(rateType = RateType.SELL)
            val keyBuy = createKey(rateType = RateType.BUY)

            repository.recordSentAlert(createSentAlert(key = keySell, rateAtAlert = 495.0))
            repository.recordSentAlert(createSentAlert(key = keyBuy, rateAtAlert = 490.0))

            val sell = repository.getLastSentAlert(keySell).getOrThrow()
            val buy = repository.getLastSentAlert(keyBuy).getOrThrow()

            sell shouldNotBe null
            buy shouldNotBe null
            sell!!.rateAtAlert shouldBe 495.0
            buy!!.rateAtAlert shouldBe 490.0
        }

        @Test
        fun `должен различать по валютной паре`() = runTest {
            val keyUsd = createKey(pair = CurrencyPair.USD_TO_KZT)
            val keyRub = createKey(pair = CurrencyPair.RUB_TO_KZT)

            repository.recordSentAlert(createSentAlert(key = keyUsd, rateAtAlert = 495.0))
            repository.recordSentAlert(createSentAlert(key = keyRub, rateAtAlert = 4.95))

            repository.getLastSentAlert(keyUsd).getOrThrow()!!.rateAtAlert shouldBe 495.0
            repository.getLastSentAlert(keyRub).getOrThrow()!!.rateAtAlert shouldBe 4.95
        }

        @Test
        fun `должен различать по периоду`() = runTest {
            val keyHour = createKey(period = AlertPeriod.HOUR)
            val keyDay = createKey(period = AlertPeriod.DAY)

            repository.recordSentAlert(createSentAlert(key = keyHour, rateAtAlert = 495.0))
            repository.recordSentAlert(createSentAlert(key = keyDay, rateAtAlert = 498.0))

            repository.getLastSentAlert(keyHour).getOrThrow()!!.rateAtAlert shouldBe 495.0
            repository.getLastSentAlert(keyDay).getOrThrow()!!.rateAtAlert shouldBe 498.0
        }
    }

    @Nested
    @DisplayName("Очистка алертов")
    inner class ClearAlerts {

        @Test
        fun `должен очистить алерт по ключу`() = runTest {
            val key = createKey()
            repository.recordSentAlert(createSentAlert(key = key))

            repository.clearSentAlert(key)

            repository.getLastSentAlert(key).getOrThrow() shouldBe null
        }

        @Test
        fun `очистка несуществующего ключа не должна падать`() = runTest {
            val result = repository.clearSentAlert(createKey())
            result.isSuccess shouldBe true
        }

        @Test
        fun `очистка одного ключа не должна затрагивать другие`() = runTest {
            val key1 = createKey(rateType = RateType.SELL)
            val key2 = createKey(rateType = RateType.BUY)

            repository.recordSentAlert(createSentAlert(key = key1))
            repository.recordSentAlert(createSentAlert(key = key2))

            repository.clearSentAlert(key1)

            repository.getLastSentAlert(key1).getOrThrow() shouldBe null
            repository.getLastSentAlert(key2).getOrThrow() shouldNotBe null
        }
    }

    @Nested
    @DisplayName("Удаление всех записей")
    inner class DeleteAll {

        @Test
        fun `должен удалить все записи`() = runTest {
            repository.recordSentAlert(createSentAlert(key = createKey(rateType = RateType.SELL)))
            repository.recordSentAlert(createSentAlert(key = createKey(rateType = RateType.BUY)))
            repository.recordSentAlert(createSentAlert(key = createKey(pair = CurrencyPair.RUB_TO_KZT)))

            val result = repository.deleteAll()
            result.isSuccess shouldBe true
            result.getOrThrow() shouldBe 3

            repository.getLastSentAlert(createKey(rateType = RateType.SELL)).getOrThrow() shouldBe null
            repository.getLastSentAlert(createKey(rateType = RateType.BUY)).getOrThrow() shouldBe null
        }

        @Test
        fun `должен вернуть 0 для пустой базы`() = runTest {
            val result = repository.deleteAll()
            result.isSuccess shouldBe true
            result.getOrThrow() shouldBe 0
        }
    }
}
