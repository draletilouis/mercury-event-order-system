package com.mercury.orders.inventory.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "inventory_items")
data class InventoryItem(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "sku", nullable = false, unique = true)
    val sku: String,
    
    @Column(name = "name", nullable = false, length = 500)
    val name: String,
    
    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,
    
    @Column(name = "available_quantity", nullable = false)
    val availableQuantity: Int = 0,
    
    @Column(name = "reserved_quantity", nullable = false)
    val reservedQuantity: Int = 0,
    
    @Column(name = "unit_cost", precision = 19, scale = 2)
    val unitCost: BigDecimal? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
    
    @Version
    @Column(name = "version", nullable = false)
    val version: Int = 0
) {
    val totalQuantity: Int get() = availableQuantity + reservedQuantity
    
    fun canReserve(quantity: Int): Boolean {
        return availableQuantity >= quantity && quantity > 0
    }
    
    fun reserve(quantity: Int): InventoryItem {
        if (!canReserve(quantity)) {
            throw IllegalArgumentException("Cannot reserve $quantity items. Available: $availableQuantity")
        }
        return copy(
            availableQuantity = availableQuantity - quantity,
            reservedQuantity = reservedQuantity + quantity,
            updatedAt = Instant.now()
        )
    }
    
    fun release(quantity: Int): InventoryItem {
        if (reservedQuantity < quantity) {
            throw IllegalArgumentException("Cannot release $quantity items. Reserved: $reservedQuantity")
        }
        return copy(
            availableQuantity = availableQuantity + quantity,
            reservedQuantity = reservedQuantity - quantity,
            updatedAt = Instant.now()
        )
    }
    
    fun confirmReservation(quantity: Int): InventoryItem {
        if (reservedQuantity < quantity) {
            throw IllegalArgumentException("Cannot confirm reservation of $quantity items. Reserved: $reservedQuantity")
        }
        return copy(
            reservedQuantity = reservedQuantity - quantity,
            updatedAt = Instant.now()
        )
    }
}

