# Telegram Bot Setup Guide

This guide explains how to set up and configure the Telegram bot for currency rate notifications.

## Overview

The bot uses a **webhook-based architecture** where Telegram sends updates directly to the server via HTTP POST requests. This is more efficient than polling and provides instant message delivery.

## Architecture

```
┌─────────────┐         POST /telegram/webhook         ┌──────────────────┐
│   Telegram  │ ─────────────────────────────────────> │  Ktor Server     │
│   Servers   │                                          │  (No Auth)       │
└─────────────┘                                          └──────────────────┘
                                                                   │
                                                                   ▼
                                                         ┌──────────────────┐
                                                         │ TelegramWebhook  │
                                                         │   Controller     │
                                                         └──────────────────┘
                                                                   │
                                                                   ▼
                                                         ┌──────────────────┐
                                                         │ TelegramCommand  │
                                                         │    Handler       │
                                                         └──────────────────┘
                                                                   │
                          ┌────────────────────────────────────────┼─────────────────┐
                          ▼                                        ▼                 ▼
                   /trigger command                        /start command     /help command
                          │                                        │                 │
                          ▼                                        ▼                 ▼
              ┌────────────────────┐                    ┌──────────────┐   ┌──────────────┐
              │  TriggerApiClient  │                    │ Send welcome │   │ Send help    │
              │  POST /api/trigger │                    │   message    │   │   message    │
              └────────────────────┘                    └──────────────┘   └──────────────┘
```

## Available Commands

### `/trigger`
Triggers a manual update of currency rates and sends the latest USD→KZT and RUB→KZT rates to the channel.

**Example:**
```
User: /trigger
[Bot fetches and sends current rates to the channel]
```

**Note:** On success, the bot sends rates directly to the configured channel without additional confirmation messages. On failure, an error message is sent to the user.

### `/start`
Shows a welcome message and lists available commands.

**Example:**
```
User: /start
Bot: 👋 Привет! Я бот для отслеживания курсов валют.

Доступные команды:
/trigger - Получить актуальный курс валют
/help - Показать справку
```

### `/help`
Displays detailed help information about bot commands.

**Example:**
```
User: /help
Bot: 📖 Справка по командам бота:

/trigger - Принудительно обновить и получить актуальные курсы USD→KZT и RUB→KZT
/start - Показать приветственное сообщение
/help - Показать эту справку

ℹ️ Бот автоматически отправляет курсы валют каждый час.
```

## Configuration

### Environment Variables

Add the following to your `.env` file:

```bash
# Telegram Bot Configuration
BOT_TOKEN=your-telegram-bot-token-here
CHAT_ID=your-telegram-chat-id-here

# Internal API Key (for /trigger command to call POST /api/trigger)
INTERNAL_API_KEY=your-internal-api-key-here
```

### How to Get Bot Token

1. Talk to [@BotFather](https://t.me/botfather) on Telegram
2. Send `/newbot` command
3. Follow the instructions to create a new bot
4. Copy the bot token provided by BotFather

### How to Get Chat ID

**For a private chat:**
1. Send a message to your bot
2. Visit: `https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates`
3. Look for `"chat":{"id":<CHAT_ID>}`

**For a channel:**
1. Add the bot to your channel as an administrator
2. Send a message in the channel
3. Visit: `https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates`
4. Look for the channel's chat ID (will be negative for channels)

## Setting Up the Webhook

### Prerequisites

- Your server must be publicly accessible via HTTPS
- Valid SSL certificate (Telegram requires HTTPS for webhooks)
- Bot token from BotFather

### Configure Webhook URL

Set the webhook using Telegram's API:

```bash
curl -X POST "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://your-domain.com/telegram/webhook"
  }'
```

**Successful response:**
```json
{
  "ok": true,
  "result": true,
  "description": "Webhook was set"
}
```

### Verify Webhook Status

Check if webhook is configured correctly:

```bash
curl "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getWebhookInfo"
```

**Example response:**
```json
{
  "ok": true,
  "result": {
    "url": "https://your-domain.com/telegram/webhook",
    "has_custom_certificate": false,
    "pending_update_count": 0,
    "max_connections": 40
  }
}
```

### Delete Webhook (if needed)

To remove the webhook:

```bash
curl -X POST "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/deleteWebhook"
```

## Local Development

For local development, you can use ngrok to expose your local server:

### Setup ngrok

1. Install ngrok: https://ngrok.com/download
2. Start your Ktor server locally (default port 8080)
3. Create a tunnel:
   ```bash
   ngrok http 8080
   ```
4. Copy the HTTPS URL provided by ngrok (e.g., `https://abc123.ngrok.io`)
5. Set the webhook:
   ```bash
   curl -X POST "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook" \
     -H "Content-Type: application/json" \
     -d '{
       "url": "https://abc123.ngrok.io/telegram/webhook"
     }'
   ```

**Note:** ngrok URLs change when you restart, so you'll need to update the webhook each time.

## Testing

### Manual Testing

1. Send `/trigger` command to your bot
2. Verify the bot responds with currency rates
3. Check server logs for incoming webhook requests

### Automated Tests

Run the webhook integration tests:

```bash
./gradlew test --tests "dev.proflyder.currency.api.TelegramWebhookApiTest"
```

The test suite includes:
- ✅ Successful webhook processing
- ✅ Command handling (/trigger, /start, /help)
- ✅ Empty message handling
- ✅ Error handling
- ✅ No authentication requirement verification

## Security Considerations

### Webhook Endpoint

The `/telegram/webhook` endpoint does **not** require authentication because:
- Telegram sends updates directly to this endpoint
- Telegram validates requests using their own security mechanisms
- The endpoint only accepts POST requests from Telegram

### Internal API Calls

When the bot needs to trigger currency updates (via `/trigger` command), it calls the internal `POST /api/trigger` endpoint which **requires authentication** using the `INTERNAL_API_KEY`.

### Recommended Security Measures

1. **Verify Telegram Requests** (optional but recommended):
   - Implement Telegram's secret token validation
   - Verify the request originates from Telegram servers

2. **Rate Limiting**:
   - Implement rate limiting on the webhook endpoint
   - Prevent abuse from malicious actors

3. **HTTPS Only**:
   - Always use HTTPS in production
   - Telegram requires HTTPS for webhooks

## Troubleshooting

### Bot Not Responding

**Check webhook status:**
```bash
curl "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getWebhookInfo"
```

**Common issues:**
- `pending_update_count` is high → Server not processing updates correctly
- `last_error_date` present → Check the error message
- `url` is empty → Webhook not set

**Solution:**
1. Check server logs for errors
2. Verify the webhook URL is correct and accessible
3. Ensure server has a valid SSL certificate

### Commands Not Working

**Check logs:**
```bash
tail -f logs/application.log | grep -i telegram
```

**Common issues:**
- Command handler not receiving messages
- TriggerApiClient failing to call API
- Internal API key incorrect

**Solution:**
1. Verify `INTERNAL_API_KEY` matches between bot and API
2. Check that POST /api/trigger endpoint is accessible
3. Review TelegramCommandHandler logs

### Updates Not Received

**Symptoms:**
- Bot doesn't respond to messages
- No logs showing incoming webhook requests

**Solution:**
1. Verify webhook URL is publicly accessible:
   ```bash
   curl https://your-domain.com/telegram/webhook
   ```
2. Check firewall settings
3. Ensure port 8080 (or your configured port) is open
4. Verify HTTPS certificate is valid

### Duplicate Messages

**Cause:**
Telegram resends updates if the server doesn't respond with HTTP 200 OK.

**Solution:**
1. Ensure webhook handler always returns 200 OK
2. Handle errors gracefully without throwing exceptions
3. Process updates asynchronously if needed

## Monitoring

### Health Checks

Monitor webhook health:
- Check `getWebhookInfo` regularly
- Monitor `pending_update_count` (should be 0 or low)
- Track response times for webhook endpoint

### Logging

Key events to log:
- ✅ Incoming webhook requests with update ID
- ✅ Command processing (which command, from which chat)
- ✅ API calls from bot to internal endpoints
- ✅ Errors and exceptions

Example log format:
```
[INFO] Received Telegram update 123456789
[INFO] Handling /trigger command for chat 987654321
[INFO] Successfully triggered currency update via /trigger command
```

## Related Documentation

- [Trigger API Documentation](../api/trigger-api.md) - Details on POST /api/trigger endpoint
- [Telegram Bot API](https://core.telegram.org/bots/api) - Official Telegram Bot API docs
- [Architecture Overview](../architecture/overview.md) - System architecture details

## Support

For issues or questions:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review server logs for error details
3. Verify configuration in `.env` file
4. Test webhook connectivity using curl
