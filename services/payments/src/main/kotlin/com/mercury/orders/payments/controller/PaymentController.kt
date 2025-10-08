package com.mercury.orders.payments.controller

import com.mercury.orders.payments.domain.Payment
import com.mercury.orders.payments.domain.PaymentStatus
import com.mercury.orders.payments.dto.*
import com.mercury.orders.payments.repository.PaymentRepository
import com.mercury.orders.payments.service.PaymentService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/payments")
class PaymentController(
    private val paymentService: PaymentService,
    private val paymentRepository: PaymentRepository
) {

    @GetMapping("/{paymentId}")
    fun getPayment(@PathVariable paymentId: UUID): ResponseEntity<PaymentResponse> {
        val payment = paymentService.getPayment(paymentId)
            ?: throw PaymentNotFoundException("Payment not found: $paymentId")
        
        return ResponseEntity.ok(payment.toResponse())
    }

    @GetMapping("/{paymentId}/status")
    fun getPaymentStatus(@PathVariable paymentId: UUID): ResponseEntity<PaymentStatusResponse> {
        val payment = paymentService.getPayment(paymentId)
            ?: throw PaymentNotFoundException("Payment not found: $paymentId")
        
        val response = PaymentStatusResponse(
            paymentId = payment.id,
            orderId = payment.orderId,
            status = payment.status,
            amount = payment.amount,
            currency = payment.currency,
            createdAt = payment.createdAt,
            updatedAt = payment.updatedAt
        )
        
        return ResponseEntity.ok(response)
    }

    @GetMapping("/order/{orderId}")
    fun getPaymentsByOrder(@PathVariable orderId: UUID): ResponseEntity<PaymentListResponse> {
        val payments = paymentService.getPaymentsByOrder(orderId)
        
        val response = PaymentListResponse(
            payments = payments.map { it.toResponse() },
            totalCount = payments.size
        )
        
        return ResponseEntity.ok(response)
    }

    @PostMapping
    fun createPayment(
        @Valid @RequestBody request: CreatePaymentRequest
    ): ResponseEntity<PaymentResponse> {
        val payment = paymentService.authorizePayment(
            orderId = request.orderId,
            amount = request.amount,
            currency = request.currency
        )
        
        return ResponseEntity.status(HttpStatus.CREATED).body(payment.toResponse())
    }

    @PostMapping("/{paymentId}/authorize")
    fun authorizePayment(
        @PathVariable paymentId: UUID,
        @Valid @RequestBody request: AuthorizePaymentRequest
    ): ResponseEntity<PaymentAuthorizationResponse> {
        // For now, we'll use the existing authorize logic from service
        // In a real implementation, this would handle manual authorization of existing payments
        val payment = paymentService.getPayment(paymentId)
            ?: throw PaymentNotFoundException("Payment not found: $paymentId")
        
        if (payment.status != PaymentStatus.PENDING) {
            throw InvalidPaymentStateException("Payment is not in pending state")
        }
        
        val authorizedPayment = paymentService.authorizePayment(
            orderId = payment.orderId,
            amount = request.amount,
            currency = request.currency
        )
        
        val response = PaymentAuthorizationResponse(
            paymentId = authorizedPayment.id,
            orderId = authorizedPayment.orderId,
            status = authorizedPayment.status,
            authorized = authorizedPayment.status == PaymentStatus.AUTHORIZED,
            amount = authorizedPayment.amount,
            currency = authorizedPayment.currency,
            message = when (authorizedPayment.status) {
                PaymentStatus.AUTHORIZED -> "Payment authorized successfully"
                PaymentStatus.DECLINED -> "Payment was declined"
                else -> "Payment authorization pending"
            }
        )
        
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{paymentId}/reverse")
    fun reversePayment(
        @PathVariable paymentId: UUID,
        @Valid @RequestBody request: ReversePaymentRequest
    ): ResponseEntity<PaymentReversalResponse> {
        val reversedPayment = paymentService.reversePayment(paymentId, request.reason)
        
        val response = PaymentReversalResponse(
            paymentId = reversedPayment.id,
            orderId = reversedPayment.orderId,
            status = reversedPayment.status,
            reversed = reversedPayment.status == PaymentStatus.REVERSED,
            amount = reversedPayment.amount,
            currency = reversedPayment.currency,
            reason = request.reason
        )
        
        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun listPayments(
        @RequestParam(required = false) status: PaymentStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ResponseEntity<PaymentListResponse> {
        val payments = if (status != null) {
            paymentRepository.findByStatus(status)
        } else {
            paymentRepository.findAll()
        }
        
        val response = PaymentListResponse(
            payments = payments.map { it.toResponse() },
            totalCount = payments.size
        )
        
        return ResponseEntity.ok(response)
    }
}

// Extension function to convert domain model to response
private fun Payment.toResponse() = PaymentResponse(
    id = id,
    orderId = orderId,
    amount = amount,
    currency = currency,
    status = status,
    paymentMethod = paymentMethod,
    externalPaymentId = externalPaymentId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version
)

// Custom exceptions
class PaymentNotFoundException(message: String) : RuntimeException(message)
class InvalidPaymentStateException(message: String) : RuntimeException(message)
class PaymentAuthorizationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class PaymentReversalException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)


