package com.mercury.orders.payments.domain

import com.mercury.orders.events.OutboxEventStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "outbox_events")
data class OutboxEventEntity(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "event_type", nullable = false)
    val eventType: String,
    
    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: String,
    
    @Column(name = "event_data", nullable = false, columnDefinition = "TEXT")
    val eventData: String,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: OutboxEventStatus = OutboxEventStatus.PENDING,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "published_at")
    val publishedAt: Instant? = null,
    
    @Column(name = "retry_count", nullable = false)
    val retryCount: Int = 0,
    
    @Column(name = "last_error", columnDefinition = "TEXT")
    val lastError: String? = null
)















































