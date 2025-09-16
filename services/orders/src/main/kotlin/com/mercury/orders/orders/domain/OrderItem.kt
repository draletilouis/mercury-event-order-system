package com.mercury.orders.orders.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "order_items")
data class OrderItem(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "order_id", nullable = false)
    val orderId: UUID,
    
    @Column(name = "sku", nullable = false)
    val sku: String,
    
    @Column(name = "quantity", nullable = false)
    val quantity: Int,
    
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    val unitPrice: BigDecimal,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

