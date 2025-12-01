# Currency History API

REST API для получения истории курсов валют из базы данных H2.

## Обзор

API предоставляет endpoints для получения истории курсов валют из базы данных H2:
- **GET /api/history** - Полная история всех курсов валют
- **GET /api/latest** - Последний актуальный курс валют

История включает записи курсов USD→KZT и RUB→KZT с временными метками.

## Endpoints

### GET /api/history

Получить полную историю курсов валют.

**URL:** `/api/history`

**Method:** `GET`

**Authentication:** Требуется (Bearer Token)

#### Request Headers

| Header | Value | Description |
|--------|-------|-------------|
| `Authorization` | `Bearer <api_key>` | API ключ для аутентификации через Unkey |

#### Example Request

**cURL:**
```bash
curl -X GET "http://localhost:8080/api/history" \
  -H "Authorization: Bearer your-api-key-here"
```

**JavaScript (fetch):**
```javascript
fetch('http://localhost:8080/api/history', {
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

response = requests.get('http://localhost:8080/api/history', headers=headers)
data = response.json()
print(data)
```

#### Response

**Success Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "records": [
      {
        "timestamp": "2025-11-30T12:00:00Z",
        "rates": {
          "usdToKzt": {
            "buy": 485.50,
            "sell": 487.20
          },
          "rubToKzt": {
            "buy": 4.85,
            "sell": 4.92
          }
        }
      },
      {
        "timestamp": "2025-11-30T11:00:00Z",
        "rates": {
          "usdToKzt": {
            "buy": 485.00,
            "sell": 486.70
          },
          "rubToKzt": {
            "buy": 4.83,
            "sell": 4.90
          }
        }
      }
    ],
    "totalCount": 2
  },
  "message": "Currency history fetched successfully"
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

**Error Response (500 Internal Server Error):**

```json
{
  "success": false,
  "data": {
    "records": [],
    "totalCount": 0
  },
  "message": "Failed to fetch currency history: Database error"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `success` | Boolean | Статус выполнения запроса |
| `data` | Object | Объект с данными истории |
| `data.records` | Array | Массив записей курсов валют |
| `data.totalCount` | Integer | Общее количество записей |
| `message` | String | Сообщение о результате выполнения |

#### Record Fields

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | String (ISO 8601) | Дата и время записи курса |
| `rates` | Object | Объект с курсами валют |
| `rates.usdToKzt` | Object | Курс USD → KZT |
| `rates.usdToKzt.buy` | Double | Курс покупки USD |
| `rates.usdToKzt.sell` | Double | Курс продажи USD |
| `rates.rubToKzt` | Object | Курс RUB → KZT |
| `rates.rubToKzt.buy` | Double | Курс покупки RUB |
| `rates.rubToKzt.sell` | Double | Курс продажи RUB |

#### Status Codes

| Code | Description |
|------|-------------|
| 200 | Успешный запрос, данные возвращены |
| 401 | Отсутствует или недействителен API ключ |
| 500 | Внутренняя ошибка сервера (проблема с БД) |

### GET /api/latest

Получить последний актуальный курс валют (самая свежая запись из базы данных).

**URL:** `/api/latest`

**Method:** `GET`

**Authentication:** Требуется (Bearer Token)

#### Request Headers

| Header | Value | Description |
|--------|-------|-------------|
| `Authorization` | `Bearer <api_key>` | API ключ для аутентификации через Unkey |

#### Example Request

**cURL:**
```bash
curl -X GET "http://localhost:8080/api/latest" \
  -H "Authorization: Bearer your-api-key-here"
```

**JavaScript (fetch):**
```javascript
fetch('http://localhost:8080/api/latest', {
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

response = requests.get('http://localhost:8080/api/latest', headers=headers)
data = response.json()
print(data)
```

#### Response

**Success Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "timestamp": "2025-11-30T12:00:00Z",
    "rates": {
      "usdToKzt": {
        "buy": 485.50,
        "sell": 487.20
      },
      "rubToKzt": {
        "buy": 4.85,
        "sell": 4.92
      }
    }
  },
  "message": "Latest currency rate fetched successfully"
}
```

**Not Found Response (404 Not Found):**

Возвращается когда база данных пуста (нет записей):

```json
{
  "success": false,
  "data": null,
  "message": "No currency rates found"
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

**Error Response (500 Internal Server Error):**

```json
{
  "success": false,
  "data": null,
  "message": "Failed to fetch latest currency rate: Database connection error"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `success` | Boolean | Статус выполнения запроса |
| `data` | Object или null | Объект с последней записью курса, или null если записей нет |
| `data.timestamp` | String (ISO 8601) | Дата и время записи курса |
| `data.rates` | Object | Объект с курсами валют |
| `data.rates.usdToKzt` | Object | Курс USD → KZT |
| `data.rates.usdToKzt.buy` | Double | Курс покупки USD |
| `data.rates.usdToKzt.sell` | Double | Курс продажи USD |
| `data.rates.rubToKzt` | Object | Курс RUB → KZT |
| `data.rates.rubToKzt.buy` | Double | Курс покупки RUB |
| `data.rates.rubToKzt.sell` | Double | Курс продажи RUB |
| `message` | String | Сообщение о результате выполнения |

#### Status Codes

| Code | Description |
|------|-------------|
| 200 | Успешный запрос, последний курс возвращен |
| 404 | База данных пуста, записей нет |
| 401 | Отсутствует или недействителен API ключ |
| 500 | Внутренняя ошибка сервера (проблема с БД) |

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
# Получить всю историю
curl -X GET "http://localhost:8080/api/history" \
  -H "Authorization: Bearer your-api-key-here"

# Получить последний актуальный курс
curl -X GET "http://localhost:8080/api/latest" \
  -H "Authorization: Bearer your-api-key-here"

# Красивый вывод с jq
curl -X GET "http://localhost:8080/api/history" \
  -H "Authorization: Bearer your-api-key-here" | jq '.'

curl -X GET "http://localhost:8080/api/latest" \
  -H "Authorization: Bearer your-api-key-here" | jq '.'
```

### JavaScript (fetch)

**Получение полной истории:**
```javascript
const apiKey = 'your-api-key-here';

fetch('http://localhost:8080/api/history', {
  headers: {
    'Authorization': `Bearer ${apiKey}`
  }
})
  .then(response => response.json())
  .then(data => {
    if (data.success) {
      console.log(`Total records: ${data.data.totalCount}`);
      data.data.records.forEach(record => {
        console.log(`${record.timestamp}: USD ${record.rates.usdToKzt.sell} ₸`);
      });
    } else {
      console.error('Error:', data.message);
    }
  })
  .catch(error => console.error('Request failed:', error));
```

**Получение последнего курса:**
```javascript
const apiKey = 'your-api-key-here';

fetch('http://localhost:8080/api/latest', {
  headers: {
    'Authorization': `Bearer ${apiKey}`
  }
})
  .then(response => response.json())
  .then(data => {
    if (data.success && data.data) {
      const rate = data.data;
      console.log(`Latest rate (${rate.timestamp}):`);
      console.log(`  USD: ${rate.rates.usdToKzt.buy} / ${rate.rates.usdToKzt.sell} ₸`);
      console.log(`  RUB: ${rate.rates.rubToKzt.buy} / ${rate.rates.rubToKzt.sell} ₸`);
    } else if (response.status === 404) {
      console.log('No currency rates found in database');
    } else {
      console.error('Error:', data.message);
    }
  })
  .catch(error => console.error('Request failed:', error));
```

### Python (requests)

**Получение полной истории:**
```python
import requests

api_key = 'your-api-key-here'
headers = {
    'Authorization': f'Bearer {api_key}'
}

response = requests.get('http://localhost:8080/api/history', headers=headers)
data = response.json()

if data['success']:
    print(f"Total records: {data['data']['totalCount']}")
    for record in data['data']['records']:
        timestamp = record['timestamp']
        usd_rate = record['rates']['usdToKzt']['sell']
        print(f"{timestamp}: USD {usd_rate} ₸")
else:
    print(f"Error: {data['message']}")
```

**Получение последнего курса:**
```python
import requests

api_key = 'your-api-key-here'
headers = {
    'Authorization': f'Bearer {api_key}'
}

response = requests.get('http://localhost:8080/api/latest', headers=headers)
data = response.json()

if response.status_code == 200 and data['success'] and data['data']:
    rate = data['data']
    print(f"Latest rate ({rate['timestamp']}):")
    print(f"  USD: {rate['rates']['usdToKzt']['buy']} / {rate['rates']['usdToKzt']['sell']} ₸")
    print(f"  RUB: {rate['rates']['rubToKzt']['buy']} / {rate['rates']['rubToKzt']['sell']} ₸")
elif response.status_code == 404:
    print("No currency rates found in database")
else:
    print(f"Error: {data['message']}")
```

### Kotlin (Ktor Client)

**Получение полной истории:**
```kotlin
val apiKey = "your-api-key-here"
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}

val response: CurrencyHistoryResponseDto = client.get("http://localhost:8080/api/history") {
    header(HttpHeaders.Authorization, "Bearer $apiKey")
}.body()

if (response.success) {
    println("Total records: ${response.data.totalCount}")
    response.data.records.forEach { record ->
        println("${record.timestamp}: USD ${record.rates.usdToKzt.sell} ₸")
    }
} else {
    println("Error: ${response.message}")
}
```

**Получение последнего курса:**
```kotlin
val apiKey = "your-api-key-here"
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}

val httpResponse = client.get("http://localhost:8080/api/latest") {
    header(HttpHeaders.Authorization, "Bearer $apiKey")
}

when (httpResponse.status) {
    HttpStatusCode.OK -> {
        val response: LatestCurrencyRateResponseDto = httpResponse.body()
        response.data?.let { rate ->
            println("Latest rate (${rate.timestamp}):")
            println("  USD: ${rate.rates.usdToKzt.buy} / ${rate.rates.usdToKzt.sell} ₸")
            println("  RUB: ${rate.rates.rubToKzt.buy} / ${rate.rates.rubToKzt.sell} ₸")
        }
    }
    HttpStatusCode.NotFound -> {
        println("No currency rates found in database")
    }
    else -> {
        val response: LatestCurrencyRateResponseDto = httpResponse.body()
        println("Error: ${response.message}")
    }
}
```

## Архитектура

API endpoints реализованы следуя Clean Architecture паттерну:

### Компоненты

1. **Routing.kt** (Presentation Layer)
   - HTTP endpoint definitions (GET /api/history, GET /api/latest)
   - Роутинг запросов к контроллерам
   - Unkey authentication для обоих endpoints

2. **CurrencyHistoryController** (Presentation Layer)
   - Обработка HTTP запросов/ответов для обоих endpoints
   - `getHistory()` - обработка GET /api/history
   - `getLatest()` - обработка GET /api/latest
   - Логирование с MDC контекстом
   - Преобразование Domain models → DTO
   - HTTP status codes (200/404/500)

3. **Use Cases** (Domain Layer)
   - `GetCurrencyHistoryUseCase` - бизнес-логика получения истории
   - `GetLatestCurrencyRateUseCase` - бизнес-логика получения последнего курса
   - Вызов репозитория
   - Логирование операций

4. **CurrencyHistoryRepository** (Data Layer)
   - `getAllRecords()` - получение всех записей из H2 БД
   - `getLatestRecord()` - получение последней записи (ORDER BY timestamp DESC LIMIT 1)
   - Сортировка по времени (DESC)
   - Маппинг Row → CurrencyRateRecord

5. **DTO Models** (Data Layer)
   - `CurrencyHistoryResponseDto` - ответ для GET /api/history
   - `LatestCurrencyRateResponseDto` - ответ для GET /api/latest
   - `CurrencyHistoryDataDto` - данные истории
   - `CurrencyRateRecordDto` - одна запись курса
   - Extension функции для конвертации Domain → DTO

### Data Flow

**GET /api/history:**
```
HTTP GET /api/history
  ↓
Routing.kt (route definition)
  ↓
CurrencyHistoryController.getHistory()
  ↓
GetCurrencyHistoryUseCase.invoke()
  ↓
CurrencyHistoryRepository.getAllRecords()
  ↓
H2 Database (SELECT * FROM currency_history ORDER BY timestamp DESC)
  ↓
CurrencyRateRecord (Domain Model)
  ↓
CurrencyRateRecordDto (DTO)
  ↓
JSON Response
```

**GET /api/latest:**
```
HTTP GET /api/latest
  ↓
Routing.kt (route definition)
  ↓
CurrencyHistoryController.getLatest()
  ↓
GetLatestCurrencyRateUseCase.invoke()
  ↓
CurrencyHistoryRepository.getLatestRecord()
  ↓
H2 Database (SELECT * FROM currency_history ORDER BY timestamp DESC LIMIT 1)
  ↓
CurrencyRateRecord? (Domain Model, nullable)
  ↓
CurrencyRateRecordDto? (DTO, nullable)
  ↓
JSON Response (200 with data, or 404 if null)
```

## Производительность

### Оптимизации

**GET /api/history:**
- **Индекс на timestamp:** Быстрая сортировка записей
- **Одиночный запрос:** Все данные получаются за один SELECT
- **Прямой маппинг:** Row → Model без промежуточных преобразований

**GET /api/latest:**
- **LIMIT 1:** Только одна запись возвращается из базы
- **Индекс на timestamp:** Очень быстрый поиск максимального timestamp
- **Минимальный overhead:** Один запрос, одна запись
- **Константная сложность:** O(1) независимо от размера таблицы

### Ограничения

**GET /api/history:**
- **Полный список:** API возвращает ВСЕ записи из базы
- **Нет пагинации:** Для больших объемов данных может быть медленно
- **Память:** Весь список загружается в память

**GET /api/latest:**
- **Только последняя запись:** Нет доступа к истории
- **Нет фильтрации:** Всегда возвращается самая свежая запись

### Рекомендации

**Для GET /api/history** при больших объемах данных (>10000 записей):
- Добавление пагинации (`?page=1&limit=100`)
- Фильтрацию по дате (`?from=2025-11-01&to=2025-11-30`)
- Streaming response для очень больших наборов данных

**Для GET /api/latest:**
- ✅ Оптимален для текущего use case
- Рассмотреть кэширование на уровне приложения (TTL 5-10 минут)

## Тестирование

API покрыт тестами:

### Unit тесты

**GetCurrencyHistoryUseCaseTest.kt** - 13 тестов
  - Успешное получение записей
  - Пустая история
  - Обработка ошибок БД
  - Большие объемы данных

**GetLatestCurrencyRateUseCaseTest.kt** - 10 тестов
  - Успешное получение последней записи
  - Возврат null при пустой БД
  - Обработка ошибок БД
  - Граничные случаи (минимальные/максимальные значения)

### Integration тесты

**CurrencyHistoryApiTest.kt** - 9 тестов

*GET /api/history (4 теста):*
  - HTTP 200 с записями
  - HTTP 200 с пустым списком
  - HTTP 500 при ошибке
  - Корректный JSON формат

*GET /api/latest (5 тестов):*
  - HTTP 200 с последней записью
  - HTTP 404 при пустой БД
  - HTTP 500 при ошибке
  - HTTP 401 без аутентификации
  - Корректный JSON формат

### Запуск тестов

```bash
# Все unit тесты для use cases
./gradlew test --tests "*.GetCurrencyHistoryUseCaseTest"
./gradlew test --tests "*.GetLatestCurrencyRateUseCaseTest"

# Все integration тесты для API
./gradlew test --tests "*.CurrencyHistoryApiTest"

# Все тесты для Currency History API
./gradlew test --tests "*CurrencyHistory*"
```

## Мониторинг

### Логирование

API использует structured logging с MDC контекстом:

```kotlin
withLoggingContext(mapOf("request_id" to UUID.randomUUID().toString())) {
    logger.info("GET /api/history - Fetching currency history")
    // ...
    logger.info("Successfully fetched ${records.size} records")
}
```

Логи в формате JSON (Logstash encoder):

```json
{
  "timestamp": "2025-12-01T14:40:51.128Z",
  "level": "INFO",
  "logger": "io.ktor.server.application.Application",
  "message": "GET /api/history - Fetching currency history",
  "request_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Метрики

Рекомендуется отслеживать:
- **Время ответа:** Latency endpoint
- **Количество записей:** Размер response
- **Частота ошибок:** HTTP 500 rate
- **Размер БД:** Рост таблицы currency_history

## Безопасность

### Текущее состояние

- ✅ Read-only endpoints (GET only)
- ✅ Нет мутаций данных
- ✅ **Аутентификация через Unkey** (Bearer Token required)
- ✅ API ключ обязателен для всех endpoints
- ⚠️ CORS не настроен
- ⚠️ Rate limiting отсутствует

### Рекомендации для продакшена

1. **Rate Limiting:** Ограничить количество запросов (напр. 100 req/hour per API key)
2. **CORS:** Настроить для фронтенда с whitelist доменов
3. **HTTPS:** Использовать только HTTPS в продакшене (обязательно!)
4. **Мониторинг:** Отслеживать подозрительную активность и abuse
5. **API Key Rotation:** Регулярная ротация API ключей
6. **Audit Logging:** Логирование всех API запросов с metadata

---

**Последнее обновление:** 2025-12-01
**Версия API:** 1.1.0
**Статус:** Stable

**Изменения в версии 1.1.0:**
- ✅ Добавлен endpoint GET /api/latest
- ✅ Добавлена Unkey аутентификация для всех endpoints
- ✅ Улучшена обработка ошибок (404 для пустой БД в /api/latest)
