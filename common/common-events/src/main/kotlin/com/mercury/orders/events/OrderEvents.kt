package com.mercury.orders.events

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Base interface for all domain events
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@type")
interface DomainEvent {
    val eventId: UUID
    val eventType: String
    val timestamp: Instant
    val version: String
}

/**
 * Order domain events
 */
@JsonSerialize(using = ToStringSerializer::class)
data class OrderCreatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val orderId: UUID,
    val customerId: String,
    val items: List<OrderItem>,
    val totalAmount: BigDecimal,
    val currency: String = "USD",
    override val eventType: String = "OrderCreated",
    override val timestamp: Instant = Instant.now(),
    override val version: String = "1.0"
) : DomainEvent {
    data class OrderItem(
        val sku: String,
        val quantity: Int,
        val unitPrice: BigDecimal
    )
}

@JsonSerialize(using = ToStringSerializer::class)
data class PaymentAuthorizedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val orderId: UUID,
    val paymentId: String,
    val authorizedAmount: BigDecimal,
    val currency: String = "USD",
    override val eventType: String = "PaymentAuthorized",
    override val timestamp: Instant = Instant.now(),
    override val version: String = "1.0"
) : DomainEvent

@JsonSerialize(using = ToStringSerializer::class)
data class PaymentDeclinedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val orderId: UUID,
    val paymentId: String,
    val reason: String,
    val declinedAmount: BigDecimal,
    val currency: String = "USD",
    override val eventType: String = "PaymentDeclined",
    override val timestamp: Instant = Instant.now(),
    override val version: String = "1.0"
) : DomainEvent

@JsonSerialize(using = ToStringSerializer::class)
data class InventoryReservedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val orderId: UUID,
    val reservedItems: List<ReservedItem>,
    override val eventType: String = "InventoryReserved",
    override val timestamp: Instant = Instant.now(),
    override val version: String = "1.0"
) : DomainEvent {
    data class ReservedItem(
        val sku: String,
        val quantity: Int,
        val reservationId: String
    )
}

@JsonSerialize(using = ToStringSerializer::class)
data class InventoryInsufficientEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val orderId: UUID,
    val insufficientItems: List<InsufficientItem>,
    override val eventType: String = "InventoryInsufficient",
    override val timestamp: Instant = Instant.now(),
    override val version: String = "1.0"
) : DomainEvent {
    data class InsufficientItem(
        val sku: String,
        val requested: Int,
        val available: Int
    )
}

@JsonSerialize(using = ToStringSerializer::class)
data class OrderCompletedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val orderId: UUID,
    override val eventType: String = "OrderCompleted",
    override val timestamp: Instant = Instant.now(),
    override val version: String = "1.0"
) : DomainEvent

@JsonSerialize(using = ToStringSerializer::class)
data class OrderCancelledEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val orderId: UUID,
    val reason: String,
    override val eventType: String = "OrderCancelled",
    override val timestamp: Instant = Instant.now(),
    override val version: String = "1.0"
) : DomainEvent

@JsonSerialize(using = ToStringSerializer::class)
data class PaymentReversedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val orderId: UUID,
    val paymentId: String,
    val reversedAmount: BigDecimal,
    val currency: String = "USD",
    override val eventType: String = "PaymentReversed",
    override val timestamp: Instant = Instant.now(),
    override val version: String = "1.0"
) : DomainEvent

@JsonSerialize(using = ToStringSerializer::class)
data class InventoryReleasedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val orderId: UUID,
    val releasedItems: List<ReleasedItem>,
    override val eventType: String = "InventoryReleased",
    override val timestamp: Instant = Instant.now(),
    override val version: String = "1.0"
) : DomainEvent {
    data class ReleasedItem(
        val sku: String,
        val quantity: Int,
        val reservationId: String
    )
}

/**
 * Event publisher interface for publishing domain events
 */
interface EventPublisher {
    fun publish(event: DomainEvent)
    fun publishWithKey(event: DomainEvent, key: String)
}

/**
 * Event handler interface for consuming domain events
 */
interface EventHandler<T : DomainEvent> {
    fun handle(event: T)
    fun getEventType(): String
}

/**
 * Outbox event entity for reliable event publishing
 */
data class OutboxEvent(
    val id: UUID = UUID.randomUUID(),
    val eventType: String,
    val aggregateId: String,
    val eventData: String, // JSON serialized event
    val status: OutboxEventStatus = OutboxEventStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    val publishedAt: Instant? = null,
    val retryCount: Int = 0,
    val lastError: String? = null
)

enum class OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}

