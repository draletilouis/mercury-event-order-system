package com.mercury.orders.inventory.repository

import com.mercury.orders.inventory.domain.InventoryReservation
import com.mercury.orders.inventory.domain.ReservationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface InventoryReservationRepository : JpaRepository<InventoryReservation, UUID> {
    fun findByOrderId(orderId: UUID): List<InventoryReservation>
    fun findByOrderIdAndStatus(orderId: UUID, status: ReservationStatus): List<InventoryReservation>
    fun findByStatusAndExpiresAtBefore(status: ReservationStatus, instant: Instant): List<InventoryReservation>
}


