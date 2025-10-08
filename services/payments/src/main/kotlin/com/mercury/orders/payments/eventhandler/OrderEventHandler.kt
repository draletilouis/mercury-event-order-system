package com.mercury.orders.payments.eventhandler

import com.mercury.orders.events.OrderCreatedEvent
import com.mercury.orders.payments.service.PaymentService
import com.mercury.orders.tracing.TracingMetrics
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class OrderEventHandler(
    private val paymentService: PaymentService,
    private val tracingMetrics: TracingMetrics
) {

    @KafkaListener(topics = ["order-events"], groupId = "payments-service")
    fun handleOrderCreated(event: OrderCreatedEvent) {
        try {
            // Authorize payment for the order
            paymentService.authorizePayment(
                orderId = event.orderId,
                amount = event.totalAmount,
                currency = event.currency
            )
            tracingMetrics.incrementEventConsumed(event.eventType)
        } catch (e: Exception) {
            tracingMetrics.incrementEventProcessingError(event.eventType, "PROCESSING_ERROR")
            throw e
        }
    }
}















































