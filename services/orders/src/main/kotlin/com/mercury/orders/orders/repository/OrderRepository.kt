package com.mercury.orders.orders.repository

import com.mercury.orders.orders.domain.Order
import com.mercury.orders.orders.domain.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OrderRepository : JpaRepository<Order, UUID> {
    
    fun findByCustomerId(customerId: String): List<Order>
    
    fun findByStatus(status: OrderStatus): List<Order>
    
    @Query("SELECT o FROM Order o WHERE o.status IN :statuses ORDER BY o.createdAt DESC")
    fun findByStatusIn(@Param("statuses") statuses: List<OrderStatus>): List<Order>
    
    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId AND o.status = :status ORDER BY o.createdAt DESC")
    fun findByCustomerIdAndStatus(@Param("customerId") customerId: String, @Param("status") status: OrderStatus): List<Order>
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    fun countByStatus(@Param("status") status: OrderStatus): Long
    
    @Query("SELECT o FROM Order o WHERE o.createdAt >= :fromDate AND o.createdAt <= :toDate ORDER BY o.createdAt DESC")
    fun findByCreatedAtBetween(@Param("fromDate") fromDate: java.time.Instant, @Param("toDate") toDate: java.time.Instant): List<Order>
}
