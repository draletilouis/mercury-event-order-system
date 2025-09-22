package com.mercury.orders.orders.readmodel

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "order_read_model")
data class OrderReadModel(
    @Id
    val orderId: UUID,

    @Column(name = "customer_id", nullable = false)
    val customerId: String,

    @Column(name = "status", nullable = false, length = 32)
    val status: String,

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    val totalAmount: BigDecimal,

    @Column(name = "currency", nullable = false, length = 3)
    val currency: String,

    @Column(name = "last_event_type", nullable = false, length = 64)
    val lastEventType: String,

    @Column(name = "last_event_at", nullable = false)
    val lastEventAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)


