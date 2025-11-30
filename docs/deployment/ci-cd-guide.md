# CI/CD Guide

## Структура Pipeline

```
┌─────────────────────────────────────────────────────┐
│                   Push to GitHub                    │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────┐
│  JOB 1: Build                                        │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  1. Checkout кода                                    │
│  2. Setup Java 21 + Gradle                           │
│  3. Gradle: buildFatJar                              │
│  4. Upload JAR → Artifact Storage                    │
└──────────────────────┬───────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────┐
│  JOB 2: Docker Build                                 │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  1. Download JAR ← Artifact Storage                  │
│  2. Build Docker (Dockerfile)                        │
│  3. Publish to GHCR                                  │
└──────────────────────┬───────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────┐
│  JOB 3: Deploy (только main/master)                 │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  1. Download Docker image ← Artifact Storage         │
│  2. 🚧 STUB - реализовать деплой                     │
└──────────────────────────────────────────────────────┘
```

## Файлы

### Dockerfile (универсальный)
```dockerfile
# Multi-stage: собирает проект внутри контейнера
FROM gradle:8.5-jdk21 AS build
# ... сборка ...
FROM eclipse-temurin:21-jre-alpine
# ... runtime ...
```

**Использование локально:**
```bash
docker build -t currency-bot .
docker run -e BOT_TOKEN=xxx -e CHAT_ID=yyy currency-bot
```

**Использование в CI:**
```bash
# JAR уже собран в предыдущей джобе
docker build -t currency-bot .
docker push ghcr.io/username/currency-bot:latest
```

## Артефакты между джобами

### 1. JAR Artifact (build → docker)
```yaml
# Upload в build job
- uses: actions/upload-artifact@v4
  with:
    name: application-jar
    path: build/libs/*-all.jar

# Download в docker job
- uses: actions/download-artifact@v4
  with:
    name: application-jar
    path: build/libs/
```

### 2. Docker Image Artifact (docker → deploy)
```yaml
# Upload в docker job
- run: docker save ... | gzip > image.tar.gz
- uses: actions/upload-artifact@v4
  with:
    name: docker-image
    path: currency-bot.tar.gz

# Download в deploy job
- uses: actions/download-artifact@v4
  with:
    name: docker-image
- run: docker load < currency-bot.tar.gz
```

## Как реализовать Deploy

### Вариант 1: Docker Hub / GHCR

Замените deploy job stub на:

```yaml
deploy:
  name: Deploy to Registry
  runs-on: ubuntu-latest
  needs: docker
  if: github.ref == 'refs/heads/main'

  steps:
    - uses: docker/login-action@v3
      with:
        registry: ghcr.io
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}

    - uses: actions/download-artifact@v4
      with:
        name: docker-image

    - run: docker load < currency-bot.tar.gz

    - run: |
        docker tag currency-bot:${{ github.sha }} \
          ghcr.io/${{ github.repository }}:latest
        docker push ghcr.io/${{ github.repository }}:latest
```

**Настройки в GitHub:**
- Settings → Packages → Enable package creation

### Вариант 2: SSH Deploy на сервер

```yaml
deploy:
  name: Deploy to Server
  runs-on: ubuntu-latest
  needs: docker
  if: github.ref == 'refs/heads/main'

  steps:
    - uses: appleboy/scp-action@v0.1.4
      with:
        host: ${{ secrets.SERVER_HOST }}
        username: ${{ secrets.SERVER_USER }}
        key: ${{ secrets.SSH_PRIVATE_KEY }}
        source: "docker-compose.yml,.env.example"
        target: "/app"

    - uses: appleboy/ssh-action@v1.0.0
      with:
        host: ${{ secrets.SERVER_HOST }}
        username: ${{ secrets.SERVER_USER }}
        key: ${{ secrets.SSH_PRIVATE_KEY }}
        script: |
          cd /app
          docker-compose pull
          docker-compose up -d
```

**Настройки в GitHub:**
- Settings → Secrets → Actions:
  - `SERVER_HOST` - IP или домен сервера
  - `SERVER_USER` - SSH username
  - `SSH_PRIVATE_KEY` - приватный SSH ключ

### Вариант 3: Cloud Provider (AWS ECS, GCP Cloud Run)

```yaml
- uses: google-github-actions/setup-gcloud@v1
  with:
    service_account_key: ${{ secrets.GCP_SA_KEY }}

- run: |
    gcloud run deploy currency-bot \
      --image gcr.io/$PROJECT_ID/currency-bot:${{ github.sha }} \
      --region us-central1 \
      --platform managed
```

## Триггеры Workflow

Текущие триггеры:
```yaml
on:
  push:
    branches: [ main, master ]  # Автоматически при пуше
  pull_request:
    branches: [ main, master ]  # При создании PR
  workflow_dispatch:            # Ручной запуск через UI
```

### Ручной запуск

1. Перейдите: Actions → CI/CD Pipeline
2. Нажмите "Run workflow"
3. Выберите ветку и запустите

## Кеширование

### Gradle Cache
```yaml
- uses: gradle/actions/setup-gradle@v3
  with:
    cache-read-only: false  # Пишем в кеш
```

### Docker BuildKit Cache
```yaml
cache-from: type=gha        # Читаем из GitHub cache
cache-to: type=gha,mode=max # Пишем в GitHub cache
```

Экономия времени: ~2-5 минут на каждый build!

## Мониторинг

- GitHub Actions tab показывает статус каждой джобы
- Артефакты доступны 7 дней
- Логи каждого шага можно развернуть

## Troubleshooting

### JAR не найден
```
Error: No files were found with the provided path: build/libs/*-all.jar
```

**Решение:** Проверьте что `./gradlew buildFatJar` создает JAR файл.

### Docker image не загружается
```
Error: manifest unknown
```

**Решение:** Проверьте что docker save/load использует правильные теги.

### Deploy job пропускается
```
Skipping deploy job
```

**Решение:** Проверьте условие `if: github.ref == 'refs/heads/main'`
