package dev.proflyder.currency.presentation.swagger

import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.data.AuthScheme
import io.github.smiley4.ktorswaggerui.data.AuthType
import io.ktor.server.application.*

fun Application.configureSwagger() {
    install(SwaggerUI) {
        info {
            title = "Currency Exchange API"
            version = "2.0.0"
            description = """
                API для работы с курсами валют USD и RUB к тенге (KZT).

                Основные возможности:
                - Получение исторических данных о курсах валют с kurs.kz
                - Получение актуального курса валют
                - Принудительное обновление курсов
                - Интеграция с Telegram Bot API
                - Мониторинг через Prometheus метрики

                Защищенные эндпоинты требуют Bearer токен аутентификации (Unkey API Key).
            """.trimIndent()
        }

        // Описания тегов
        tags {
            tag("Currency History") {
                description = """
                    Эндпоинты для работы с историческими данными о курсах валют USD и RUB к тенге (KZT).

                    Данные собираются с сайта kurs.kz и сохраняются в H2 базу данных.
                    Автоматическое обновление происходит по расписанию через Quartz Scheduler.
                """.trimIndent()
            }
            tag("Triggers") {
                description = """
                    Эндпоинты для ручного запуска процессов обновления и отправки курсов валют.

                    Позволяют принудительно обновить данные независимо от расписания и порогов изменения.
                    Полезно для тестирования и срочных обновлений.
                """.trimIndent()
            }
//            tag("Webhooks") {
//                description = """
//                    Эндпоинты для интеграции с Telegram Bot API.
//
//                    Принимают webhook события от Telegram и обрабатывают команды бота.
//                    Необходимо настроить webhook через setWebhook метод Telegram Bot API.
//                """.trimIndent()
//            }
//            tag("Monitoring") {
//                description = """
//                    Эндпоинты для мониторинга и наблюдаемости приложения.
//
//                    Предоставляют метрики в формате Prometheus для интеграции с системами мониторинга.
//                """.trimIndent()
//            }
        }

        // Схема безопасности для Bearer токена
        security {
            securityScheme("UnkeyAuth") {
                type = AuthType.HTTP
                scheme = AuthScheme.BEARER
                bearerFormat = "API Key"
                description = """
                    API ключ от Unkey для доступа к защищенным эндпоинтам.

                    Передается в заголовке Authorization:
                    Authorization: Bearer YOUR_API_KEY
                """.trimIndent()
            }
        }
    }
}
