package com.mercury.orders.inventory.jobs

import com.mercury.orders.inventory.domain.ReservationStatus
import com.mercury.orders.inventory.repository.InventoryReservationRepository
import com.mercury.orders.inventory.service.InventoryService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ReservationExpiryJob(
    private val reservationRepository: InventoryReservationRepository,
    private val inventoryService: InventoryService
) {
    @Scheduled(fixedDelayString = "\${app.reservations.expiry-scan-interval:60000}")
    fun releaseExpiredReservations() {
        val now = Instant.now()
        val expired = reservationRepository.findByStatusAndExpiresAtBefore(ReservationStatus.ACTIVE, now)
        expired.map { it.orderId }.distinct().forEach { orderId ->
            inventoryService.releaseForOrder(orderId)
        }
    }
}


