# Telegram Webhook API

## Endpoint

```
POST /telegram/webhook
```

Receives webhook updates from Telegram Bot API.

## Authentication

**No authentication required** - This endpoint is publicly accessible as Telegram sends updates directly.

## Request

### Headers

```
Content-Type: application/json
```

### Body

The request body follows the Telegram Update object structure:

```json
{
  "update_id": 123456789,
  "message": {
    "message_id": 1,
    "chat": {
      "id": 987654321,
      "type": "private",
      "first_name": "John",
      "last_name": "Doe"
    },
    "text": "/trigger"
  }
}
```

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `update_id` | Long | Unique identifier for this update |
| `message` | Object (optional) | New incoming message of any kind |
| `message.message_id` | Long | Unique message identifier |
| `message.chat` | Object | Conversation the message belongs to |
| `message.chat.id` | Long | Unique identifier for this chat |
| `message.chat.type` | String | Type of chat: "private", "group", "supergroup", or "channel" |
| `message.text` | String (optional) | Text of the message |

## Response

### Success Response

**Code:** `200 OK`

**Body:**
```json
"OK"
```

### Error Response

**Code:** `500 Internal Server Error`

**Body:**
```json
"ERROR"
```

**Cause:** Exception occurred while processing the update

## Supported Commands

The webhook processes the following bot commands:

### `/trigger`

Triggers a manual currency rate update by calling the internal `POST /api/trigger` endpoint.

**Bot Response:**
- On success: Currency rates are sent directly to the configured channel (no confirmation message to user)
- On failure: `❌ Не удалось обновить курсы: <error message>`

### `/start`

Sends a welcome message with available commands.

**Bot Response:**
```
👋 Привет! Я бот для отслеживания курсов валют.

Доступные команды:
/trigger - Получить актуальный курс валют
/help - Показать справку
```

### `/help`

Displays help information about bot commands.

**Bot Response:**
```
📖 Справка по командам бота:

/trigger - Принудительно обновить и получить актуальные курсы USD→KZT и RUB→KZT
/start - Показать приветственное сообщение
/help - Показать эту справку

ℹ️ Бот автоматически отправляет курсы валют каждый час.
```

## Example Requests

### cURL

#### Send a message with /trigger command

```bash
curl -X POST http://localhost:8080/telegram/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "update_id": 123456789,
    "message": {
      "message_id": 1,
      "chat": {
        "id": 987654321,
        "type": "private",
        "first_name": "John",
        "last_name": "Doe"
      },
      "text": "/trigger"
    }
  }'
```

#### Send a message without command

```bash
curl -X POST http://localhost:8080/telegram/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "update_id": 123456790,
    "message": {
      "message_id": 2,
      "chat": {
        "id": 987654321,
        "type": "private",
        "first_name": "John",
        "last_name": "Doe"
      },
      "text": "Hello bot!"
    }
  }'
```

### JavaScript (fetch)

```javascript
const sendTelegramUpdate = async (text) => {
  const update = {
    update_id: Date.now(),
    message: {
      message_id: 1,
      chat: {
        id: 987654321,
        type: "private",
        first_name: "John",
        last_name: "Doe"
      },
      text: text
    }
  };

  const response = await fetch('http://localhost:8080/telegram/webhook', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(update)
  });

  return await response.text();
};

// Usage
await sendTelegramUpdate('/trigger');
```

### Python

```python
import requests
import time

def send_telegram_update(text):
    update = {
        "update_id": int(time.time() * 1000),
        "message": {
            "message_id": 1,
            "chat": {
                "id": 987654321,
                "type": "private",
                "first_name": "John",
                "last_name": "Doe"
            },
            "text": text
        }
    }

    response = requests.post(
        'http://localhost:8080/telegram/webhook',
        json=update
    )

    return response.text

# Usage
result = send_telegram_update('/trigger')
print(result)
```

### Kotlin

```kotlin
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Chat(
    val id: Long,
    val type: String,
    val firstName: String? = null,
    val lastName: String? = null
)

@Serializable
data class Message(
    val messageId: Long,
    val chat: Chat,
    val text: String? = null
)

@Serializable
data class Update(
    val updateId: Long,
    val message: Message? = null
)

suspend fun sendTelegramUpdate(client: HttpClient, text: String): String {
    val update = Update(
        updateId = System.currentTimeMillis(),
        message = Message(
            messageId = 1,
            chat = Chat(
                id = 987654321,
                type = "private",
                firstName = "John",
                lastName = "Doe"
            ),
            text = text
        )
    )

    val response: HttpResponse = client.post("http://localhost:8080/telegram/webhook") {
        contentType(ContentType.Application.Json)
        setBody(update)
    }

    return response.bodyAsText()
}

// Usage
val result = sendTelegramUpdate(httpClient, "/trigger")
println(result)
```

## Implementation Details

### Controller

`TelegramWebhookController.kt` handles incoming webhook requests:

```kotlin
suspend fun handleWebhook(call: RoutingCall) {
    try {
        val update = call.receive<TelegramUpdate>()

        update.message?.let { message ->
            commandHandler.handleMessage(message)
        }

        call.respond(HttpStatusCode.OK, "OK")
    } catch (e: Exception) {
        logger.error("Failed to handle Telegram webhook", e)
        call.respond(HttpStatusCode.InternalServerError, "ERROR")
    }
}
```

### Command Handler

`TelegramCommandHandler.kt` processes bot commands:

```kotlin
suspend fun handleMessage(message: TelegramMessage) {
    val text = message.text?.trim() ?: return
    val chatId = message.chat.id.toString()

    when {
        text.startsWith("/trigger") -> handleCurrentCommand(chatId)
        text.startsWith("/start") -> handleStartCommand(chatId)
        text.startsWith("/help") -> handleHelpCommand(chatId)
        else -> logger.debug("Ignoring non-command message: $text")
    }
}
```

## Architecture

```
┌──────────────┐
│   Telegram   │
│   Servers    │
└──────┬───────┘
       │ POST /telegram/webhook
       ▼
┌──────────────────────┐
│ TelegramWebhook      │
│ Controller           │
│ - Receives updates   │
│ - Extracts message   │
│ - Returns 200 OK     │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ TelegramCommand      │
│ Handler              │
│ - Parses commands    │
│ - Routes to handlers │
└──────────┬───────────┘
           │
           ├─→ /trigger  ──→ TriggerApiClient ──→ POST /api/trigger
           ├─→ /start    ──→ Send welcome message
           └─→ /help     ──→ Send help message
```

## Error Handling

The endpoint handles errors gracefully:

1. **Invalid JSON**: Returns 500 with "ERROR"
2. **Missing fields**: Ignores updates without messages
3. **Command handler exception**: Catches and logs, returns 500
4. **Empty text**: Ignores messages with no text

## Testing

Run the integration tests:

```bash
./gradlew test --tests "dev.proflyder.currency.api.TelegramWebhookApiTest"
```

Test coverage includes:
- ✅ Successful webhook processing
- ✅ Command handling (/trigger, /start, /help)
- ✅ Updates without messages
- ✅ Messages without text
- ✅ Non-command messages
- ✅ Error handling
- ✅ No authentication requirement

## Related Endpoints

- [POST /api/trigger](trigger-api.md) - Manually trigger currency update (called by /trigger command)
- [GET /api/history](currency-history-api.md) - Get currency history
- [GET /api/latest](currency-history-api.md) - Get latest currency rates

## Configuration

Required environment variables:

```bash
BOT_TOKEN=your-telegram-bot-token
CHAT_ID=your-telegram-chat-id
INTERNAL_API_KEY=your-internal-api-key
```

## Security Notes

- **No authentication required**: Telegram sends updates to this endpoint
- **HTTPS required**: In production, use HTTPS (Telegram requirement)
- **Internal API calls**: Bot uses `INTERNAL_API_KEY` to call `/api/trigger`
- **Rate limiting**: Consider implementing rate limiting to prevent abuse

## Further Reading

- [Telegram Bot Setup Guide](../guides/telegram-bot-setup.md) - Complete setup instructions
- [Telegram Bot API Documentation](https://core.telegram.org/bots/api) - Official API reference
- [Webhook Best Practices](https://core.telegram.org/bots/webhooks) - Telegram's webhook guide
