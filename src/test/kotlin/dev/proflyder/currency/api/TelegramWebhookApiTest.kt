package dev.proflyder.currency.api

import dev.proflyder.currency.configureRouting
import dev.proflyder.currency.data.dto.telegram.Chat
import dev.proflyder.currency.data.dto.telegram.TelegramMessage
import dev.proflyder.currency.data.dto.telegram.TelegramUpdate
import dev.proflyder.currency.data.remote.unkey.UnkeyClient
import dev.proflyder.currency.data.remote.unkey.UnkeyVerifyData
import dev.proflyder.currency.data.remote.unkey.UnkeyVerifyResponse
import dev.proflyder.currency.domain.telegram.TelegramCommandHandler
import dev.proflyder.currency.presentation.auth.configureAuthentication
import dev.proflyder.currency.presentation.controller.TelegramWebhookController
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.test.KoinTest

@DisplayName("Telegram Webhook API Integration Tests")
class TelegramWebhookApiTest : KoinTest {

    @Nested
    @DisplayName("POST /telegram/webhook")
    inner class WebhookEndpoint {

        @Test
        fun `должен вернуть 200 при успешной обработке webhook`() = testApplication {
            // Arrange
            val mockCommandHandler = mockk<TelegramCommandHandler>()
            coEvery { mockCommandHandler.handleMessage(any()) } returns Unit

            val mockController = TelegramWebhookController(mockCommandHandler)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                    })
                }
                configureAuthentication(mockUnkeyClient)
                configureRouting()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
            }

            val update = TelegramUpdate(
                updateId = 123456789,
                message = TelegramMessage(
                    messageId = 1,
                    chat = Chat(
                        id = 987654321,
                        type = "private",
                        firstName = "Test",
                        lastName = "User"
                    ),
                    text = "/trigger"
                )
            )

            // Act
            val response = client.post("/telegram/webhook") {
                contentType(ContentType.Application.Json)
                setBody(update)
            }

            // Assert
            response.status shouldBe HttpStatusCode.OK
            coVerify(exactly = 1) { mockCommandHandler.handleMessage(update.message!!) }
        }


        @Test
        fun `должен обработать команду start через webhook`() = testApplication {
            // Arrange
            val mockCommandHandler = mockk<TelegramCommandHandler>()
            coEvery { mockCommandHandler.handleMessage(any()) } returns Unit

            val mockController = TelegramWebhookController(mockCommandHandler)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                    })
                }
                configureAuthentication(mockUnkeyClient)
                configureRouting()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
            }

            val testMessage = TelegramMessage(
                messageId = 2,
                chat = Chat(
                    id = 987654321,
                    type = "private",
                    firstName = "Test",
                    lastName = "User"
                ),
                text = "/start"
            )

            val update = TelegramUpdate(
                updateId = 123456790,
                message = testMessage
            )

            // Act
            val response = client.post("/telegram/webhook") {
                contentType(ContentType.Application.Json)
                setBody(update)
            }

            // Assert
            response.status shouldBe HttpStatusCode.OK
            coVerify(exactly = 1) { mockCommandHandler.handleMessage(testMessage) }
        }

        @Test
        fun `должен обработать команду help через webhook`() = testApplication {
            // Arrange
            val mockCommandHandler = mockk<TelegramCommandHandler>()
            coEvery { mockCommandHandler.handleMessage(any()) } returns Unit

            val mockController = TelegramWebhookController(mockCommandHandler)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                    })
                }
                configureAuthentication(mockUnkeyClient)
                configureRouting()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
            }

            val testMessage = TelegramMessage(
                messageId = 3,
                chat = Chat(
                    id = 987654321,
                    type = "private",
                    firstName = "Test",
                    lastName = "User"
                ),
                text = "/help"
            )

            val update = TelegramUpdate(
                updateId = 123456791,
                message = testMessage
            )

            // Act
            val response = client.post("/telegram/webhook") {
                contentType(ContentType.Application.Json)
                setBody(update)
            }

            // Assert
            response.status shouldBe HttpStatusCode.OK
            coVerify(exactly = 1) { mockCommandHandler.handleMessage(testMessage) }
        }

        @Test
        fun `должен игнорировать обновления без сообщений`() = testApplication {
            // Arrange
            val mockCommandHandler = mockk<TelegramCommandHandler>()
            val mockController = TelegramWebhookController(mockCommandHandler)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                    })
                }
                configureAuthentication(mockUnkeyClient)
                configureRouting()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
            }

            val update = TelegramUpdate(
                updateId = 123456792,
                message = null // No message in update
            )

            // Act
            val response = client.post("/telegram/webhook") {
                contentType(ContentType.Application.Json)
                setBody(update)
            }

            // Assert
            response.status shouldBe HttpStatusCode.OK
            coVerify(exactly = 0) { mockCommandHandler.handleMessage(any()) }
        }

        @Test
        fun `должен обработать сообщения без команд`() = testApplication {
            // Arrange
            val mockCommandHandler = mockk<TelegramCommandHandler>()
            coEvery { mockCommandHandler.handleMessage(any()) } returns Unit

            val mockController = TelegramWebhookController(mockCommandHandler)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                    })
                }
                configureAuthentication(mockUnkeyClient)
                configureRouting()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
            }

            val testMessage = TelegramMessage(
                messageId = 4,
                chat = Chat(
                    id = 987654321,
                    type = "private",
                    firstName = "Test",
                    lastName = "User"
                ),
                text = "Просто обычное сообщение"
            )

            val update = TelegramUpdate(
                updateId = 123456793,
                message = testMessage
            )

            // Act
            val response = client.post("/telegram/webhook") {
                contentType(ContentType.Application.Json)
                setBody(update)
            }

            // Assert
            response.status shouldBe HttpStatusCode.OK
            coVerify(exactly = 1) { mockCommandHandler.handleMessage(testMessage) }
        }

        @Test
        fun `должен вернуть 500 при ошибке обработки`() = testApplication {
            // Arrange
            val mockCommandHandler = mockk<TelegramCommandHandler>()
            coEvery { mockCommandHandler.handleMessage(any()) } throws Exception("Processing error")

            val mockController = TelegramWebhookController(mockCommandHandler)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                    })
                }
                configureAuthentication(mockUnkeyClient)
                configureRouting()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
            }

            val update = TelegramUpdate(
                updateId = 123456794,
                message = TelegramMessage(
                    messageId = 5,
                    chat = Chat(
                        id = 987654321,
                        type = "private",
                        firstName = "Test",
                        lastName = "User"
                    ),
                    text = "/trigger"
                )
            )

            // Act
            val response = client.post("/telegram/webhook") {
                contentType(ContentType.Application.Json)
                setBody(update)
            }

            // Assert
            response.status shouldBe HttpStatusCode.InternalServerError
            coVerify(exactly = 1) { mockCommandHandler.handleMessage(any()) }
        }

        @Test
        fun `не должен требовать аутентификации для webhook endpoint`() = testApplication {
            // Arrange
            val mockCommandHandler = mockk<TelegramCommandHandler>()
            coEvery { mockCommandHandler.handleMessage(any()) } returns Unit

            val mockController = TelegramWebhookController(mockCommandHandler)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                    })
                }
                configureAuthentication(mockUnkeyClient)
                configureRouting()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
            }

            val update = TelegramUpdate(
                updateId = 123456795,
                message = TelegramMessage(
                    messageId = 6,
                    chat = Chat(
                        id = 987654321,
                        type = "private",
                        firstName = "Test",
                        lastName = "User"
                    ),
                    text = "/trigger"
                )
            )

            // Act - отправляем запрос БЕЗ заголовка Authorization
            val response = client.post("/telegram/webhook") {
                contentType(ContentType.Application.Json)
                setBody(update)
            }

            // Assert - должно работать без аутентификации
            response.status shouldBe HttpStatusCode.OK
            coVerify(exactly = 1) { mockCommandHandler.handleMessage(any()) }
        }

        @Test
        fun `должен обработать команду trigger через webhook`() = testApplication {
            // Arrange
            val mockCommandHandler = mockk<TelegramCommandHandler>()
            coEvery { mockCommandHandler.handleMessage(any()) } returns Unit

            val mockController = TelegramWebhookController(mockCommandHandler)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                    })
                }
                configureAuthentication(mockUnkeyClient)
                configureRouting()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
            }

            val testMessage = TelegramMessage(
                messageId = 10,
                chat = Chat(
                    id = 987654321,
                    type = "private",
                    firstName = "Test",
                    lastName = "User"
                ),
                text = "/trigger"
            )

            val update = TelegramUpdate(
                updateId = 123456800,
                message = testMessage
            )

            // Act
            val response = client.post("/telegram/webhook") {
                contentType(ContentType.Application.Json)
                setBody(update)
            }

            // Assert
            response.status shouldBe HttpStatusCode.OK
            coVerify(exactly = 1) { mockCommandHandler.handleMessage(testMessage) }
        }

        @Test
        fun `должен корректно обработать пустой текст сообщения`() = testApplication {
            // Arrange
            val mockCommandHandler = mockk<TelegramCommandHandler>()
            coEvery { mockCommandHandler.handleMessage(any()) } returns Unit

            val mockController = TelegramWebhookController(mockCommandHandler)

            val mockUnkeyClient = mockk<UnkeyClient>()
            coEvery { mockUnkeyClient.verifyKey(any()) } returns Result.success(
                UnkeyVerifyResponse(
                    data = UnkeyVerifyData(valid = true, keyId = "test", name = "test", ownerId = "test")
                )
            )

            application {
                install(Koin) {
                    modules(module {
                        single { mockController }
                    })
                }
                configureAuthentication(mockUnkeyClient)
                configureRouting()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
            }

            val testMessage = TelegramMessage(
                messageId = 7,
                chat = Chat(
                    id = 987654321,
                    type = "private",
                    firstName = "Test",
                    lastName = "User"
                ),
                text = null // Empty text
            )

            val update = TelegramUpdate(
                updateId = 123456796,
                message = testMessage
            )

            // Act
            val response = client.post("/telegram/webhook") {
                contentType(ContentType.Application.Json)
                setBody(update)
            }

            // Assert
            response.status shouldBe HttpStatusCode.OK
            coVerify(exactly = 1) { mockCommandHandler.handleMessage(testMessage) }
        }
    }
}
