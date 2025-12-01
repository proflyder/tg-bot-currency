# Currency History API

REST API для получения истории курсов валют из базы данных H2.

## Обзор

API предоставляет endpoint для получения полной истории курсов валют, которые были сохранены ботом. История включает все записи курсов USD→KZT и RUB→KZT с временными метками.

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

# Красивый вывод с jq
curl -X GET "http://localhost:8080/api/history" \
  -H "Authorization: Bearer your-api-key-here" | jq '.'
```

### JavaScript (fetch)

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

### Python (requests)

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

### Kotlin (Ktor Client)

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

## Архитектура

API endpoint реализован следуя Clean Architecture паттерну:

### Компоненты

1. **Routing.kt** (Presentation Layer)
   - HTTP endpoint definition
   - Роутинг запросов к контроллерам

2. **CurrencyHistoryController** (Presentation Layer)
   - Обработка HTTP запросов/ответов
   - Логирование с MDC контекстом
   - Преобразование Domain models → DTO
   - HTTP status codes

3. **GetCurrencyHistoryUseCase** (Domain Layer)
   - Бизнес-логика получения истории
   - Вызов репозитория
   - Логирование операций

4. **CurrencyHistoryRepository** (Data Layer)
   - `getAllRecords()` - получение всех записей из H2 БД
   - Сортировка по времени (DESC)
   - Маппинг Row → CurrencyRateRecord

5. **DTO Models** (Data Layer)
   - `CurrencyHistoryResponseDto` - основной ответ API
   - `CurrencyHistoryDataDto` - данные истории
   - `CurrencyRateRecordDto` - одна запись курса
   - Extension функции для конвертации Domain → DTO

### Data Flow

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

## Производительность

### Оптимизации

- **Индекс на timestamp:** Быстрая сортировка записей
- **Одиночный запрос:** Все данные получаются за один SELECT
- **Прямой маппинг:** Row → Model без промежуточных преобразований

### Ограничения

- **Полный список:** API возвращает ВСЕ записи из базы
- **Нет пагинации:** Для больших объемов данных может быть медленно
- **Память:** Весь список загружается в память

### Рекомендации

Для больших объемов данных (>10000 записей) рассмотрите:
- Добавление пагинации (`?page=1&limit=100`)
- Фильтрацию по дате (`?from=2025-11-01&to=2025-11-30`)
- Streaming response для очень больших наборов данных

## Тестирование

API покрыт тестами:

### Unit тесты

- `GetCurrencyHistoryUseCaseTest.kt` - 13 тестов
  - Успешное получение записей
  - Пустая история
  - Обработка ошибок БД
  - Большие объемы данных

### Integration тесты

- `CurrencyHistoryApiTest.kt` - 4 теста
  - HTTP 200 с записями
  - HTTP 200 с пустым списком
  - HTTP 500 при ошибке
  - Корректный JSON формат

Запуск тестов:

```bash
./gradlew test --tests "*.GetCurrencyHistoryUseCaseTest"
./gradlew test --tests "*.CurrencyHistoryApiTest"
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

- ✅ Read-only endpoint (GET)
- ✅ Нет мутаций данных
- ✅ Нет аутентификации (публичный endpoint)
- ⚠️ CORS не настроен
- ⚠️ Rate limiting отсутствует
- ⚠️ API ключ не требуется

### Рекомендации для продакшена

1. **Аутентификация:** Добавить API ключ или JWT
2. **Rate Limiting:** Ограничить количество запросов (напр. 100/час)
3. **CORS:** Настроить для фронтенда
4. **HTTPS:** Использовать только HTTPS в продакшене
5. **Мониторинг:** Отслеживать подозрительную активность

---

**Последнее обновление:** 2025-12-01
**Версия API:** 1.0.0
**Статус:** Stable
