# Logging Best Practices

## Обзор

Проект использует **SLF4J + Logback** для логирования с поддержкой:
- Структурированных логов (JSON для продакшна)
- MDC (Mapped Diagnostic Context) для трейсинга
- Автоматической ротации логов
- Маскирования sensitive данных

## Уровни логирования

### TRACE
Самый детальный уровень. Используй для дебаггинга алгоритмов.
```kotlin
logger.trace("Processing item: $item")
```

### DEBUG
Детальная информация для разработки.
```kotlin
logger.debug("Parsed ${punktsArray.size} exchange points")
```

### INFO
Важные события в приложении.
```kotlin
logger.info("Currency rates sent successfully")
```

### WARN
Потенциальные проблемы, не критичные ошибки.
```kotlin
logger.warn("Retry attempt #3 failed, will try again")
```

### ERROR
Ошибки, требующие внимания.
```kotlin
logger.error("Failed to parse currency rates", exception)
```

## Как логировать

### 1. Создание логгера

```kotlin
import dev.proflyder.currency.util.logger

class MyClass {
    private val logger = logger()

    fun myMethod() {
        logger.info("Method called")
    }
}
```

### 2. Логирование с таймингом

```kotlin
import dev.proflyder.currency.util.logWithTiming

val result = logger.logWithTiming("Fetching data from API") {
    // код, который нужно замерить
    fetchDataFromApi()
}
// Автоматически залогирует: "Fetching data from API - completed in 123ms"
```

### 3. Логирование с контекстом (MDC)

```kotlin
import dev.proflyder.currency.util.withLoggingContext

withLoggingContext(mapOf("request_id" to requestId, "user_id" to userId)) {
    // весь код внутри будет иметь этот контекст в логах
    processRequest()
}
```

### 4. Маскирование sensitive данных

```kotlin
import dev.proflyder.currency.util.maskSensitive

val logMessage = "token=abc123secret".maskSensitive()
logger.info(logMessage) // Выведет: "token=***"
```

## Конфигурация (logback.xml)

### Console Appender
Цветные логи для локальной разработки:
```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %highlight(%-5level) %cyan(%logger{36}) - %msg%n</pattern>
    </encoder>
</appender>
```

### File Appender с ротацией
Логи пишутся в `logs/currency-bot.log` с ротацией:
- Новый файл каждый день
- Хранение 30 дней
- Максимум 1GB всех логов

```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/currency-bot.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/currency-bot.%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>30</maxHistory>
        <totalSizeCap>1GB</totalSizeCap>
    </rollingPolicy>
</appender>
```

### JSON Appender для продакшна
Структурированные логи в JSON формате (для ELK, Datadog, etc):
```xml
<appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>request_id</includeMdcKeyName>
        <includeMdcKeyName>user_id</includeMdcKeyName>
    </encoder>
</appender>
```

## Примеры из кода

### Parser с детальным логированием
```kotlin
class KursKzParser {
    private val logger = logger()

    suspend fun parseCurrencyRates(): Result<CurrencyRate> {
        return try {
            logger.logWithTiming("Parsing currency rates") {
                val html = httpClient.get(baseUrl).bodyAsText()
                logger.debug("Received HTML response, size: ${html.length} bytes")

                // ... парсинг ...

                logger.info("Successfully parsed rates: USD ${rates.usdToKzt.buy}")
                Result.success(rates)
            }
        } catch (e: Exception) {
            logger.error("Failed to parse rates", e)
            Result.failure(e)
        }
    }
}
```

### Telegram API с маскированием токена
```kotlin
class TelegramApi(private val botToken: String) {
    private val logger = logger()
    private val maskedToken = botToken.take(8) + "***"

    suspend fun sendMessage(request: SendMessageRequest) {
        logger.debug("Sending message, token: $maskedToken")
        // ... отправка ...
        logger.info("Message sent successfully, id: ${response.messageId}")
    }
}
```

### Scheduler с MDC контекстом
```kotlin
class CurrencyRatesScheduler {
    private val logger = logger()

    fun start() {
        executionCount++
        val requestId = generateRequestId()

        withLoggingContext(mapOf("request_id" to requestId)) {
            logger.info("Scheduler execution #$executionCount started")
            // весь код внутри имеет request_id в логах
        }
    }
}
```

## Best Practices

### ✅ DO

1. **Используй правильный уровень**
   ```kotlin
   logger.info("User logged in")        // Важное событие
   logger.debug("Cache hit for key X")  // Детали для разработки
   logger.error("DB connection failed", e) // Ошибка
   ```

2. **Логируй контекст**
   ```kotlin
   logger.info("Processing order $orderId for user $userId")
   ```

3. **Используй lazy evaluation**
   ```kotlin
   logger.debug { "Expensive calculation: ${expensiveOperation()}" }
   ```

4. **Всегда передавай exception**
   ```kotlin
   logger.error("Failed to process", exception)
   ```

5. **Маскируй sensitive данные**
   ```kotlin
   logger.info("Token: ${token.take(8)}***")
   ```

### ❌ DON'T

1. **Не логируй в циклах на INFO**
   ```kotlin
   // Плохо
   items.forEach { logger.info("Processing $it") }

   // Хорошо
   logger.info("Processing ${items.size} items")
   items.forEach { logger.debug("Processing $it") }
   ```

2. **Не конкатенируй строки без lazy eval**
   ```kotlin
   // Плохо (строка собирается всегда)
   logger.debug("Result: " + expensiveOperation())

   // Хорошо (вычисляется только если DEBUG включен)
   logger.debug { "Result: ${expensiveOperation()}" }
   ```

3. **Не логируй полные токены/пароли**
   ```kotlin
   // Плохо
   logger.info("Token: $token")

   // Хорошо
   logger.info("Token: ${token.take(8)}***")
   ```

4. **Не используй println**
   ```kotlin
   // Плохо
   println("Something happened")

   // Хорошо
   logger.info("Something happened")
   ```

## Переключение на JSON логи (продакшн)

В `logback.xml` замени:
```xml
<root level="INFO">
    <appender-ref ref="CONSOLE"/>      <!-- Для dev -->
    <appender-ref ref="JSON_CONSOLE"/> <!-- Для prod -->
    <appender-ref ref="FILE"/>
</root>
```

Или используй переменные окружения:
```xml
<if condition='property("LOG_FORMAT").equals("json")'>
    <then>
        <appender-ref ref="JSON_CONSOLE"/>
    </then>
    <else>
        <appender-ref ref="CONSOLE"/>
    </else>
</if>
```

## Мониторинг логов

### Локально
```bash
# Следить за логами в реальном времени
tail -f logs/currency-bot.log

# Искать ошибки
grep ERROR logs/currency-bot.log

# Фильтровать по request_id
grep "request_id=abc123" logs/currency-bot.log
```

### В Docker
```bash
docker logs -f currency-bot

# С фильтрацией
docker logs currency-bot 2>&1 | grep ERROR
```

### В продакшне
Используй ELK Stack, Datadog, CloudWatch или аналоги для агрегации и анализа JSON логов.
