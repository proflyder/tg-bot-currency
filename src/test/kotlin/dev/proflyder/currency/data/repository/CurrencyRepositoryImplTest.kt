package dev.proflyder.currency.data.repository

import dev.proflyder.currency.TestFixtures
import dev.proflyder.currency.data.remote.parser.KursKzParser
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("CurrencyRepositoryImpl")
class CurrencyRepositoryImplTest {

    private lateinit var parser: KursKzParser
    private lateinit var repository: CurrencyRepositoryImpl

    @BeforeEach
    fun setup() {
        parser = mockk()
        repository = CurrencyRepositoryImpl(parser)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `должен делегировать вызов парсеру при успешном парсинге`() = runTest {
        // Arrange
        val expectedRates = TestFixtures.sampleCurrencyRate
        coEvery { parser.parseCurrencyRates() } returns Result.success(expectedRates)

        // Act
        val result = repository.getCurrentRates()

        // Assert
        result.isSuccess shouldBe true
        result.getOrNull() shouldBe expectedRates
        coVerify(exactly = 1) { parser.parseCurrencyRates() }
    }

    @Test
    fun `должен вернуть ошибку если парсер вернул ошибку`() = runTest {
        // Arrange
        val error = Exception("Parsing failed")
        coEvery { parser.parseCurrencyRates() } returns Result.failure(error)

        // Act
        val result = repository.getCurrentRates()

        // Assert
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldBe "Parsing failed"
        coVerify(exactly = 1) { parser.parseCurrencyRates() }
    }

    @Test
    fun `должен вызывать парсер каждый раз при вызове getCurrentRates`() = runTest {
        // Arrange
        val rates = TestFixtures.sampleCurrencyRate
        coEvery { parser.parseCurrencyRates() } returns Result.success(rates)

        // Act
        repository.getCurrentRates()
        repository.getCurrentRates()
        repository.getCurrentRates()

        // Assert - парсер должен быть вызван 3 раза (нет кэширования)
        coVerify(exactly = 3) { parser.parseCurrencyRates() }
    }
}
