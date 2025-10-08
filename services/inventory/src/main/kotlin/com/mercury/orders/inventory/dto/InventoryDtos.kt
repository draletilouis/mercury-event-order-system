package com.mercury.orders.inventory.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Request DTOs
 */
data class CreateInventoryItemRequest(
    @field:NotBlank(message = "SKU is required")
    @field:Size(max = 255, message = "SKU must not exceed 255 characters")
    val sku: String,
    
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 500, message = "Name must not exceed 500 characters")
    val name: String,
    
    val description: String? = null,
    
    @field:NotNull(message = "Initial quantity is required")
    @field:Min(value = 0, message = "Initial quantity must be non-negative")
    val initialQuantity: Int,
    
    val unitCost: BigDecimal? = null
)

data class UpdateInventoryItemRequest(
    val name: String? = null,
    val description: String? = null,
    val unitCost: BigDecimal? = null
)

data class AdjustInventoryQuantityRequest(
    @field:NotNull(message = "Quantity adjustment is required")
    val quantityChange: Int,
    
    val reason: String? = null
)

data class ReserveInventoryRequest(
    @field:NotNull(message = "Order ID is required")
    val orderId: UUID,
    
    @field:NotNull(message = "Items are required")
    @field:Size(min = 1, message = "At least one item is required")
    val items: List<ReservationItem>
) {
    data class ReservationItem(
        @field:NotBlank(message = "SKU is required")
        val sku: String,
        
        @field:NotNull(message = "Quantity is required")
        @field:Min(value = 1, message = "Quantity must be at least 1")
        val quantity: Int
    )
}

data class ReleaseInventoryRequest(
    @field:NotNull(message = "Order ID is required")
    val orderId: UUID
)

/**
 * Response DTOs
 */
data class InventoryItemResponse(
    val id: UUID,
    val sku: String,
    val name: String,
    val description: String?,
    val availableQuantity: Int,
    val reservedQuantity: Int,
    val totalQuantity: Int,
    val unitCost: BigDecimal?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Int
)

data class ReservationResponse(
    val orderId: UUID,
    val reservations: List<ReservationDetail>,
    val status: ReservationStatus,
    val message: String? = null
) {
    data class ReservationDetail(
        val reservationId: UUID,
        val sku: String,
        val quantity: Int,
        val expiresAt: Instant
    )
    
    enum class ReservationStatus {
        SUCCESS,
        INSUFFICIENT_INVENTORY,
        PARTIAL_SUCCESS,
        FAILED
    }
}

data class ReleaseResponse(
    val orderId: UUID,
    val releasedItems: List<ReleasedItem>,
    val message: String
) {
    data class ReleasedItem(
        val sku: String,
        val quantity: Int
    )
}

data class InventoryCheckResponse(
    val sku: String,
    val available: Boolean,
    val availableQuantity: Int,
    val reservedQuantity: Int,
    val requestedQuantity: Int? = null,
    val canFulfill: Boolean? = null
)

data class ErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String
)

