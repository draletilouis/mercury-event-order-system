package com.mercury.orders.inventory.outbox

import com.mercury.orders.events.OutboxEventStatus
import com.mercury.orders.events.OutboxRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface InventoryOutboxJpaRepository : JpaRepository<InventoryOutboxEvent, UUID> {
    fun findByStatusOrderByCreatedAtAsc(status: OutboxEventStatus): List<InventoryOutboxEvent>

    @Modifying
    @Query("update InventoryOutboxEvent e set e.status = :status, e.publishedAt = :publishedAt where e.id = :id")
    fun markAsPublished(id: UUID, status: OutboxEventStatus, publishedAt: Instant)

    @Modifying
    @Query("update InventoryOutboxEvent e set e.status = :status, e.retryCount = e.retryCount + 1, e.lastError = :error where e.id = :id")
    fun markAsFailed(id: UUID, status: OutboxEventStatus, error: String)

    @Query("select e from InventoryOutboxEvent e where e.status = 'FAILED' and e.retryCount < :maxRetries order by e.createdAt asc")
    fun findFailedEventsForRetry(maxRetries: Int): List<InventoryOutboxEvent>
}

@Repository
class InventoryOutboxRepository(
    private val jpaRepository: InventoryOutboxJpaRepository
) : OutboxRepository<InventoryOutboxEvent> {
    override fun findByStatusOrderByCreatedAtAsc(status: OutboxEventStatus): List<InventoryOutboxEvent> =
        jpaRepository.findByStatusOrderByCreatedAtAsc(status)

    override fun markAsPublished(id: UUID, status: OutboxEventStatus, publishedAt: Instant) =
        jpaRepository.markAsPublished(id, status, publishedAt)

    override fun markAsFailed(id: UUID, status: OutboxEventStatus, error: String) =
        jpaRepository.markAsFailed(id, status, error)

    override fun findFailedEventsForRetry(maxRetries: Int): List<InventoryOutboxEvent> =
        jpaRepository.findFailedEventsForRetry(maxRetries)
}


