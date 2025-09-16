package com.mercury.orders.events

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Generic outbox event publisher for reliable event publishing
 */
@Component
class OutboxEventPublisher<T>(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    private val outboxRepository: OutboxRepository<T>
) where T : OutboxEventEntity {

    @Scheduled(fixedDelayString = "\${app.outbox.polling-interval:5000}")
    @Transactional
    fun publishPendingEvents() {
        val pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING)
        
        pendingEvents.forEach { event ->
            try {
                val domainEvent = objectMapper.readValue(event.eventData, DomainEvent::class.java)
                val topic = getTopicForEvent(domainEvent.eventType)
                
                kafkaTemplate.send(topic, event.aggregateId, domainEvent)
                
                outboxRepository.markAsPublished(
                    event.id,
                    OutboxEventStatus.PUBLISHED,
                    Instant.now()
                )
            } catch (e: Exception) {
                handlePublishError(event, e)
            }
        }
    }

    @Scheduled(fixedDelayString = "\${app.outbox.retry-interval:30000}")
    @Transactional
    fun retryFailedEvents() {
        val maxRetries = 3 // configurable
        val failedEvents = outboxRepository.findFailedEventsForRetry(maxRetries)
        
        failedEvents.forEach { event ->
            try {
                val domainEvent = objectMapper.readValue(event.eventData, DomainEvent::class.java)
                val topic = getTopicForEvent(domainEvent.eventType)
                
                kafkaTemplate.send(topic, event.aggregateId, domainEvent)
                
                outboxRepository.markAsPublished(
                    event.id,
                    OutboxEventStatus.PUBLISHED,
                    Instant.now()
                )
            } catch (e: Exception) {
                outboxRepository.markAsFailed(
                    event.id,
                    OutboxEventStatus.FAILED,
                    "Retry failed: ${e.message}"
                )
            }
        }
    }

    private fun handlePublishError(event: T, error: Exception) {
        val maxRetries = 3 // configurable
        if (event.retryCount >= maxRetries) {
            outboxRepository.markAsFailed(
                event.id,
                OutboxEventStatus.FAILED,
                "Max retries exceeded: ${error.message}"
            )
        } else {
            outboxRepository.markAsFailed(
                event.id,
                OutboxEventStatus.FAILED,
                error.message ?: "Unknown error"
            )
        }
    }

    private fun getTopicForEvent(eventType: String): String {
        return when (eventType) {
            "OrderCreated" -> "order-events"
            "PaymentAuthorized", "PaymentDeclined", "PaymentReversed" -> "payment-events"
            "InventoryReserved", "InventoryInsufficient", "InventoryReleased" -> "inventory-events"
            "OrderCompleted", "OrderCancelled" -> "order-events"
            else -> "domain-events"
        }
    }
}

/**
 * Generic repository interface for outbox events
 */
interface OutboxRepository<T> where T : OutboxEventEntity {
    fun findByStatusOrderByCreatedAtAsc(status: OutboxEventStatus): List<T>
    fun markAsPublished(id: java.util.UUID, status: OutboxEventStatus, publishedAt: Instant)
    fun markAsFailed(id: java.util.UUID, status: OutboxEventStatus, error: String)
    fun findFailedEventsForRetry(maxRetries: Int): List<T>
}

/**
 * Base interface for outbox event entities
 */
interface OutboxEventEntity {
    val id: java.util.UUID
    val eventType: String
    val aggregateId: String
    val eventData: String
    val status: OutboxEventStatus
    val retryCount: Int
}
