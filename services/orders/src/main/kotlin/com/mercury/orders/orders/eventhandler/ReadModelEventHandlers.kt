package com.mercury.orders.orders.eventhandler

import com.mercury.orders.events.*
import com.mercury.orders.orders.readmodel.OrderReadModelProjector
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ReadModelEventHandlers(
    private val projector: OrderReadModelProjector
) {
    @KafkaListener(topics = ["order-events"], groupId = "orders-read-model")
    fun onOrderCreated(event: OrderCreatedEvent) = projector.on(event)

    @KafkaListener(topics = ["payment-events"], groupId = "orders-read-model")
    fun onPaymentAuthorized(event: PaymentAuthorizedEvent) = projector.on(event)

    @KafkaListener(topics = ["payment-events"], groupId = "orders-read-model")
    fun onPaymentDeclined(event: PaymentDeclinedEvent) = projector.on(event)

    @KafkaListener(topics = ["inventory-events"], groupId = "orders-read-model")
    fun onInventoryReserved(event: InventoryReservedEvent) = projector.on(event)

    @KafkaListener(topics = ["inventory-events"], groupId = "orders-read-model")
    fun onInventoryInsufficient(event: InventoryInsufficientEvent) = projector.on(event)

    @KafkaListener(topics = ["order-events"], groupId = "orders-read-model")
    fun onOrderCompleted(event: OrderCompletedEvent) = projector.on(event)

    @KafkaListener(topics = ["order-events"], groupId = "orders-read-model")
    fun onOrderCancelled(event: OrderCancelledEvent) = projector.on(event)
}


