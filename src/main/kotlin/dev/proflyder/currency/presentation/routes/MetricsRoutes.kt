package dev.proflyder.currency.presentation.routes

import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Route.metricsRoutes(prometheusRegistry: PrometheusMeterRegistry) {
    get("/metrics", {
        hidden = true
        tags = listOf("Monitoring")
        summary = "Метрики Prometheus"
        description = """
            Эндпоинт для экспорта метрик в формате Prometheus.

            Собираемые метрики:
            - HTTP запросы (количество, длительность)
            - JVM метрики (память, GC, потоки)
            - Системные метрики (CPU, память)
            - Кастомные метрики приложения

            Этот эндпоинт используется для мониторинга и алертинга через Prometheus + Grafana.
            Формат ответа: text/plain в формате Prometheus exposition format.
        """.trimIndent()
        response {
            HttpStatusCode.OK to {
                description = "Метрики в формате Prometheus (text/plain; version=0.0.4)"
                body<String> {
                    example("Пример метрик") {
                        value = """
                            # HELP http_server_requests_seconds Duration of HTTP server request handling
                            # TYPE http_server_requests_seconds summary
                            http_server_requests_seconds_count{method="GET",status="200",uri="/api/history"} 42.0
                            http_server_requests_seconds_sum{method="GET",status="200",uri="/api/history"} 1.234

                            # HELP jvm_memory_used_bytes The amount of used memory
                            # TYPE jvm_memory_used_bytes gauge
                            jvm_memory_used_bytes{area="heap",id="G1 Old Gen"} 1.234567E8
                            jvm_memory_used_bytes{area="heap",id="G1 Survivor Space"} 1234567.0
                            jvm_memory_used_bytes{area="heap",id="G1 Eden Space"} 1.234567E7
                        """.trimIndent()
                    }
                }
            }
        }
    }) {
        call.respondText(
            text = prometheusRegistry.scrape(),
            contentType = ContentType.parse("text/plain; version=0.0.4")
        )
    }
}
