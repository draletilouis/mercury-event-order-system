package com.mercury.orders.payments.service

import com.mercury.orders.payments.domain.Payment
import com.mercury.orders.payments.domain.PaymentAttempt
import com.mercury.orders.payments.domain.PaymentStatus
import com.mercury.orders.payments.repository.PaymentRepository
import com.mercury.orders.tracing.TracingMetrics
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

@Service
@Transactional
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val paymentEventPublisher: PaymentEventPublisher,
    private val tracingMetrics: TracingMetrics
) {

    fun authorizePayment(orderId: UUID, amount: BigDecimal, currency: String = "USD"): Payment {
        val payment = Payment(
            orderId = orderId,
            amount = amount,
            currency = currency,
            status = PaymentStatus.PENDING
        )
        
        val savedPayment = paymentRepository.save(payment)
        
        // Simulate payment authorization (mock external service call)
        val authorizationResult = simulatePaymentAuthorization(savedPayment)
        
        val updatedPayment = when (authorizationResult.success) {
            true -> {
                val authorizedPayment = savedPayment.transitionTo(PaymentStatus.AUTHORIZED)
                paymentEventPublisher.publishPaymentAuthorized(
                    orderId = orderId,
                    paymentId = savedPayment.id,
                    authorizedAmount = amount
                )
                tracingMetrics.incrementPaymentAuthorized()
                authorizedPayment
            }
            false -> {
                val declinedPayment = savedPayment.transitionTo(PaymentStatus.DECLINED)
                paymentEventPublisher.publishPaymentDeclined(
                    orderId = orderId,
                    paymentId = savedPayment.id,
                    reason = authorizationResult.reason ?: "Payment declined",
                    declinedAmount = amount
                )
                tracingMetrics.incrementPaymentDeclined()
                declinedPayment
            }
        }
        
        return paymentRepository.save(updatedPayment)
    }

    fun reversePayment(paymentId: UUID, reason: String): Payment {
        val payment = paymentRepository.findById(paymentId)
            .orElseThrow { IllegalArgumentException("Payment not found: $paymentId") }
        
        if (payment.status != PaymentStatus.AUTHORIZED) {
            throw IllegalStateException("Cannot reverse payment with status: ${payment.status}")
        }
        
        val reversedPayment = payment.transitionTo(PaymentStatus.REVERSED)
        val savedPayment = paymentRepository.save(reversedPayment)
        
        paymentEventPublisher.publishPaymentReversed(
            orderId = payment.orderId,
            paymentId = paymentId,
            reversedAmount = payment.amount
        )
        
        return savedPayment
    }

    fun getPayment(paymentId: UUID): Payment? {
        return paymentRepository.findById(paymentId).orElse(null)
    }

    fun getPaymentsByOrder(orderId: UUID): List<Payment> {
        return paymentRepository.findByOrderId(orderId)
    }

    private fun simulatePaymentAuthorization(payment: Payment): AuthorizationResult {
        // Mock payment provider logic
        // In real implementation, this would call an external payment service
        
        return when {
            payment.amount > BigDecimal.valueOf(10000) -> {
                AuthorizationResult(success = false, reason = "Amount exceeds limit")
            }
            payment.amount <= BigDecimal.ZERO -> {
                AuthorizationResult(success = false, reason = "Invalid amount")
            }
            else -> {
                // Simulate random failures for testing (10% failure rate)
                if (Math.random() < 0.1) {
                    AuthorizationResult(success = false, reason = "Payment provider timeout")
                } else {
                    AuthorizationResult(success = true)
                }
            }
        }
    }

    private data class AuthorizationResult(
        val success: Boolean,
        val reason: String? = null
    )
}


