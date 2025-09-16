package com.mercury.orders.payments.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "payment_attempts")
data class PaymentAttempt(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "payment_id", nullable = false)
    val paymentId: UUID,
    
    @Column(name = "attempt_number", nullable = false)
    val attemptNumber: Int,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: AttemptStatus,
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null,
    
    @Column(name = "external_response", columnDefinition = "TEXT")
    val externalResponse: String? = null,
    
    @Column(name = "attempted_at", nullable = false)
    val attemptedAt: Instant = Instant.now()
)

enum class AttemptStatus {
    PENDING,
    SUCCESS,
    FAILED
}

