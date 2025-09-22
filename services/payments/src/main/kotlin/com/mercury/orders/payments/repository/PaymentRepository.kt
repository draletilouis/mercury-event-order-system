package com.mercury.orders.payments.repository

import com.mercury.orders.payments.domain.Payment
import com.mercury.orders.payments.domain.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PaymentRepository : JpaRepository<Payment, UUID> {
    
    fun findByOrderId(orderId: UUID): List<Payment>
    
    fun findByStatus(status: PaymentStatus): List<Payment>
    
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId AND p.status = :status")
    fun findByOrderIdAndStatus(@Param("orderId") orderId: UUID, @Param("status") status: PaymentStatus): Payment?
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    fun countByStatus(@Param("status") status: PaymentStatus): Long
}

































