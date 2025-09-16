package com.mercury.orders.payments.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "payments")
data class Payment(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "order_id", nullable = false)
    val orderId: UUID,
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    val amount: BigDecimal,
    
    @Column(name = "currency", nullable = false, length = 3)
    val currency: String = "USD",
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: PaymentStatus = PaymentStatus.PENDING,
    
    @Column(name = "payment_method", length = 100)
    val paymentMethod: String? = null,
    
    @Column(name = "external_payment_id")
    val externalPaymentId: String? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
    
    @Version
    @Column(name = "version", nullable = false)
    val version: Int = 0,
    
    @OneToMany(mappedBy = "paymentId", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val attempts: List<PaymentAttempt> = emptyList()
) {
    fun canTransitionTo(newStatus: PaymentStatus): Boolean {
        return when (status) {
            PaymentStatus.PENDING -> newStatus in listOf(PaymentStatus.AUTHORIZED, PaymentStatus.DECLINED)
            PaymentStatus.AUTHORIZED -> newStatus == PaymentStatus.REVERSED
            PaymentStatus.DECLINED -> false
            PaymentStatus.REVERSED -> false
        }
    }
    
    fun transitionTo(newStatus: PaymentStatus): Payment {
        if (!canTransitionTo(newStatus)) {
            throw IllegalStateException("Cannot transition from $status to $newStatus")
        }
        return copy(status = newStatus, updatedAt = Instant.now())
    }
}

enum class PaymentStatus {
    PENDING,
    AUTHORIZED,
    DECLINED,
    REVERSED
}

