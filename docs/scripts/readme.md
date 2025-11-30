# Scripts

Полезные скрипты для управления проектом.

## logs.sh - Управление логами

Утилита для работы с логами Currency Bot в Docker.

### Быстрый старт

```bash
./scripts/logs.sh tail    # Следить за логами
./scripts/logs.sh help    # Показать все команды
```

### Команды

| Команда | Описание | Пример |
|---------|----------|--------|
| `tail` | Следить за логами в реальном времени | `./logs.sh tail` |
| `show` | Показать последние 100 строк | `./logs.sh show` |
| `save [file]` | Сохранить логи в файл | `./logs.sh save my-logs.txt` |
| `size` | Показать размер логов | `./logs.sh size` |
| `clean [days]` | Удалить логи старше N дней | `./logs.sh clean 30` |
| `backup [file]` | Создать backup архив | `./logs.sh backup` |
| `restore [file]` | Восстановить из backup | `./logs.sh restore logs.tar.gz` |
| `list` | Список всех файлов логов | `./logs.sh list` |
| `volume` | Информация о Docker volume | `./logs.sh volume` |

### Примеры использования

**Мониторинг логов:**
```bash
# Следить в реальном времени
./scripts/logs.sh tail

# Показать последние строки
./scripts/logs.sh show
```

**Backup перед обновлением:**
```bash
# Создать backup
./scripts/logs.sh backup

# Обновить приложение
docker-compose down
docker-compose pull
docker-compose up -d

# Если что-то пошло не так - восстановить
./scripts/logs.sh restore logs-backup-20250130-120000.tar.gz
```

**Очистка места:**
```bash
# Посмотреть сколько места занимают логи
./scripts/logs.sh size

# Удалить старые логи (старше 7 дней)
./scripts/logs.sh clean 7
```

**Отладка проблем:**
```bash
# Сохранить логи в файл для анализа
./scripts/logs.sh save error-logs.txt

# Отправить логи разработчику
./scripts/logs.sh save logs-for-support.txt
# Затем отправь файл logs-for-support.txt
```

### Troubleshooting

**Ошибка: Container is not running**
```bash
# Проверь что контейнер запущен
docker ps | grep currency-bot

# Если не запущен - запусти
docker-compose up -d
```

**Ошибка: Permission denied**
```bash
# Дай скрипту права на выполнение
chmod +x ./scripts/logs.sh
```

**Ошибка: Volume not found**
```bash
# Убедись что volume создан
docker volume ls | grep currency-bot-logs

# Если нет - создай через docker-compose
docker-compose up -d
```
