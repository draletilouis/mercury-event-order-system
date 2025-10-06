package com.mercury.orders.orders.eventhandler

import com.mercury.orders.events.EventHandler
import com.mercury.orders.events.PaymentAuthorizedEvent
import com.mercury.orders.events.PaymentDeclinedEvent
import com.mercury.orders.orders.domain.OrderStatus
import com.mercury.orders.orders.service.OrderService
import com.mercury.orders.tracing.TracingMetrics
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class PaymentEventHandler(
    private val orderService: OrderService,
    private val tracingMetrics: TracingMetrics
) {

    @KafkaListener(topics = ["payment-events"], groupId = "orders-service")
    fun handlePaymentAuthorized(event: PaymentAuthorizedEvent) {
        try {
            val order = orderService.getOrder(event.orderId)
            if (order != null && order.status == OrderStatus.PAYMENT_PENDING) {
                // Transition to inventory pending status
                orderService.updateOrderStatus(event.orderId, OrderStatus.INVENTORY_PENDING)
            }
            tracingMetrics.incrementEventConsumed(event.eventType)
        } catch (e: Exception) {
            tracingMetrics.incrementEventProcessingError(event.eventType, "PROCESSING_ERROR")
            throw e
        }
    }

    @KafkaListener(topics = ["payment-events"], groupId = "orders-service")
    fun handlePaymentDeclined(event: PaymentDeclinedEvent) {
        try {
            val order = orderService.getOrder(event.orderId)
            if (order != null && order.status == OrderStatus.PAYMENT_PENDING) {
                // Cancel the order due to payment failure
                orderService.cancelOrder(event.orderId, "Payment declined: ${event.reason}")
            }
            tracingMetrics.incrementEventConsumed(event.eventType)
        } catch (e: Exception) {
            tracingMetrics.incrementEventProcessingError(event.eventType, "PROCESSING_ERROR")
            throw e
        }
    }
}








































