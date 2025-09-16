package com.mercury.orders.orders.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "orders")
data class Order(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "customer_id", nullable = false)
    val customerId: String,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: OrderStatus = OrderStatus.PENDING,
    
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    val totalAmount: BigDecimal,
    
    @Column(name = "currency", nullable = false, length = 3)
    val currency: String = "USD",
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
    
    @Version
    @Column(name = "version", nullable = false)
    val version: Int = 0,
    
    @OneToMany(mappedBy = "orderId", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val items: List<OrderItem> = emptyList()
) {
    fun canTransitionTo(newStatus: OrderStatus): Boolean {
        return when (status) {
            OrderStatus.PENDING -> newStatus in listOf(OrderStatus.PAYMENT_PENDING, OrderStatus.CANCELLED)
            OrderStatus.PAYMENT_PENDING -> newStatus in listOf(OrderStatus.INVENTORY_PENDING, OrderStatus.CANCELLED)
            OrderStatus.INVENTORY_PENDING -> newStatus in listOf(OrderStatus.COMPLETED, OrderStatus.CANCELLED)
            OrderStatus.COMPLETED -> false
            OrderStatus.CANCELLED -> false
        }
    }
    
    fun transitionTo(newStatus: OrderStatus): Order {
        if (!canTransitionTo(newStatus)) {
            throw IllegalStateException("Cannot transition from $status to $newStatus")
        }
        return copy(status = newStatus, updatedAt = Instant.now())
    }
}

enum class OrderStatus {
    PENDING,
    PAYMENT_PENDING,
    INVENTORY_PENDING,
    COMPLETED,
    CANCELLED
}
