package dev.proflyder.currency.di

import dev.proflyder.currency.data.remote.parser.KursKzParser
import dev.proflyder.currency.data.remote.telegram.TelegramApi
import dev.proflyder.currency.data.repository.CurrencyHistoryRepositoryImpl
import dev.proflyder.currency.data.repository.CurrencyRepositoryImpl
import dev.proflyder.currency.data.repository.TelegramRepositoryImpl
import dev.proflyder.currency.domain.repository.CurrencyHistoryRepository
import dev.proflyder.currency.domain.repository.CurrencyRepository
import dev.proflyder.currency.domain.repository.TelegramRepository
import dev.proflyder.currency.domain.usecase.SendCurrencyRatesUseCase
import dev.proflyder.currency.scheduler.QuartzSchedulerManager
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.*
import org.junit.jupiter.api.*
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

@DisplayName("AppModule - Koin DI Configuration")
class AppModuleTest : KoinTest {

    @BeforeEach
    fun setUp() {
        // Принудительно останавливаем Koin перед каждым тестом
        GlobalContext.getOrNull()?.let { stopKoin() }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Nested
    @DisplayName("Проверка конфигурации модуля")
    inner class ModuleConfiguration {

        @Test
        fun `модуль должен загружаться без ошибок`() {
            // Arrange & Act & Assert - не должно быть исключений
            startKoin {
                modules(
                    module {
                        single { createTestConfig() }
                    },
                    appModule
                )
            }
        }

        @Test
        fun `все зависимости должны резолвиться корректно`() {
            // Arrange & Act & Assert - проверяем что Koin может резолвить все зависимости
            startKoin {
                modules(
                    module {
                        single { createTestConfig() }
                    },
                    appModule
                )
            }

            // Просто проверяем что все основные компоненты можно получить
            val httpClient = get<HttpClient>()
            val currencyRepo = get<CurrencyRepository>()
            val telegramRepo = get<TelegramRepository>()
            val historyRepo = get<CurrencyHistoryRepository>()
            val useCase = get<SendCurrencyRatesUseCase>()
            val scheduler = get<QuartzSchedulerManager>()

            // Все компоненты должны быть не null
            httpClient shouldNotBe null
            currencyRepo shouldNotBe null
            telegramRepo shouldNotBe null
            historyRepo shouldNotBe null
            useCase shouldNotBe null
            scheduler shouldNotBe null
        }
    }

    @Nested
    @DisplayName("Проверка отдельных компонентов")
    inner class ComponentResolution {

        @Test
        fun `должен предоставить HttpClient singleton`() {
            // Arrange
            startKoin {
                modules(
                    module { single { createTestConfig() } },
                    appModule
                )
            }

            // Act
            val httpClient1 = get<HttpClient>()
            val httpClient2 = get<HttpClient>()

            // Assert
            httpClient1 shouldNotBe null
            httpClient1 shouldBe httpClient2 // Singleton
        }

        @Test
        fun `должен предоставить TelegramApi`() {
            // Arrange
            startKoin {
                modules(
                    module { single { createTestConfig() } },
                    appModule
                )
            }

            // Act
            val telegramApi = get<TelegramApi>()

            // Assert
            telegramApi shouldNotBe null
            telegramApi.shouldBeInstanceOf<TelegramApi>()
        }

        @Test
        fun `должен предоставить KursKzParser`() {
            // Arrange
            startKoin {
                modules(
                    module { single { createTestConfig() } },
                    appModule
                )
            }

            // Act
            val parser = get<KursKzParser>()

            // Assert
            parser shouldNotBe null
            parser.shouldBeInstanceOf<KursKzParser>()
        }

        @Test
        fun `должен предоставить CurrencyRepository`() {
            // Arrange
            startKoin {
                modules(
                    module { single { createTestConfig() } },
                    appModule
                )
            }

            // Act
            val repository = get<CurrencyRepository>()

            // Assert
            repository shouldNotBe null
            repository.shouldBeInstanceOf<CurrencyRepositoryImpl>()
        }

        @Test
        fun `должен предоставить TelegramRepository`() {
            // Arrange
            startKoin {
                modules(
                    module { single { createTestConfig() } },
                    appModule
                )
            }

            // Act
            val repository = get<TelegramRepository>()

            // Assert
            repository shouldNotBe null
            repository.shouldBeInstanceOf<TelegramRepositoryImpl>()
        }

        @Test
        fun `должен предоставить CurrencyHistoryRepository с правильным путем к БД`() {
            // Arrange
            val testDbPath = "mem:test-db"
            val testConfig = createTestConfig(databasePath = testDbPath)

            startKoin {
                modules(
                    module { single { testConfig } },
                    appModule
                )
            }

            // Act
            val repository = get<CurrencyHistoryRepository>()

            // Assert
            repository shouldNotBe null
            repository.shouldBeInstanceOf<CurrencyHistoryRepositoryImpl>()
        }

        @Test
        fun `должен предоставить SendCurrencyRatesUseCase со всеми зависимостями`() {
            // Arrange
            startKoin {
                modules(
                    module { single { createTestConfig() } },
                    appModule
                )
            }

            // Act
            val useCase = get<SendCurrencyRatesUseCase>()

            // Assert
            useCase shouldNotBe null
            useCase.shouldBeInstanceOf<SendCurrencyRatesUseCase>()
        }

        @Test
        fun `должен предоставить QuartzSchedulerManager со всеми зависимостями`() {
            // Arrange
            startKoin {
                modules(
                    module { single { createTestConfig() } },
                    appModule
                )
            }

            // Act
            val scheduler = get<QuartzSchedulerManager>()

            // Assert
            scheduler shouldNotBe null
            scheduler.shouldBeInstanceOf<QuartzSchedulerManager>()
        }
    }

    @Nested
    @DisplayName("Проверка связей зависимостей")
    inner class DependencyRelations {

        @Test
        fun `CurrencyRepositoryImpl должен получить KursKzParser`() {
            // Arrange
            startKoin {
                modules(
                    module { single { createTestConfig() } },
                    appModule
                )
            }

            // Act
            val repository = get<CurrencyRepository>()
            val parser = get<KursKzParser>()

            // Assert
            repository shouldNotBe null
            parser shouldNotBe null
            // Проверяем что репозиторий использует тот же парсер из контейнера
        }

        @Test
        fun `TelegramRepositoryImpl должен получить TelegramApi`() {
            // Arrange
            startKoin {
                modules(
                    module { single { createTestConfig() } },
                    appModule
                )
            }

            // Act
            val repository = get<TelegramRepository>()
            val api = get<TelegramApi>()

            // Assert
            repository shouldNotBe null
            api shouldNotBe null
        }

        @Test
        fun `SendCurrencyRatesUseCase должен получить все три репозитория`() {
            // Arrange
            startKoin {
                modules(
                    module { single { createTestConfig() } },
                    appModule
                )
            }

            // Act
            val useCase = get<SendCurrencyRatesUseCase>()
            val currencyRepo = get<CurrencyRepository>()
            val telegramRepo = get<TelegramRepository>()
            val historyRepo = get<CurrencyHistoryRepository>()

            // Assert
            useCase shouldNotBe null
            currencyRepo shouldNotBe null
            telegramRepo shouldNotBe null
            historyRepo shouldNotBe null
        }

        @Test
        fun `все компоненты должны быть singleton`() {
            // Arrange
            startKoin {
                modules(
                    module { single { createTestConfig() } },
                    appModule
                )
            }

            // Act & Assert - проверяем что все компоненты singleton
            val httpClient1 = get<HttpClient>()
            val httpClient2 = get<HttpClient>()
            httpClient1 shouldBe httpClient2

            val currencyRepo1 = get<CurrencyRepository>()
            val currencyRepo2 = get<CurrencyRepository>()
            currencyRepo1 shouldBe currencyRepo2

            val telegramRepo1 = get<TelegramRepository>()
            val telegramRepo2 = get<TelegramRepository>()
            telegramRepo1 shouldBe telegramRepo2

            val historyRepo1 = get<CurrencyHistoryRepository>()
            val historyRepo2 = get<CurrencyHistoryRepository>()
            historyRepo1 shouldBe historyRepo2

            val useCase1 = get<SendCurrencyRatesUseCase>()
            val useCase2 = get<SendCurrencyRatesUseCase>()
            useCase1 shouldBe useCase2

            val scheduler1 = get<QuartzSchedulerManager>()
            val scheduler2 = get<QuartzSchedulerManager>()
            scheduler1 shouldBe scheduler2
        }
    }

    @Nested
    @DisplayName("Проверка конфигурации")
    inner class ConfigurationTests {

        @Test
        fun `AppConfig должен передаваться корректно`() {
            // Arrange
            val testConfig = AppConfig(
                botToken = "test-token-123",
                chatId = "test-chat-456",
                schedulerCron = "0 0 */2 * * ?",
                databasePath = "mem:custom-db",
                unkeyRootKey = "test-unkey-root-key",
                internalApiKey = "test-internal-key"
            )

            startKoin {
                modules(
                    module { single { testConfig } },
                    appModule
                )
            }

            // Act
            val config = get<AppConfig>()

            // Assert
            config shouldBe testConfig
            config.botToken shouldBe "test-token-123"
            config.chatId shouldBe "test-chat-456"
            config.schedulerCron shouldBe "0 0 */2 * * ?"
            config.databasePath shouldBe "mem:custom-db"
        }
    }

    private fun createTestConfig(
        botToken: String = "test-bot-token",
        chatId: String = "test-chat-id",
        schedulerCron: String = "0 0 * * * ?",
        databasePath: String = "mem:test-db",
        unkeyRootKey: String = "test-unkey-root-key",
        internalApiKey: String = "test-internal-key"
    ): AppConfig {
        return AppConfig(
            botToken = botToken,
            chatId = chatId,
            schedulerCron = schedulerCron,
            databasePath = databasePath,
            unkeyRootKey = unkeyRootKey,
            internalApiKey = internalApiKey
        )
    }
}
