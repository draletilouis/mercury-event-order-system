package com.mercury.orders.payments.dto

import com.mercury.orders.payments.domain.PaymentStatus
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Request DTOs
 */
data class CreatePaymentRequest(
    @field:NotNull(message = "Order ID is required")
    val orderId: UUID,
    
    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    val amount: BigDecimal,
    
    @field:NotBlank(message = "Currency is required")
    @field:Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    val currency: String = "USD",
    
    val paymentMethod: String? = null
)

data class AuthorizePaymentRequest(
    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    val amount: BigDecimal,
    
    @field:NotBlank(message = "Currency is required")
    @field:Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    val currency: String = "USD",
    
    val paymentMethod: String? = null
)

data class ReversePaymentRequest(
    @field:NotBlank(message = "Reason is required")
    val reason: String
)

/**
 * Response DTOs
 */
data class PaymentResponse(
    val id: UUID,
    val orderId: UUID,
    val amount: BigDecimal,
    val currency: String,
    val status: PaymentStatus,
    val paymentMethod: String?,
    val externalPaymentId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Int
)

data class PaymentListResponse(
    val payments: List<PaymentResponse>,
    val totalCount: Int
)

data class PaymentAuthorizationResponse(
    val paymentId: UUID,
    val orderId: UUID,
    val status: PaymentStatus,
    val authorized: Boolean,
    val amount: BigDecimal,
    val currency: String,
    val message: String,
    val timestamp: Instant = Instant.now()
)

data class PaymentReversalResponse(
    val paymentId: UUID,
    val orderId: UUID,
    val status: PaymentStatus,
    val reversed: Boolean,
    val amount: BigDecimal,
    val currency: String,
    val reason: String,
    val timestamp: Instant = Instant.now()
)

data class PaymentStatusResponse(
    val paymentId: UUID,
    val orderId: UUID,
    val status: PaymentStatus,
    val amount: BigDecimal,
    val currency: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class ErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String
)


