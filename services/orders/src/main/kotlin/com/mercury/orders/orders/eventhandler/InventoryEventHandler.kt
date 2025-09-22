package com.mercury.orders.orders.eventhandler

import com.mercury.orders.events.InventoryInsufficientEvent
import com.mercury.orders.events.InventoryReservedEvent
import com.mercury.orders.orders.domain.OrderStatus
import com.mercury.orders.orders.service.OrderService
import com.mercury.orders.tracing.TracingMetrics
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class InventoryEventHandler(
    private val orderService: OrderService,
    private val tracingMetrics: TracingMetrics
) {

    @KafkaListener(topics = ["inventory-events"], groupId = "orders-service")
    fun handleInventoryReserved(event: InventoryReservedEvent) {
        try {
            val order = orderService.getOrder(event.orderId)
            if (order != null && order.status == OrderStatus.INVENTORY_PENDING) {
                // Complete the order
                orderService.updateOrderStatus(event.orderId, OrderStatus.COMPLETED)
            }
            tracingMetrics.incrementEventConsumed(event.eventType)
        } catch (e: Exception) {
            tracingMetrics.incrementEventProcessingError(event.eventType, "PROCESSING_ERROR")
            throw e
        }
    }

    @KafkaListener(topics = ["inventory-events"], groupId = "orders-service")
    fun handleInventoryInsufficient(event: InventoryInsufficientEvent) {
        try {
            val order = orderService.getOrder(event.orderId)
            if (order != null && order.status == OrderStatus.INVENTORY_PENDING) {
                // Cancel the order due to insufficient inventory
                val reason = "Insufficient inventory: ${event.insufficientItems.joinToString { "${it.sku} (requested: ${it.requested}, available: ${it.available})" }}"
                orderService.cancelOrder(event.orderId, reason)
            }
            tracingMetrics.incrementEventConsumed(event.eventType)
        } catch (e: Exception) {
            tracingMetrics.incrementEventProcessingError(event.eventType, "PROCESSING_ERROR")
            throw e
        }
    }
}































