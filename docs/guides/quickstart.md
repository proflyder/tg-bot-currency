# 🚀 Quick Start Guide

## После того как Docker джоба отработала на CI/CD

### Вариант 1: Запустить локально на своем компьютере

```bash
# 1. Создай .env файл
cp .env.example .env
nano .env  # Заполни BOT_TOKEN и CHAT_ID

# 2. Отредактируй docker-compose.prod.yml
# Замени YOUR_GITHUB_USERNAME на свой GitHub username (строка 7)

# 3. Скачай и запусти
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d

# 4. Посмотри логи
docker-compose -f docker-compose.prod.yml logs -f
```

**Готово!** Бот работает 🎉

---

### Вариант 2: Запустить на сервере (VPS)

```bash
# На сервере:

# 1. Установи Docker и Docker Compose (если еще нет)
curl -fsSL https://get.docker.com | sh

# 2. Скачай файлы проекта
git clone https://github.com/твой-username/currency-bot.git
cd currency-bot

# 3. Создай .env файл
cp .env.example .env
nano .env  # Заполни BOT_TOKEN и CHAT_ID

# 4. Отредактируй docker-compose.prod.yml
nano docker-compose.prod.yml
# Замени YOUR_GITHUB_USERNAME на свой username

# 5. Запусти
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d

# Проверь статус
docker-compose -f docker-compose.prod.yml ps
docker-compose -f docker-compose.prod.yml logs -f
```

---

### При обновлении кода (новый коммит)

GitHub Actions автоматически соберет и опубликует новый образ.

**Локально или на сервере:**
```bash
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d
```

Всё! Обновлено за 5 секунд 🚀

---

## Полезные команды

```bash
# Посмотреть статус
docker-compose -f docker-compose.prod.yml ps

# Посмотреть логи
docker-compose -f docker-compose.prod.yml logs -f

# Перезапустить
docker-compose -f docker-compose.prod.yml restart

# Остановить
docker-compose -f docker-compose.prod.yml down

# Полностью удалить и пересоздать
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d
```

---

## Где находится Docker образ?

После успешного запуска CI/CD, образ публикуется в:

```
ghcr.io/твой-github-username/currency-bot:latest
```

Можешь проверить на GitHub:
1. Перейди в свой репозиторий
2. Справа увидишь секцию **Packages**
3. Там будет `currency-bot`

---

## Troubleshooting

### Ошибка: "unauthorized: unauthenticated"

Образ приватный. Сделай его публичным:
1. GitHub → твой репозиторий → Packages
2. Выбери `currency-bot`
3. Package settings → Change visibility → Public

Или залогинься в GHCR:
```bash
echo $GITHUB_TOKEN | docker login ghcr.io -u твой-username --password-stdin
```

### Бот не запускается

Проверь переменные окружения:
```bash
cat .env
docker-compose -f docker-compose.prod.yml config
```

Посмотри логи:
```bash
docker-compose -f docker-compose.prod.yml logs
```

---

## Нужна помощь?

Читай полный README.md или открой issue на GitHub!
