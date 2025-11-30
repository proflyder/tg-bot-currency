# Get Chat ID

> Как получить Chat ID программно через метод getChatIds()

Метод `getChatIds()` был добавлен в `TelegramRepository` для получения списка chat ID из последних сообщений боту.

## Использование

### Вариант 1: Вызов через Repository

```kotlin
import dev.proflyder.currency.domain.repository.TelegramRepository
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MyChatIdFetcher : KoinComponent {
    private val telegramRepository: TelegramRepository by inject()

    fun fetchChatIds() = runBlocking {
        val result = telegramRepository.getChatIds()

        result.fold(
            onSuccess = { chatIds ->
                println("✓ Found ${chatIds.size} chat(s):")
                chatIds.forEach { id ->
                    println("  Chat ID: $id")
                }
            },
            onFailure = { error ->
                println("✗ Error: ${error.message}")
            }
        )
    }
}
```

### Вариант 2: Простой UseCase

Создай файл `GetChatIdsUseCase.kt`:

```kotlin
package dev.proflyder.currency.domain.usecase

import dev.proflyder.currency.domain.repository.TelegramRepository

class GetChatIdsUseCase(
    private val telegramRepository: TelegramRepository
) {
    suspend operator fun invoke(): Result<List<Long>> {
        return telegramRepository.getChatIds()
    }
}
```

Зарегистрируй в Koin (добавь в `AppModule.kt`):

```kotlin
single { GetChatIdsUseCase(get()) }
```

Используй:

```kotlin
import dev.proflyder.currency.domain.usecase.GetChatIdsUseCase
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MyApp : KoinComponent {
    private val getChatIdsUseCase: GetChatIdsUseCase by inject()

    fun printChatIds() = runBlocking {
        getChatIdsUseCase().fold(
            onSuccess = { chatIds ->
                chatIds.forEach { println("Chat ID: $it") }
            },
            onFailure = { error ->
                println("Error: ${error.message}")
            }
        )
    }
}
```

### Вариант 3: Простой скрипт для тестирования

Создай `TestChatId.kt`:

```kotlin
package dev.proflyder.currency

import dev.proflyder.currency.di.AppConfig
import dev.proflyder.currency.di.appModule
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun main() = runBlocking {
    // Инициализируй Koin
    startKoin {
        modules(
            module {
                single {
                    AppConfig(
                        botToken = System.getenv("BOT_TOKEN") ?: "your-token-here",
                        chatId = "",
                        schedulerIntervalHours = 1
                    )
                }
            },
            appModule
        )
    }

    // Получи repository
    val koin = org.koin.core.context.GlobalContext.get()
    val telegramRepository = koin.get<dev.proflyder.currency.domain.repository.TelegramRepository>()

    // Вызови метод
    println("Fetching chat IDs from Telegram...")
    val result = telegramRepository.getChatIds()

    result.fold(
        onSuccess = { chatIds ->
            println("\n✓ SUCCESS!")
            println("Found ${chatIds.size} chat(s):\n")
            chatIds.forEach { id ->
                println("  Chat ID: $id")
            }
            println("\nCopy one of these IDs to your .env file:")
            println("CHAT_ID=${chatIds.firstOrNull() ?: "N/A"}")
        },
        onFailure = { error ->
            println("\n✗ FAILED!")
            println("Error: ${error.message}")
            println("\nTroubleshooting:")
            println("1. Check BOT_TOKEN is correct")
            println("2. Send a message to your bot first")
            println("3. Try again")
        }
    )
}
```

Запусти:

```bash
export BOT_TOKEN="your-bot-token"
./gradlew run -PmainClass=dev.proflyder.currency.TestChatIdKt
```

## Требования

1. **BOT_TOKEN** должен быть настроен
2. **Отправь хотя бы одно сообщение боту** перед вызовом метода
3. Приложение должно иметь доступ к интернету

## Возвращаемые данные

Метод возвращает `Result<List<Long>>`:

- **Success**: Список chat ID (Long)
  - Положительные числа - личные чаты
  - Отрицательные числа - группы/каналы

- **Failure**: Exception с описанием ошибки
  - Проблемы с сетью
  - Неверный BOT_TOKEN
  - Ошибка API Telegram

## Логи

Метод автоматически логирует:

```
[DEBUG] Fetching updates from Telegram, token: 12345678***
[INFO]  Found 2 unique chat(s) in recent updates
```

## Примеры ответов

### Успех (личный чат)
```kotlin
Result.success(listOf(123456789))
```

### Успех (личный чат + группа)
```kotlin
Result.success(listOf(123456789, -1001234567890))
```

### Нет сообщений
```kotlin
Result.success(emptyList())
```

### Ошибка
```kotlin
Result.failure(Exception("Telegram API error: Unauthorized"))
```

## Troubleshooting

### Пустой список
- Отправь сообщение боту `/start`
- Подожди несколько секунд
- Попробуй снова

### "Unauthorized"
- Проверь `BOT_TOKEN`
- Убедись что токен актуален (получи новый у @BotFather если нужно)

### "Not Found"
- URL неверный (это не должно случиться, но проверь)

---

**Готово!** Теперь у тебя есть метод для получения chat ID программно! 🎉
