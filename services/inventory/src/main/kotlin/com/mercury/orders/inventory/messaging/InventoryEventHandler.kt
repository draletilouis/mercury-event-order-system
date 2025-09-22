package com.mercury.orders.inventory.messaging

import com.mercury.orders.events.OrderCancelledEvent
import com.mercury.orders.events.OrderCreatedEvent
import com.mercury.orders.events.PaymentAuthorizedEvent
import com.mercury.orders.events.PaymentDeclinedEvent
import com.mercury.orders.inventory.service.InventoryService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.*

@Component
class InventoryEventHandler(
    private val inventoryService: InventoryService,
    private val messageIdempotency: MessageIdempotency
) {
    @KafkaListener(topics = ["order-events"], groupId = "inventory-service")
    fun onOrderEvents(record: ConsumerRecord<String, Any>) {
        if (!messageIdempotency.shouldProcess("order-events", record)) return
        when (val event = record.value()) {
            is OrderCreatedEvent -> handleOrderCreated(event)
            is OrderCancelledEvent -> handleOrderCancelled(event)
        }
    }

    @KafkaListener(topics = ["payment-events"], groupId = "inventory-service")
    fun onPaymentEvents(record: ConsumerRecord<String, Any>) {
        if (!messageIdempotency.shouldProcess("payment-events", record)) return
        when (val event = record.value()) {
            is PaymentAuthorizedEvent -> handlePaymentAuthorized(event)
            is PaymentDeclinedEvent -> handlePaymentDeclined(event)
        }
    }

    private fun handleOrderCreated(event: OrderCreatedEvent) {
        val orderId = event.orderId
        val items = event.items.map { com.mercury.orders.inventory.service.ReservationRequestItem(it.sku, it.quantity) }
        inventoryService.reserveForOrder(orderId, items)
    }

    private fun handleOrderCancelled(event: OrderCancelledEvent) {
        inventoryService.releaseForOrder(event.orderId)
    }

    private fun handlePaymentAuthorized(event: PaymentAuthorizedEvent) {
        inventoryService.confirmForOrder(event.orderId)
    }

    private fun handlePaymentDeclined(event: PaymentDeclinedEvent) {
        inventoryService.releaseForOrder(event.orderId)
    }
}


