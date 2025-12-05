package dev.proflyder.currency.presentation.metrics

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

/**
 * Конфигурация метрик Prometheus
 * Экспортирует только CPU и Memory метрики
 * Возвращает prometheusRegistry для использования в роутинге
 */
fun Application.configureMetrics(): PrometheusMeterRegistry {
    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) {
        registry = prometheusRegistry

        // Только CPU и Memory метрики
        meterBinders = listOf(
            ProcessorMetrics(),      // system_cpu_usage, process_cpu_usage
            JvmMemoryMetrics()       // jvm_memory_used_bytes, jvm_memory_max_bytes
        )
    }

    return prometheusRegistry
}
