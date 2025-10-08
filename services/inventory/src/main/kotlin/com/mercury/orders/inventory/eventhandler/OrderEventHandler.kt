package com.mercury.orders.inventory.eventhandler

import com.mercury.orders.events.OrderCancelledEvent
import com.mercury.orders.events.OrderCreatedEvent
import com.mercury.orders.events.PaymentAuthorizedEvent
import com.mercury.orders.inventory.service.InventoryService
import com.mercury.orders.inventory.service.ReservationRequestItem
import com.mercury.orders.tracing.TracingMetrics
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class OrderEventHandler(
    private val inventoryService: InventoryService,
    private val tracingMetrics: TracingMetrics
) {

    @KafkaListener(
        topics = ["payment-events"],
        groupId = "inventory-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handlePaymentEvent(event: Any) {
        when (event) {
            is PaymentAuthorizedEvent -> handlePaymentAuthorized(event)
        }
    }

    @KafkaListener(
        topics = ["order-events"],
        groupId = "inventory-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleOrderEvent(event: Any) {
        when (event) {
            is OrderCreatedEvent -> handleOrderCreated(event)
            is OrderCancelledEvent -> handleOrderCancelled(event)
        }
    }

    private fun handlePaymentAuthorized(event: PaymentAuthorizedEvent) {
        try {
            // When payment is authorized, we need to handle inventory reservation
            // In a proper implementation, we would get order details from order service
            // For now, this is a placeholder for the event consumption pattern
            tracingMetrics.incrementEventConsumed(event.eventType)
        } catch (e: Exception) {
            tracingMetrics.incrementEventProcessingError(event.eventType, "PROCESSING_ERROR")
            throw e
        }
    }

    private fun handleOrderCreated(event: OrderCreatedEvent) {
        try {
            // Reserve inventory for the new order
            val reservationItems = event.items.map {
                ReservationRequestItem(sku = it.sku, quantity = it.quantity)
            }
            inventoryService.reserveForOrder(event.orderId, reservationItems)
            tracingMetrics.incrementEventConsumed(event.eventType)
        } catch (e: Exception) {
            tracingMetrics.incrementEventProcessingError(event.eventType, "PROCESSING_ERROR")
            throw e
        }
    }

    private fun handleOrderCancelled(event: OrderCancelledEvent) {
        try {
            // Release inventory when order is cancelled
            inventoryService.releaseForOrder(event.orderId)
            tracingMetrics.incrementEventConsumed(event.eventType)
        } catch (e: Exception) {
            tracingMetrics.incrementEventProcessingError(event.eventType, "PROCESSING_ERROR")
            throw e
        }
    }
}


