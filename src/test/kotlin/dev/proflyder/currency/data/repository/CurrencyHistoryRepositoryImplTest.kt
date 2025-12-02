package dev.proflyder.currency.data.repository

import dev.proflyder.currency.TestFixtures
import dev.proflyder.currency.data.database.CurrencyHistoryTable
import dev.proflyder.currency.domain.model.CurrencyRate
import dev.proflyder.currency.domain.model.ExchangeRate
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@DisplayName("CurrencyHistoryRepositoryImpl - Интеграционные тесты с H2")
class CurrencyHistoryRepositoryImplTest {

    private lateinit var repository: CurrencyHistoryRepositoryImpl

    @BeforeEach
    fun setup() {
        // Создаем уникальную in-memory H2 для каждого теста
        val testDbPath = "mem:test-${System.nanoTime()}-${Thread.currentThread().id}"
        repository = CurrencyHistoryRepositoryImpl(testDbPath)
    }

    @Nested
    @DisplayName("Сохранение записей")
    inner class SaveRecords {

        @Test
        fun `должен создать таблицу и сохранить запись`() = runTest {
            // Arrange
            val rates = TestFixtures.sampleCurrencyRate
            val timestamp = TestFixtures.sampleTimestamp

            // Act
            val result = repository.saveRecord(rates, timestamp)

            // Assert
            result.isSuccess shouldBe true

            // Assert через repository
            result.isSuccess shouldBe true
        }

        @Test
        fun `должен добавить новую запись к существующим`() = runTest {
            // Arrange
            val rates1 = TestFixtures.sampleCurrencyRate
            val timestamp1 = Instant.parse("2025-11-30T10:00:00Z")

            val rates2 = TestFixtures.sampleCurrencyRate
            val timestamp2 = Instant.parse("2025-11-30T11:00:00Z")

            // Act
            val result1 = repository.saveRecord(rates1, timestamp1)
            val result2 = repository.saveRecord(rates2, timestamp2)

            // Assert
            result1.isSuccess shouldBe true
            result2.isSuccess shouldBe true
        }

        @Test
        fun `должен сохранять корректные значения курсов`() = runTest {
            // Arrange
            val rates = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 480.0, sell = 485.0),
                rubToKzt = ExchangeRate(buy = 4.80, sell = 4.90)
            )
            val timestamp = TestFixtures.sampleTimestamp

            // Act
            val result = repository.saveRecord(rates, timestamp)

            // Assert
            result.isSuccess shouldBe true

            // Проверяем через getRecordBefore
            val record = repository.getRecordBefore(0.hours).getOrThrow()
            record shouldNotBe null
            record!!.rates.usdToKzt.buy shouldBe 480.0
            record.rates.usdToKzt.sell shouldBe 485.0
            record.rates.rubToKzt.buy shouldBe 4.80
            record.rates.rubToKzt.sell shouldBe 4.90
        }
    }

    @Nested
    @DisplayName("Очистка старых записей")
    inner class CleanOldRecords {

        @Test
        fun `должен удалить записи старше 30 дней`() = runTest {
            // Arrange - создаем записи разного возраста
            val now = Clock.System.now()
            val old1 = now - 31.days
            val old2 = now - 35.days
            val recent1 = now - 10.days
            val recent2 = now - 5.days

            repository.saveRecord(TestFixtures.sampleCurrencyRate, old1)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, old2)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, recent1)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, recent2)

            // Act
            val result = repository.cleanOldRecords(olderThanDays = 30)

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe 2 // Удалено 2 старых записи
        }

        @Test
        fun `не должен удалять записи если все свежие`() = runTest {
            // Arrange
            val now = Clock.System.now()
            val recent1 = now - 10.days
            val recent2 = now - 5.days

            repository.saveRecord(TestFixtures.sampleCurrencyRate, recent1)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, recent2)

            // Act
            val result = repository.cleanOldRecords(olderThanDays = 30)

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe 0 // Ничего не удалено
        }

        @Test
        fun `должен работать с пустой БД`() = runTest {
            // Act
            val result = repository.cleanOldRecords(olderThanDays = 30)

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe 0
        }

        @Test
        fun `должен поддерживать настраиваемый период очистки`() = runTest {
            // Arrange
            val now = Clock.System.now()
            val old = now - 8.days
            val recent = now - 3.days

            repository.saveRecord(TestFixtures.sampleCurrencyRate, old)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, recent)

            // Act - очистка записей старше 7 дней
            val result = repository.cleanOldRecords(olderThanDays = 7)

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe 1 // Удалена 1 запись
        }
    }

    @Nested
    @DisplayName("Получение записей за период")
    inner class GetRecordBefore {

        @Test
        fun `должен вернуть запись за указанный период назад`() = runTest {
            // Arrange
            val now = Clock.System.now()
            val twoHoursAgo = now - 2.hours
            val oneDayAgo = now - 1.days

            // Сохраняем несколько записей
            repository.saveRecord(TestFixtures.sampleCurrencyRate, oneDayAgo)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, twoHoursAgo)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, now)

            // Act - ищем запись за 1.5 часа назад
            val result = repository.getRecordBefore(1.5.hours)

            // Assert
            result.isSuccess shouldBe true
            val record = result.getOrThrow()
            record shouldNotBe null
            // Должна вернуться запись 2 часа назад (ближайшая к 1.5 часам назад)
            record!!.timestamp shouldBe twoHoursAgo
        }

        @Test
        fun `должен вернуть ближайшую запись к целевому времени`() = runTest {
            // Arrange
            val now = Clock.System.now()
            val fiveHoursAgo = now - 5.hours
            val threeHoursAgo = now - 3.hours
            val oneHourAgo = now - 1.hours

            repository.saveRecord(TestFixtures.sampleCurrencyRate, fiveHoursAgo)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, threeHoursAgo)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, oneHourAgo)

            // Act - ищем запись за 4 часа назад
            val result = repository.getRecordBefore(4.hours)

            // Assert
            result.isSuccess shouldBe true
            val record = result.getOrThrow()
            record shouldNotBe null
            // Должна вернуться запись 5 часов назад (самая свежая из подходящих)
            record!!.timestamp shouldBe fiveHoursAgo
        }

        @Test
        fun `должен вернуть null если нет записей в истории`() = runTest {
            // Act
            val result = repository.getRecordBefore(1.hours)

            // Assert
            result.isSuccess shouldBe true
            result.getOrThrow() shouldBe null
        }

        @Test
        fun `должен вернуть null если все записи новее целевого времени`() = runTest {
            // Arrange
            val now = Clock.System.now()
            val thirtyMinutesAgo = now - 30.minutes

            // Сохраняем запись 30 минут назад
            repository.saveRecord(TestFixtures.sampleCurrencyRate, thirtyMinutesAgo)

            // Act - ищем запись за 2 часа назад (но таких нет)
            val result = repository.getRecordBefore(2.hours)

            // Assert
            result.isSuccess shouldBe true
            result.getOrThrow() shouldBe null
        }

        @Test
        fun `должен вернуть самую свежую запись из подходящих`() = runTest {
            // Arrange
            val now = Clock.System.now()
            val tenHoursAgo = now - 10.hours
            val eightHoursAgo = now - 8.hours
            val sixHoursAgo = now - 6.hours
            val fourHoursAgo = now - 4.hours

            repository.saveRecord(TestFixtures.sampleCurrencyRate, tenHoursAgo)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, eightHoursAgo)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, sixHoursAgo)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, fourHoursAgo)

            // Act - ищем запись за 5 часов назад
            val result = repository.getRecordBefore(5.hours)

            // Assert
            result.isSuccess shouldBe true
            val record = result.getOrThrow()
            record shouldNotBe null
            // Должна вернуться запись 6 часов назад (самая свежая из тех что <= 5 часов назад)
            record!!.timestamp shouldBe sixHoursAgo
        }

        @Test
        fun `должен корректно работать с периодом 1 день`() = runTest {
            // Arrange
            val now = Clock.System.now()
            val twoDaysAgo = now - 2.days
            val oneDayAgo = now - 1.days
            val twelveHoursAgo = now - 12.hours

            repository.saveRecord(TestFixtures.sampleCurrencyRate, twoDaysAgo)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, oneDayAgo)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, twelveHoursAgo)

            // Act
            val result = repository.getRecordBefore(24.hours)

            // Assert
            result.isSuccess shouldBe true
            val record = result.getOrThrow()
            record shouldNotBe null
            record!!.timestamp shouldBe oneDayAgo
        }

        @Test
        fun `должен корректно работать с периодом 1 неделя`() = runTest {
            // Arrange
            val now = Clock.System.now()
            val tenDaysAgo = now - 10.days
            val sevenDaysAgo = now - 7.days
            val threeDaysAgo = now - 3.days

            repository.saveRecord(TestFixtures.sampleCurrencyRate, tenDaysAgo)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, sevenDaysAgo)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, threeDaysAgo)

            // Act
            val result = repository.getRecordBefore(168.hours) // 7 дней

            // Assert
            result.isSuccess shouldBe true
            val record = result.getOrThrow()
            record shouldNotBe null
            record!!.timestamp shouldBe sevenDaysAgo
        }

        @Test
        fun `должен вернуть правильные данные о курсах`() = runTest {
            // Arrange
            val now = Clock.System.now()
            val oneHourAgo = now - 1.hours

            val customRate = CurrencyRate(
                usdToKzt = ExchangeRate(buy = 480.0, sell = 485.0),
                rubToKzt = ExchangeRate(buy = 4.80, sell = 4.90)
            )

            repository.saveRecord(customRate, oneHourAgo)

            // Act
            val result = repository.getRecordBefore(30.minutes)

            // Assert
            result.isSuccess shouldBe true
            val record = result.getOrThrow()
            record shouldNotBe null
            record!!.rates.usdToKzt.buy shouldBe 480.0
            record.rates.usdToKzt.sell shouldBe 485.0
            record.rates.rubToKzt.buy shouldBe 4.80
            record.rates.rubToKzt.sell shouldBe 4.90
        }
    }

    @Nested
    @DisplayName("Удаление всех записей")
    inner class DeleteAll {

        @Test
        fun `должен удалить все записи из базы данных`() = runTest {
            // Arrange - создаем несколько записей
            val now = Clock.System.now()
            repository.saveRecord(TestFixtures.sampleCurrencyRate, now - 1.hours)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, now - 2.hours)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, now - 3.hours)

            // Act
            val result = repository.deleteAll()

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe 3 // Удалено 3 записи

            // Проверяем, что база действительно пустая
            val allRecords = repository.getAllRecords().getOrThrow()
            allRecords.size shouldBe 0
        }

        @Test
        fun `должен вернуть 0 если база данных уже пустая`() = runTest {
            // Act - удаляем из пустой базы
            val result = repository.deleteAll()

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe 0
        }

        @Test
        fun `должен удалить одну запись если она одна`() = runTest {
            // Arrange
            repository.saveRecord(TestFixtures.sampleCurrencyRate, Clock.System.now())

            // Act
            val result = repository.deleteAll()

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe 1

            // Проверяем, что база пустая
            val allRecords = repository.getAllRecords().getOrThrow()
            allRecords.size shouldBe 0
        }

        @Test
        fun `должен удалить большое количество записей`() = runTest {
            // Arrange - создаем 100 записей
            val now = Clock.System.now()
            repeat(100) { i ->
                repository.saveRecord(TestFixtures.sampleCurrencyRate, now - (i * 10).minutes)
            }

            // Act
            val result = repository.deleteAll()

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe 100

            // Проверяем, что все удалены
            val allRecords = repository.getAllRecords().getOrThrow()
            allRecords.size shouldBe 0
        }

        @Test
        fun `должен корректно работать при повторном вызове`() = runTest {
            // Arrange
            repository.saveRecord(TestFixtures.sampleCurrencyRate, Clock.System.now())

            // Act - первый вызов
            val result1 = repository.deleteAll()

            // Assert первый вызов
            result1.isSuccess shouldBe true
            result1.getOrNull() shouldBe 1

            // Act - второй вызов на пустой базе
            val result2 = repository.deleteAll()

            // Assert второй вызов
            result2.isSuccess shouldBe true
            result2.getOrNull() shouldBe 0
        }

        @Test
        fun `должен удалить все записи независимо от возраста`() = runTest {
            // Arrange - создаем записи разного возраста
            val now = Clock.System.now()
            repository.saveRecord(TestFixtures.sampleCurrencyRate, now - 100.days) // Очень старая
            repository.saveRecord(TestFixtures.sampleCurrencyRate, now - 50.days)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, now - 1.days)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, now) // Свежая

            // Act
            val result = repository.deleteAll()

            // Assert - все 4 записи удалены
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe 4

            val allRecords = repository.getAllRecords().getOrThrow()
            allRecords.size shouldBe 0
        }

        @Test
        fun `после удаления можно снова добавлять записи`() = runTest {
            // Arrange
            repository.saveRecord(TestFixtures.sampleCurrencyRate, Clock.System.now())
            repository.deleteAll()

            // Act - добавляем новую запись после очистки
            val newTimestamp = Clock.System.now()
            val saveResult = repository.saveRecord(TestFixtures.sampleCurrencyRate, newTimestamp)

            // Assert
            saveResult.isSuccess shouldBe true

            val allRecords = repository.getAllRecords().getOrThrow()
            allRecords.size shouldBe 1
            allRecords.first().timestamp shouldBe newTimestamp
        }

        @Test
        fun `должен корректно возвращать количество удаленных записей через прямую проверку БД`() = runTest {
            // Arrange
            val now = Clock.System.now()
            repository.saveRecord(TestFixtures.sampleCurrencyRate, now - 1.hours)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, now - 2.hours)
            repository.saveRecord(TestFixtures.sampleCurrencyRate, now - 3.hours)

            // Проверяем количество записей до удаления через прямой SQL
            val countBefore = transaction(repository.database) {
                CurrencyHistoryTable.selectAll().count()
            }
            countBefore shouldBe 3

            // Act
            val result = repository.deleteAll()

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe 3

            // Проверяем количество записей после удаления через прямой SQL
            val countAfter = transaction(repository.database) {
                CurrencyHistoryTable.selectAll().count()
            }
            countAfter shouldBe 0
        }
    }
}
