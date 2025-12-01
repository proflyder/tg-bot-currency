# Trigger API

REST API для принудительного запуска обновления курсов валют.

## Обзор

API предоставляет endpoint для ручного запуска задачи обновления курсов валют, которая обычно выполняется автоматически по расписанию (каждый час). При вызове этого endpoint выполняются следующие действия:

1. **Парсинг курсов** - получение актуальных курсов USD→KZT и RUB→KZT с сайта kurs.kz
2. **Сохранение в БД** - запись курсов в базу данных H2 с временной меткой
3. **Отправка в Telegram** - отправка сообщения с курсами в настроенный Telegram канал

## Endpoint

### POST /api/trigger

Принудительно запустить обновление курсов валют.

**URL:** `/api/trigger`

**Method:** `POST`

**Authentication:** Требуется (Bearer Token)

#### Request Headers

| Header | Value | Description |
|--------|-------|-------------|
| `Authorization` | `Bearer <api_key>` | API ключ для аутентификации через Unkey |

#### Example Request

**cURL:**
```bash
curl -X POST "http://localhost:8080/api/trigger" \
  -H "Authorization: Bearer your-api-key-here"
```

**JavaScript (fetch):**
```javascript
fetch('http://localhost:8080/api/trigger', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer your-api-key-here'
  }
})
.then(response => response.json())
.then(data => console.log(data));
```

**Python (requests):**
```python
import requests

headers = {
    'Authorization': 'Bearer your-api-key-here'
}

response = requests.post('http://localhost:8080/api/trigger', headers=headers)
data = response.json()
print(data)
```

**Kotlin (Ktor Client):**
```kotlin
val apiKey = "your-api-key-here"
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}

val response: TriggerResponseDto = client.post("http://localhost:8080/api/trigger") {
    header(HttpHeaders.Authorization, "Bearer $apiKey")
}.body()

if (response.success) {
    println("Success: ${response.message}")
} else {
    println("Error: ${response.message}")
}
```

#### Response

**Success Response (200 OK):**

```json
{
  "success": true,
  "message": "Currency rates updated and sent to Telegram successfully"
}
```

**Error Response (500 Internal Server Error):**

Возвращается когда произошла ошибка при парсинге, сохранении или отправке:

```json
{
  "success": false,
  "message": "Failed to update currency rates: Connection timeout"
}
```

**Error Response (401 Unauthorized):**

```json
{
  "error": "Missing API key"
}
```

или

```json
{
  "error": "Invalid API key"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `success` | Boolean | Статус выполнения запроса |
| `message` | String | Сообщение о результате выполнения или описание ошибки |

#### Status Codes

| Code | Description |
|------|-------------|
| 200 | Успешное выполнение - курсы обновлены, записаны в БД и отправлены в Telegram |
| 401 | Отсутствует или недействителен API ключ |
| 500 | Внутренняя ошибка сервера (проблема с парсингом, БД или Telegram API) |

## Authentication

API использует систему управления API ключами [Unkey](https://unkey.com). Для доступа к endpoint необходимо предоставить валидный API ключ в заголовке `Authorization`.

### Получение API ключа

1. API ключи управляются через Unkey dashboard
2. Администратор может создать новый API ключ с необходимыми правами доступа
3. Ключ должен быть передан в заголовке запроса как Bearer token

### Использование API ключа

Все запросы должны включать заголовок:
```
Authorization: Bearer your-api-key-here
```

## Примеры использования

### cURL

```bash
# Запустить обновление курсов
curl -X POST "http://localhost:8080/api/trigger" \
  -H "Authorization: Bearer your-api-key-here"

# С красивым выводом JSON
curl -X POST "http://localhost:8080/api/trigger" \
  -H "Authorization: Bearer your-api-key-here" | jq '.'
```

### JavaScript (fetch) с обработкой ошибок

```javascript
const apiKey = 'your-api-key-here';

async function triggerCurrencyUpdate() {
  try {
    const response = await fetch('http://localhost:8080/api/trigger', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${apiKey}`
      }
    });

    const data = await response.json();

    if (response.ok && data.success) {
      console.log('✓ Success:', data.message);
    } else {
      console.error('✗ Error:', data.message);
    }
  } catch (error) {
    console.error('Request failed:', error);
  }
}

triggerCurrencyUpdate();
```

### Python (requests) с обработкой ошибок

```python
import requests

api_key = 'your-api-key-here'
headers = {
    'Authorization': f'Bearer {api_key}'
}

try:
    response = requests.post('http://localhost:8080/api/trigger', headers=headers)
    data = response.json()

    if response.status_code == 200 and data['success']:
        print(f"✓ Success: {data['message']}")
    else:
        print(f"✗ Error: {data['message']}")
except requests.exceptions.RequestException as e:
    print(f"Request failed: {e}")
```

### Kotlin (Ktor Client) с обработкой ошибок

```kotlin
val apiKey = "your-api-key-here"
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}

try {
    val httpResponse = client.post("http://localhost:8080/api/trigger") {
        header(HttpHeaders.Authorization, "Bearer $apiKey")
    }

    when (httpResponse.status) {
        HttpStatusCode.OK -> {
            val response: TriggerResponseDto = httpResponse.body()
            if (response.success) {
                println("✓ Success: ${response.message}")
            } else {
                println("✗ Error: ${response.message}")
            }
        }
        HttpStatusCode.Unauthorized -> {
            println("✗ Error: Unauthorized - invalid API key")
        }
        else -> {
            val response: TriggerResponseDto = httpResponse.body()
            println("✗ Error: ${response.message}")
        }
    }
} catch (e: Exception) {
    println("Request failed: ${e.message}")
}
```

## Архитектура

API endpoint реализован следуя Clean Architecture паттерну:

### Компоненты

1. **Routing.kt** (Presentation Layer)
   - HTTP endpoint definition (POST /api/trigger)
   - Роутинг запросов к контроллеру
   - Unkey authentication

2. **TriggerController** (Presentation Layer)
   - Обработка HTTP запросов/ответов
   - `triggerCurrencyUpdate()` - обработка POST /api/trigger
   - Логирование с MDC контекстом
   - HTTP status codes (200/500)

3. **SendCurrencyRatesUseCase** (Domain Layer)
   - Бизнес-логика обновления курсов
   - Координирует работу репозиториев:
     - Получение курсов (CurrencyRepository)
     - Сохранение в историю (CurrencyHistoryRepository)
     - Отправка в Telegram (TelegramRepository)
   - Логирование операций

4. **Repositories** (Data Layer)
   - `CurrencyRepository` - парсинг курсов с kurs.kz
   - `CurrencyHistoryRepository` - сохранение в H2 БД
   - `TelegramRepository` - отправка сообщения в Telegram

5. **DTO Models** (Data Layer)
   - `TriggerResponseDto` - ответ для POST /api/trigger

### Data Flow

```
HTTP POST /api/trigger
  ↓
Routing.kt (route definition)
  ↓
TriggerController.triggerCurrencyUpdate()
  ↓
SendCurrencyRatesUseCase.invoke(chatId)
  ↓
┌─────────────────────────────────────┐
│  1. CurrencyRepository              │
│     ↓ Parse rates from kurs.kz     │
│  2. CurrencyHistoryRepository       │
│     ↓ Save to H2 database           │
│  3. TelegramRepository              │
│     ↓ Send message to Telegram      │
└─────────────────────────────────────┘
  ↓
TriggerResponseDto (DTO)
  ↓
JSON Response (200 OK or 500 Error)
```

## Use Cases

### Когда использовать этот endpoint

1. **Тестирование** - проверка работы парсинга, БД и Telegram интеграции
2. **Экстренное обновление** - получение актуальных курсов вне расписания
3. **Debugging** - диагностика проблем с автоматическим обновлением
4. **Manual refresh** - ручное обновление при изменении курсов
5. **Initial setup** - первое заполнение базы данных после деплоя

### Рекомендации

- ❗ **Не вызывайте слишком часто** - каждый запрос парсит внешний сайт и отправляет сообщение в Telegram
- ⚠️ **Rate limiting** - рекомендуется не более 1 запроса в 5 минут
- ✅ **Idempotent** - безопасно вызывать повторно, каждый раз создается новая запись в БД
- 🔒 **Protected** - требуется валидный API ключ

## Тестирование

API покрыт integration тестами:

**TriggerApiTest.kt** - 5 тестов:
  - HTTP 200 при успешном выполнении
  - HTTP 500 при ошибке use case
  - HTTP 401 без аутентификации
  - HTTP 401 с невалидным API ключом
  - Корректный JSON формат

### Запуск тестов

```bash
# Все integration тесты для trigger API
./gradlew test --tests "TriggerApiTest"

# Все API тесты
./gradlew test --tests "dev.proflyder.currency.api.*"
```

## Мониторинг

### Логирование

API использует structured logging с MDC контекстом:

```kotlin
withLoggingContext(mapOf("request_id" to UUID.randomUUID().toString())) {
    logger.info("POST /api/trigger - Manual trigger for currency update")
    // ...
    logger.info("Currency update triggered successfully in ${duration}ms")
}
```

Логи в формате JSON (Logstash encoder):

```json
{
  "timestamp": "2025-12-01T14:40:51.128Z",
  "level": "INFO",
  "logger": "dev.proflyder.currency.presentation.controller.TriggerController",
  "message": "POST /api/trigger - Manual trigger for currency update",
  "request_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Метрики

Рекомендуется отслеживать:
- **Время выполнения** - Duration от вызова до завершения (обычно 2-5 секунд)
- **Частота ошибок** - HTTP 500 rate
- **Частота вызовов** - Количество ручных триггеров за период
- **Success rate** - Процент успешных обновлений

## Безопасность

### Текущее состояние

- ✅ **Аутентификация через Unkey** (Bearer Token required)
- ✅ API ключ обязателен
- ✅ **Мутация данных** - создает записи в БД и отправляет сообщения
- ⚠️ **Rate limiting отсутствует** - можно вызывать неограниченно часто
- ⚠️ **CORS не настроен**

### Рекомендации для продакшена

1. **Rate Limiting** - Ограничить до 1 запроса в 5 минут per API key (критично!)
2. **Audit Logging** - Логировать все вызовы с user_id и timestamp
3. **HTTPS** - Использовать только HTTPS в продакшене (обязательно!)
4. **Мониторинг** - Отслеживать аномальную активность и abuse
5. **API Key Permissions** - Отдельные permissions для trigger endpoint
6. **Request timeout** - Максимальное время выполнения 30 секунд

### Потенциальные риски

- **Spam** - Частые вызовы могут заспамить Telegram канал
- **Resource exhaustion** - Множественные параллельные запросы могут нагрузить парсер и БД
- **External dependency** - Зависит от доступности kurs.kz и Telegram API
- **Cost** - Каждый вызов = HTTP запрос к внешнему сайту и Telegram API

---

**Последнее обновление:** 2025-12-01
**Версия API:** 1.0.0
**Статус:** Stable
