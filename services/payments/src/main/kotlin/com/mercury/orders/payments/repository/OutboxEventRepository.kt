package com.mercury.orders.payments.repository

import com.mercury.orders.payments.domain.OutboxEventEntity
import com.mercury.orders.events.OutboxEventStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface OutboxEventRepository : JpaRepository<OutboxEventEntity, UUID> {
    
    fun findByStatusOrderByCreatedAtAsc(status: OutboxEventStatus): List<OutboxEventEntity>
    
    @Modifying
    @Query("UPDATE OutboxEventEntity e SET e.status = :status, e.publishedAt = :publishedAt WHERE e.id = :id")
    fun markAsPublished(@Param("id") id: UUID, @Param("status") status: OutboxEventStatus, @Param("publishedAt") publishedAt: Instant)
    
    @Modifying
    @Query("UPDATE OutboxEventEntity e SET e.status = :status, e.retryCount = e.retryCount + 1, e.lastError = :error WHERE e.id = :id")
    fun markAsFailed(@Param("id") id: UUID, @Param("status") status: OutboxEventStatus, @Param("error") error: String)
    
    @Query("SELECT e FROM OutboxEventEntity e WHERE e.status = 'FAILED' AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC")
    fun findFailedEventsForRetry(@Param("maxRetries") maxRetries: Int): List<OutboxEventEntity>
    
    @Modifying
    @Query("DELETE FROM OutboxEventEntity e WHERE e.status = 'PUBLISHED' AND e.publishedAt < :cutoffDate")
    fun deleteOldPublishedEvents(@Param("cutoffDate") cutoffDate: Instant)
}








































