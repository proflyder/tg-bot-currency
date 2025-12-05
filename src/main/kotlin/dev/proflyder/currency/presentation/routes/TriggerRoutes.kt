package dev.proflyder.currency.presentation.routes

import dev.proflyder.currency.data.dto.TriggerResponseDto
import dev.proflyder.currency.presentation.controller.TriggerController
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.*
import io.ktor.server.routing.*

fun Route.triggerRoutes(triggerController: TriggerController) {
    post("/api/trigger", {
        tags = listOf("Triggers")
        description = "Запускает немедленное обновление курсов: парсинг с kurs.kz, сохранение в БД и отправка в Telegram канал (независимо от порогов изменения)"
        securitySchemeNames = listOf("UnkeyAuth")
        response {
            HttpStatusCode.OK to {
                description = "Курсы успешно обновлены"
                body<TriggerResponseDto> {
                    example("Успешное обновление") {
                        value = TriggerResponseDto(
                            success = true,
                            message = "Currency rates updated and sent to Telegram successfully"
                        )
                    }
                }
            }
            HttpStatusCode.Unauthorized to {
                description = "Отсутствует или невалидный API ключ"
            }
            HttpStatusCode.InternalServerError to {
                description = "Ошибка при обновлении курсов"
                body<TriggerResponseDto> {
                    example("Ошибка при парсинге") {
                        value = TriggerResponseDto(
                            success = false,
                            message = "Failed to update currency rates: Connection timeout"
                        )
                    }
                }
            }
        }
    }) {
        triggerController.triggerCurrencyUpdate(call)
    }
}
