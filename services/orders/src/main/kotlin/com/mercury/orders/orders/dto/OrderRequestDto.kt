package com.mercury.orders.orders.dto

import com.mercury.orders.orders.validation.ValidCustomerId
import com.mercury.orders.orders.validation.ValidOrder
import com.mercury.orders.orders.validation.ValidSku
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal

@ValidOrder
data class CreateOrderRequest(
    @field:NotBlank(message = "Customer ID cannot be blank")
    @field:ValidCustomerId(message = "Customer ID format is invalid")
    @field:Size(min = 1, max = 255, message = "Customer ID must be between 1 and 255 characters")
    val customerId: String,
    
    @field:NotNull(message = "Items list cannot be null")
    @field:NotEmpty(message = "Items list cannot be empty")
    @field:Valid
    @field:Size(max = 50, message = "Cannot create order with more than 50 items")
    val items: List<OrderItemRequest>
) {
    data class OrderItemRequest(
        @field:NotBlank(message = "SKU cannot be blank")
        @field:ValidSku(message = "SKU format is invalid")
        @field:Size(min = 1, max = 100, message = "SKU must be between 1 and 100 characters")
        val sku: String,
        
        @field:NotNull(message = "Quantity cannot be null")
        @field:Min(value = 1, message = "Quantity must be at least 1")
        @field:Max(value = 1000, message = "Quantity cannot exceed 1000")
        val quantity: Int,
        
        @field:NotNull(message = "Unit price cannot be null")
        @field:DecimalMin(value = "0.01", message = "Unit price must be at least 0.01")
        @field:DecimalMax(value = "999999.99", message = "Unit price cannot exceed 999,999.99")
        @field:Digits(integer = 6, fraction = 2, message = "Unit price must have at most 6 integer digits and 2 decimal places")
        val unitPrice: BigDecimal
    )
}

data class UpdateOrderStatusRequest(
    @field:NotNull(message = "Status cannot be null")
    val status: com.mercury.orders.orders.domain.OrderStatus
)

data class CancelOrderRequest(
    @field:NotBlank(message = "Cancellation reason cannot be blank")
    @field:Size(min = 1, max = 500, message = "Cancellation reason must be between 1 and 500 characters")
    val reason: String
)
