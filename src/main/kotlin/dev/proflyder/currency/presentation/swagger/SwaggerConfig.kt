package dev.proflyder.currency.presentation.swagger

import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.data.AuthScheme
import io.github.smiley4.ktorswaggerui.data.AuthType
import io.ktor.server.application.*

fun Application.configureSwagger() {
    install(SwaggerUI) {
        info {
            title = "Currency Exchange API"
            version = "1.0.0"
            description = "API для получения курсов валют USD и RUB в KZT с сайта kurs.kz"
        }

        // Описания тегов
        tags {
            tag("Webhooks") {
                description = "Эндпоинты для интеграции с Telegram Bot API. Принимают webhook события от Telegram и обрабатывают команды бота."
            }
            tag("Currency History") {
                description = "Эндпоинты для получения исторических данных о курсах валют USD и RUB к тенге (KZT). Данные собираются с сайта kurs.kz."
            }
            tag("Triggers") {
                description = "Эндпоинты для ручного запуска процессов обновления и отправки курсов валют. Позволяют принудительно обновить данные независимо от расписания."
            }
        }

        // Схема безопасности для Bearer токена
        security {
            securityScheme("UnkeyAuth") {
                type = AuthType.HTTP
                scheme = AuthScheme.BEARER
                bearerFormat = "API Key"
                description = "API ключ от Unkey для доступа к защищенным эндпоинтам"
            }
        }
    }
}
