package com.mercury.orders.inventory.outbox

import com.mercury.orders.events.OutboxEventEntity
import com.mercury.orders.events.OutboxEventStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "outbox_events")
data class InventoryOutboxEvent(
    @Id
    override val id: UUID,

    @Column(name = "event_type", nullable = false, length = 128)
    override val eventType: String,

    @Column(name = "aggregate_id", nullable = false, length = 128)
    override val aggregateId: String,

    @Column(name = "event_data", nullable = false, columnDefinition = "TEXT")
    override val eventData: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    override val status: OutboxEventStatus = OutboxEventStatus.PENDING,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "published_at")
    val publishedAt: Instant? = null,

    @Column(name = "retry_count", nullable = false)
    override val retryCount: Int = 0,

    @Column(name = "last_error")
    val lastError: String? = null
) : OutboxEventEntity


