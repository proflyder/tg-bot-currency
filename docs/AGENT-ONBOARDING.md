# Agent Onboarding Guide

> Быстрый старт для AI-агентов и новых разработчиков в проекте Currency Bot

## Что это за проект?

**Currency Bot** - Telegram бот на Kotlin + Ktor, который парсит курсы валют с kurs.kz, сохраняет историю в H2 базе данных, отслеживает пороговые изменения и отправляет умные уведомления в Telegram по cron расписанию.

### Ключевые характеристики

- **Язык:** Kotlin 2.2.20
- **Фреймворк:** Ktor 3.3.2 (Server + Client)
- **Архитектура:** Clean Architecture (Domain → Data → Presentation)
- **DI:** Koin
- **База данных:** H2 Database + Exposed ORM
- **Scheduler:** Quartz Scheduler с cron expressions
- **Асинхронность:** Coroutines
- **Логирование:** Logback + Logstash encoder (JSON формат)
- **Деплой:** Docker + GitHub Actions (CI/CD)
- **Package:** `dev.proflyder.currency`

---

## Быстрая навигация

### 🎯 Первые шаги
1. **[Быстрый старт](guides/quickstart.md)** - запустить бота за 5 минут
2. **[Архитектура](#архитектура-проекта)** - понять структуру кода
3. **[Coding Conventions](#coding-conventions)** - правила работы с кодом
4. **[Частые задачи](#частые-задачи)** - типичные сценарии работы

### 📖 Документация
- **[Документация Index](index.md)** - полный каталог документов
- **[Логирование](guides/logging.md)** - best practices
- **[CI/CD](deployment/ci-cd-guide.md)** - автоматизация
- **[Troubleshooting](troubleshooting/common-issues.md)** - решение проблем

---

## Архитектура проекта

### Clean Architecture Layers

```
src/main/kotlin/dev/proflyder/currency/
├── Application.kt              # Entry point, Koin setup
├── Routing.kt                  # HTTP endpoints
│
├── domain/                     # ✅ БИЗНЕС-ЛОГИКА (независимый слой)
│   ├── model/
│   │   ├── CurrencyRate.kt    # Модель курса валют
│   │   ├── Alert.kt           # Модели алертов (WARNING/CRITICAL)
│   │   └── Threshold.kt       # Пороговые значения для алертов
│   ├── repository/
│   │   ├── CurrencyRepository.kt        # Интерфейс для получения курсов
│   │   ├── TelegramRepository.kt        # Интерфейс для отправки сообщений
│   │   └── CurrencyHistoryRepository.kt # Интерфейс для истории курсов
│   └── usecase/
│       ├── SendCurrencyRatesUseCase.kt         # Главный use case
│       ├── CheckCurrencyThresholdsUseCase.kt   # Проверка порогов
│       └── FormatCurrencyMessageUseCase.kt     # Форматирование сообщений
│
├── data/                       # ✅ РАБОТА С ДАННЫМИ
│   ├── database/
│   │   └── CurrencyHistoryTable.kt  # Exposed таблица для H2
│   ├── dto/
│   │   └── TelegramDto.kt     # Модели для Telegram API
│   ├── remote/
│   │   ├── parser/
│   │   │   └── KursKzParser.kt      # Парсинг kurs.kz через Ksoup
│   │   └── telegram/
│   │       └── TelegramApi.kt       # Клиент Telegram Bot API
│   └── repository/
│       ├── CurrencyRepositoryImpl.kt        # Реализация через парсер
│       ├── TelegramRepositoryImpl.kt        # Реализация через API
│       └── CurrencyHistoryRepositoryImpl.kt # Реализация через H2 + Exposed
│
├── di/                         # ✅ DEPENDENCY INJECTION
│   ├── AppConfig.kt           # Конфигурация (bot token, chat ID, cron, db path)
│   └── AppModule.kt           # Koin модуль (все зависимости)
│
├── scheduler/                  # ✅ QUARTZ SCHEDULER
│   ├── QuartzSchedulerManager.kt  # Менеджер Quartz Scheduler
│   └── CurrencyRatesJob.kt        # Quartz Job для выполнения задачи
│
└── util/
    ├── LoggingUtils.kt        # MDC контекст, request ID
    └── JsonToH2Migrator.kt    # Миграция JSON → H2
```

### Data Flow (как всё работает)

```
1. Quartz Scheduler (по cron расписанию, например каждый час)
   ↓
2. CurrencyRatesJob.execute()
   ↓
3. SendCurrencyRatesUseCase
   ↓
4. CurrencyRepository.getRates()  →  KursKzParser  →  GET https://kurs.kz
   ↓                                      ↓
   Парсинг HTML через Ksoup
   ↓
5. CurrencyHistoryRepository.saveRecord()  →  H2 Database (INSERT)
   ↓
6. CheckCurrencyThresholdsUseCase  →  Проверка порогов за час/день/неделю/месяц
   ↓                                    ↓
   CurrencyHistoryRepository.getRecordBefore()  →  H2 Database (SELECT)
   ↓
7. Если пороги превышены:
   ↓
   FormatCurrencyMessageUseCase  →  Форматирование с алертами
   ↓
   TelegramRepository.sendMessage()  →  TelegramApi  →  POST https://api.telegram.org
   ↓
8. Сообщение в Telegram чат (только если есть алерты!)
   ↓
9. CurrencyHistoryRepository.cleanOldRecords()  →  Очистка старых записей (>30 дней)
```

### Ключевые компоненты

| Компонент | Путь | Описание |
|-----------|------|----------|
| **Entry Point** | `Application.kt:23` | `module()` - инициализация Koin, запуск Quartz |
| **Quartz Manager** | `QuartzSchedulerManager.kt` | Управление Quartz Scheduler с cron |
| **Quartz Job** | `CurrencyRatesJob.kt` | Выполнение задачи парсинга и отправки |
| **Use Case** | `SendCurrencyRatesUseCase.kt` | Главная бизнес-логика: получить → сохранить → проверить пороги → отправить |
| **Threshold Check** | `CheckCurrencyThresholdsUseCase.kt` | Проверка изменений курсов за 4 периода |
| **Message Format** | `FormatCurrencyMessageUseCase.kt` | Форматирование сообщений с алертами |
| **Parser** | `KursKzParser.kt` | HTML парсинг через Ksoup, извлечение курсов |
| **Telegram Client** | `TelegramApi.kt` | Ktor Client для Telegram Bot API |
| **H2 Repository** | `CurrencyHistoryRepositoryImpl.kt` | Работа с H2 через Exposed ORM |
| **H2 Table** | `CurrencyHistoryTable.kt` | Схема таблицы currency_history |
| **DI Module** | `AppModule.kt` | Все `single {}` определения |

---

## Новые возможности

### 🗄️ H2 Database для истории курсов

**Вместо JSON файлов** теперь используется **H2 Database** с Exposed ORM:

**Преимущества:**
- ✅ Thread-safe из коробки (не нужен Mutex)
- ✅ Быстрые SQL запросы с индексами
- ✅ Меньший размер (~60% экономия)
- ✅ Инструменты: H2 Console, DataGrip

**Файл БД:** `data/currency-history.mv.db`

**Схема таблицы:**
```sql
CREATE TABLE currency_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    usd_buy DOUBLE NOT NULL,
    usd_sell DOUBLE NOT NULL,
    rub_buy DOUBLE NOT NULL,
    rub_sell DOUBLE NOT NULL,
    INDEX idx_timestamp (timestamp)
);
```

### ⚡ Quartz Scheduler с cron expressions

**Вместо простого delay** теперь используется **Quartz Scheduler**:

**Преимущества:**
- ✅ Профессиональное планирование задач
- ✅ Cron expressions для гибкости
- ✅ Умная обработка пропущенных выполнений
- ✅ Thread pool management

**Примеры cron:**
```
0 0 * * * ?      # Каждый час
0 0 */2 * * ?    # Каждые 2 часа
0 0 9 * * ?      # Каждый день в 9:00
0 30 14 * * ?    # Каждый день в 14:30
```

### 📊 Threshold Alert System

**Умные уведомления** - бот отправляет сообщения только при превышении порогов:

**4 периода проверки:**
- **Час:** WARNING 0.5%, CRITICAL 1.0%
- **Сутки:** WARNING 1.0%, CRITICAL 2.0%
- **Неделя:** WARNING 2.0%, CRITICAL 4.0%
- **Месяц:** WARNING 3.0%, CRITICAL 5.0%

**Логика:**
1. Сохраняем курсы в БД (всегда)
2. Проверяем изменения за 4 периода
3. Если **нет превышений** - сообщение НЕ отправляется (экономия spam)
4. Если **есть алерты** - отправляем красиво отформатированное сообщение

**Формат алертов:**
```
💱 *Курсы валют на kurs.kz*
...

⚠️ *ПРЕДУПРЕЖДЕНИЯ*
─────────────────────────
📈 🇺🇸 *USD → KZT* вырос на 0.80% за час
   480.00 → 483.84 ₸

🚨 *КРИТИЧЕСКИЕ ИЗМЕНЕНИЯ*
─────────────────────────
📉 🇷🇺 *RUB → KZT* упал на 2.50% за сутки
   490.00 → 478.25 ₸
```

---

## Coding Conventions

### Логирование (ВАЖНО!)

Проект использует **structured logging** с MDC контекстом.

**✅ Правильно:**
```kotlin
import dev.proflyder.currency.util.logger
import dev.proflyder.currency.util.withLoggingContext

class MyClass {
    private val logger = logger()

    suspend fun doSomething() {
        withLoggingContext(mapOf("request_id" to generateRequestId())) {
            logger.info("Starting operation")
            // код
            logger.error("Failed to parse", exception)
        }
    }
}
```

**❌ Неправильно:**
```kotlin
// НЕ используй println()
println("Debug info")

// НЕ создавай логгер напрямую
private val logger = LoggerFactory.getLogger(javaClass)
```

**Подробнее:** [Logging Guide](guides/logging.md)

### Dependency Injection

**✅ Всегда используй Koin:**
```kotlin
// В AppModule.kt
single { MyService(get(), get()) }

// В классе
class MyClass(
    private val dependency: SomeDependency  // inject через конструктор
)
```

**❌ Не создавай зависимости вручную:**
```kotlin
val myService = MyService()  // ❌ НЕТ!
```

### Работа с H2 Database

**✅ Используй Exposed DSL:**
```kotlin
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

// Вставка
transaction(database) {
    CurrencyHistoryTable.insert {
        it[timestamp] = now
        it[usdBuy] = 485.0
    }
}

// Выборка
transaction(database) {
    CurrencyHistoryTable
        .select { timestamp lessEq targetTime }
        .orderBy(timestamp, SortOrder.DESC)
        .firstOrNull()
}
```

### Coroutines

**✅ Используй withContext для IO:**
```kotlin
override suspend fun saveRecord(rates: CurrencyRate): Result<Unit> = runCatching {
    withContext(Dispatchers.IO) {
        transaction(database) {
            // DB operations
        }
    }
}
```

**✅ Обрабатывай ошибки:**
```kotlin
try {
    withContext(Dispatchers.IO) {
        // код
    }
} catch (e: Exception) {
    logger.error("Operation failed", e)
}
```

### Error Handling

Проект использует `Result<T>` для обработки ошибок:

```kotlin
suspend fun getRates(): Result<List<CurrencyRate>> = runCatching {
    // код
}

// Использование
result.fold(
    onSuccess = { data -> /* handle success */ },
    onFailure = { error -> logger.error("Error", error) }
)
```

### Clean Architecture Rules

1. **Domain** не зависит ни от чего (только Kotlin stdlib)
2. **Data** реализует интерфейсы из Domain
3. **DI слой** связывает всё вместе
4. Зависимости идут только внутрь: Data → Domain ← Scheduler

---

## Частые задачи

### Добавить новую валютную пару

**Файл:** `src/main/kotlin/dev/proflyder/currency/data/remote/parser/KursKzParser.kt`

```kotlin
private fun extractRates(html: String): List<CurrencyRate> {
    // Добавь новый блок парсинга
    val eurToKzt = parseRate("EUR", "KZT", html)
    // Добавь в return список
}
```

**Не забудь обновить:**
- `CurrencyRate` model
- `CurrencyHistoryTable` (добавить колонки)
- `CheckCurrencyThresholdsUseCase`

### Изменить расписание отправки

**Файл:** `src/main/resources/application.yaml`

```yaml
scheduler:
  cron: ${SCHEDULER_CRON:0 0 */2 * * ?}  # Каждые 2 часа
```

**Или через environment variable:**
```bash
SCHEDULER_CRON="0 0 9 * * ?" ./gradlew run  # Каждый день в 9:00
```

### Изменить пороги алертов

**Файл:** `src/main/kotlin/dev/proflyder/currency/domain/model/Threshold.kt`

```kotlin
object CurrencyThresholds {
    val HOUR = ThresholdConfig(
        period = AlertPeriod.HOUR,
        warningPercent = 0.7,   // Было 0.5%
        criticalPercent = 1.5   // Было 1.0%
    )
    // ...
}
```

### Добавить новый endpoint

**Файл:** `src/main/kotlin/Routing.kt`

```kotlin
fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respondText("OK")
        }

        // Добавь новый endpoint здесь
        get("/rates") {
            // логика
        }
    }
}
```

### Добавить новую зависимость

**1. Добавь в `gradle/libs.versions.toml`:**
```toml
[versions]
my-library = "1.0.0"

[libraries]
my-library = { module = "com.example:library", version.ref = "my-library" }
```

**2. Добавь в `build.gradle.kts`:**
```kotlin
dependencies {
    implementation(libs.my.library)
}
```

**3. Добавь в Koin модуль `AppModule.kt`:**
```kotlin
single { MyLibraryClient() }
```

### Просмотреть H2 Database

**Через H2 Console:**
```bash
# Добавь в application.yaml
ktor:
  development: true

# H2 Console будет доступна на
http://localhost:8082
```

**Или через DataGrip/DBeaver:**
```
JDBC URL: jdbc:h2:file:./data/currency-history
Driver: H2
User: (пусто)
Password: (пусто)
```

---

## Конфигурация

### Переменные окружения (.env)

```env
BOT_TOKEN=ваш_токен_от_BotFather
CHAT_ID=ваш_chat_id
SCHEDULER_CRON=0 0 * * * ?
DATABASE_PATH=data/currency-history
```

### Application.yaml

```yaml
ktor:
  application:
    modules:
      - dev.proflyder.currency.ApplicationKt.module
  deployment:
    port: 8080
    host: 0.0.0.0

bot:
  token: ${BOT_TOKEN}
  chatId: ${CHAT_ID}

scheduler:
  cron: ${SCHEDULER_CRON:0 0 * * * ?}  # Каждый час по умолчанию

database:
  path: ${DATABASE_PATH:data/currency-history}
```

---

## Запуск и тестирование

### Локальная разработка

```bash
# 1. Настроить .env
cp .env.example .env
nano .env  # Добавить BOT_TOKEN и CHAT_ID

# 2. Запустить
./gradlew run

# Логи в реальном времени
tail -f logs/currency-bot.log
```

### Docker

```bash
# Локальная сборка
docker-compose up --build

# Или использовать готовый образ из GHCR
# (раскомментировать DOCKER_IMAGE в .env)
docker-compose pull
docker-compose up -d

# Просмотр логов
./scripts/logs.sh tail
```

### Тесты

```bash
# Все тесты (98 тестов)
./gradlew test

# С подробным выводом
./gradlew test --info
```

**H2 в тестах:**
Тесты используют in-memory H2 (`mem:test-*`) для скорости и изоляции.

---

## Debugging

### Проверить логи

```bash
# Утилита logs.sh (рекомендуется)
./scripts/logs.sh tail       # Реал-тайм
./scripts/logs.sh show       # Последние 100 строк
./scripts/logs.sh size       # Размер логов

# Или напрямую
tail -f logs/currency-bot.log
```

### Проверить статус Quartz

В логах должны быть сообщения:
```
Quartz Scheduler started successfully
Next execution scheduled at: Mon Dec 01 00:00:00 ALMT 2025
```

### Проверить H2 Database

```bash
# Размер БД
ls -lh data/currency-history.mv.db

# Количество записей (через H2 Console или DataGrip)
SELECT COUNT(*) FROM currency_history;

# Последние записи
SELECT * FROM currency_history
ORDER BY timestamp DESC
LIMIT 10;
```

### Проверить статус контейнера

```bash
docker ps
docker logs currency-bot
docker exec -it currency-bot sh
```

### Проверить парсинг kurs.kz

Добавь временный endpoint в `Routing.kt`:

```kotlin
get("/debug/rates") {
    val rates = currencyRepository.getRates()
    call.respond(rates)
}
```

### Проверить Telegram API

```bash
# Через curl
curl -X POST "https://api.telegram.org/bot<YOUR_TOKEN>/sendMessage" \
  -H "Content-Type: application/json" \
  -d '{"chat_id": "<YOUR_CHAT_ID>", "text": "Test"}'
```

---

## Troubleshooting

### Бот не отправляет сообщения

1. **Проверь пороги** - возможно нет превышений (это нормально!)
2. Проверь токен и chat ID в `.env`
3. Убедись что бот добавлен в чат
4. Проверь логи: `./scripts/logs.sh show`
5. Проверь сеть: `docker exec currency-bot ping api.telegram.org`

### H2 Database ошибки

1. **"Database is already closed":**
   - Проверь что используется `transaction(database)` вместо `transaction`

2. **"Table not found":**
   - Проверь что `SchemaUtils.create()` вызывается в init

3. **Connection errors в тестах:**
   - Убедись что каждый тест создает уникальную in-memory БД

### Quartz Scheduler проблемы

1. **Job не выполняется:**
   - Проверь cron expression (валидность)
   - Проверь логи: "Quartz job execution started"

2. **Misfire:**
   - Настроено `DoNothing` - пропущенные задачи игнорируются

### Парсинг не работает

1. Проверь доступность kurs.kz: `curl https://kurs.kz`
2. Возможно изменилась структура HTML (обнови селекторы в `KursKzParser.kt`)

### Приложение не запускается

1. Проверь Java: `java -version` (нужна 21+)
2. Проверь порт 8080: `lsof -i :8080`
3. Проверь `.env` файл
4. Проверь путь к БД (должна быть доступна на запись)

**Подробнее:** [Troubleshooting](troubleshooting/common-issues.md)

---

## CI/CD Pipeline

### GitHub Actions Workflow

При каждом push в `main`:

1. **Test Job:** Запуск всех 98 тестов
2. **Build Job:** Gradle сборка → JAR артефакт
3. **Docker Job:** Сборка Docker образа → Публикация в GHCR
4. **Deploy Job:** Автоматический деплой на GCP VM

### Доступ к образу

```bash
# Образ публикуется автоматически
ghcr.io/<your-username>/currency-bot:latest

# Использование
docker pull ghcr.io/<your-username>/currency-bot:latest
```

**Подробнее:** [CI/CD Guide](deployment/ci-cd-guide.md)

---

## Полезные команды

### Gradle

```bash
./gradlew build              # Сборка проекта
./gradlew buildFatJar        # Собрать fat JAR
./gradlew run                # Запустить локально
./gradlew test               # Тесты
./gradlew clean              # Очистить build
```

### Docker

```bash
docker-compose up --build    # Сборка + запуск
docker-compose up -d         # Запуск в фоне
docker-compose down          # Остановка
docker-compose logs -f       # Логи реал-тайм
docker-compose pull          # Обновить образ
docker-compose restart       # Перезапуск
```

### Logs

```bash
./scripts/logs.sh tail       # Следить за логами
./scripts/logs.sh show       # Последние логи
./scripts/logs.sh backup     # Создать backup
./scripts/logs.sh clean 30   # Удалить старые логи
```

### H2 Database

```bash
# Backup
cp data/currency-history.mv.db data/currency-history.backup.mv.db

# Размер
du -h data/currency-history.mv.db

# Экспорт в SQL (через H2 Console)
SCRIPT TO 'backup.sql'

# Импорт из SQL
RUNSCRIPT FROM 'backup.sql'
```

### Git

```bash
git status                   # Статус
git add .                    # Добавить все изменения
git commit -m "message"      # Коммит
git push origin main         # Push в main
```

---

## Best Practices

### Перед началом работы

1. **Прочитай существующий код** в области, которую будешь менять
2. **Проверь логи** - возможно проблема уже известна
3. **Следуй архитектуре** - не нарушай Clean Architecture
4. **Используй существующие паттерны** - не изобретай велосипед

### Во время работы

1. **Логируй всё важное** - используй `withLoggingContext()`
2. **Обрабатывай ошибки** - используй `Result<T>` или `try-catch`
3. **Пиши читаемый код** - Kotlin идиомы, named arguments
4. **Не дублируй код** - создавай утилиты при необходимости
5. **Используй transaction(database)** - для работы с H2

### После завершения

1. **Протестируй локально** - `./gradlew test && ./gradlew run`
2. **Проверь логи** - нет ли ошибок
3. **Проверь БД** - данные сохраняются корректно
4. **Обнови документацию** - если добавил новую функциональность
5. **Сделай коммит** - понятное сообщение

### Код-ревью чеклист

- [ ] Код следует Clean Architecture
- [ ] Используется Koin для DI
- [ ] Есть логирование с MDC контекстом
- [ ] Обрабатываются ошибки
- [ ] H2 операции в `transaction(database)`
- [ ] Нет hardcoded значений (используется `application.yaml` или `.env`)
- [ ] Код следует Kotlin conventions
- [ ] Тесты проходят (98/98)
- [ ] Документация обновлена (если нужно)

---

## Контакты и помощь

### Документация

- **[Documentation Index](index.md)** - полный каталог
- **[Contributing Guide](contributing.md)** - как внести вклад

### External Links

- [Kotlin Docs](https://kotlinlang.org/docs/)
- [Ktor Documentation](https://ktor.io/docs/)
- [Koin Documentation](https://insert-koin.io/docs/reference/introduction)
- [Exposed Documentation](https://github.com/JetBrains/Exposed/wiki)
- [H2 Database](https://www.h2database.com/html/main.html)
- [Quartz Scheduler](http://www.quartz-scheduler.org/documentation/)
- [Telegram Bot API](https://core.telegram.org/bots/api)

---

## Checklist для новых агентов

После прочтения этого документа, ты должен понимать:

- [ ] Что делает этот проект (парсинг курсов + история + умные алерты + Telegram)
- [ ] Какая архитектура используется (Clean Architecture)
- [ ] Как работает data flow (Quartz → Job → UseCase → Repositories → H2/API)
- [ ] Где находятся ключевые компоненты
- [ ] Как работает H2 Database с Exposed ORM
- [ ] Как работает Quartz Scheduler с cron expressions
- [ ] Как работает система алертов (пороги, периоды)
- [ ] Как правильно логировать (`withLoggingContext()`)
- [ ] Как добавлять зависимости (Koin DI)
- [ ] Как работать с БД (`transaction(database)`)
- [ ] Как запустить проект локально
- [ ] Где искать логи для debugging
- [ ] Как работает CI/CD (GitHub Actions → GHCR → GCP)

**Если что-то непонятно** - читай соответствующий раздел документации из [Documentation Index](index.md).

---

**Последнее обновление:** 2025-12-01
**Версия:** 2.0.0
**Изменения:**
- Добавлен H2 Database вместо JSON
- Добавлен Quartz Scheduler вместо простого delay
- Добавлена система threshold alerts
- Обновлена архитектура и data flow
