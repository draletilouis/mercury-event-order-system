package com.mercury.orders.events

import java.util.UUID

/**
 * Basic domain events for the order system
 */
data class OrderCreatedEvent(
    val orderId: UUID,
    val customerId: String,
    val totalAmount: Long
)

data class PaymentAuthorizedEvent(
    val orderId: UUID,
    val paymentId: String,
    val authorizedAmount: Long
)

data class InventoryReservedEvent(
    val orderId: UUID,
    val reservedItems: List<ReservedItem>
) {
    data class ReservedItem(
        val sku: String,
        val quantity: Int
    )
}

data class OrderCompletedEvent(
    val orderId: UUID
)

data class OrderCompensationEvent(
    val orderId: UUID,
    val reason: String
)



