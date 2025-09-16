package com.mercury.orders.tracing

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TracingConfiguration {

    @Bean
    fun tracingMetrics(meterRegistry: MeterRegistry): TracingMetrics {
        return TracingMetrics(meterRegistry)
    }
}

class TracingMetrics(private val meterRegistry: MeterRegistry) {
    
    fun incrementEventPublished(eventType: String) {
        meterRegistry.counter("events.published", "type", eventType).increment()
    }
    
    fun incrementEventConsumed(eventType: String) {
        meterRegistry.counter("events.consumed", "type", eventType).increment()
    }
    
    fun incrementEventProcessingError(eventType: String, errorType: String) {
        meterRegistry.counter("events.processing.error", "type", eventType, "error", errorType).increment()
    }
    
    fun recordEventProcessingTime(eventType: String, durationMs: Long) {
        meterRegistry.timer("events.processing.time", "type", eventType).record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS)
    }
    
    fun incrementOrderCreated() {
        meterRegistry.counter("orders.created").increment()
    }
    
    fun incrementOrderCompleted() {
        meterRegistry.counter("orders.completed").increment()
    }
    
    fun incrementOrderCancelled() {
        meterRegistry.counter("orders.cancelled").increment()
    }
    
    fun incrementPaymentAuthorized() {
        meterRegistry.counter("payments.authorized").increment()
    }
    
    fun incrementPaymentDeclined() {
        meterRegistry.counter("payments.declined").increment()
    }
    
    fun incrementInventoryReserved() {
        meterRegistry.counter("inventory.reserved").increment()
    }
    
    fun incrementInventoryInsufficient() {
        meterRegistry.counter("inventory.insufficient").increment()
    }
}
