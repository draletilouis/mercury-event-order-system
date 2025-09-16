package com.mercury.orders.inventory.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "inventory_reservations")
data class InventoryReservation(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "order_id", nullable = false)
    val orderId: UUID,
    
    @Column(name = "sku", nullable = false)
    val sku: String,
    
    @Column(name = "quantity", nullable = false)
    val quantity: Int,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: ReservationStatus = ReservationStatus.ACTIVE,
    
    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "released_at")
    val releasedAt: Instant? = null
) {
    fun isExpired(): Boolean {
        return Instant.now().isAfter(expiresAt)
    }
    
    fun canTransitionTo(newStatus: ReservationStatus): Boolean {
        return when (status) {
            ReservationStatus.ACTIVE -> newStatus in listOf(ReservationStatus.RELEASED, ReservationStatus.CONFIRMED)
            ReservationStatus.RELEASED -> false
            ReservationStatus.CONFIRMED -> false
        }
    }
    
    fun transitionTo(newStatus: ReservationStatus): InventoryReservation {
        if (!canTransitionTo(newStatus)) {
            throw IllegalStateException("Cannot transition from $status to $newStatus")
        }
        return when (newStatus) {
            ReservationStatus.RELEASED -> copy(status = newStatus, releasedAt = Instant.now())
            ReservationStatus.CONFIRMED -> copy(status = newStatus)
            else -> copy(status = newStatus)
        }
    }
}

enum class ReservationStatus {
    ACTIVE,
    RELEASED,
    CONFIRMED
}

