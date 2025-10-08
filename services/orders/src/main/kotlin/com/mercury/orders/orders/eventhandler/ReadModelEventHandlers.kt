package com.mercury.orders.orders.eventhandler

import com.mercury.orders.events.*
import com.mercury.orders.orders.readmodel.OrderReadModelProjector
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ReadModelEventHandlers(
    private val projector: OrderReadModelProjector
) {
    @KafkaListener(
        topics = ["order-events"],
        groupId = "orders-read-model",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleOrderEvent(event: Any) {
        when (event) {
            is OrderCreatedEvent -> projector.on(event)
            is OrderCompletedEvent -> projector.on(event)
            is OrderCancelledEvent -> projector.on(event)
        }
    }

    @KafkaListener(
        topics = ["payment-events"],
        groupId = "orders-read-model",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handlePaymentEvent(event: Any) {
        when (event) {
            is PaymentAuthorizedEvent -> projector.on(event)
            is PaymentDeclinedEvent -> projector.on(event)
        }
    }

    @KafkaListener(
        topics = ["inventory-events"],
        groupId = "orders-read-model",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleInventoryEvent(event: Any) {
        when (event) {
            is InventoryReservedEvent -> projector.on(event)
            is InventoryInsufficientEvent -> projector.on(event)
        }
    }
}


