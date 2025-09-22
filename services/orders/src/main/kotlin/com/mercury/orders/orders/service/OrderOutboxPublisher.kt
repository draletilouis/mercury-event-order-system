package com.mercury.orders.orders.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.mercury.orders.events.DomainEvent
import com.mercury.orders.orders.domain.OutboxEventEntity
import com.mercury.orders.orders.repository.OutboxEventRepository
import com.mercury.orders.tracing.TracingMetrics
import org.springframework.data.domain.PageRequest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class OrderOutboxPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    private val outboxEventRepository: OutboxEventRepository,
    private val tracingMetrics: TracingMetrics
) {

    @Scheduled(fixedDelayString = "\${app.outbox.polling-interval:5000}")
    @Transactional
    fun publishPendingEvents() {
        val batchSize = 100 // configurable via application.yml
        val pageable = PageRequest.of(0, batchSize)
        val pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
            com.mercury.orders.events.OutboxEventStatus.PENDING,
            pageable
        )
        
        pendingEvents.forEach { event ->
            try {
                val domainEvent = objectMapper.readValue(event.eventData, DomainEvent::class.java)
                val topic = getTopicForEvent(domainEvent.eventType)
                
                kafkaTemplate.send(topic, event.aggregateId, domainEvent)
                
                outboxEventRepository.markAsPublished(
                    event.id,
                    com.mercury.orders.events.OutboxEventStatus.PUBLISHED,
                    Instant.now()
                )
                
                tracingMetrics.incrementEventPublished(domainEvent.eventType)
            } catch (e: Exception) {
                handlePublishError(event, e)
            }
        }
    }

    @Scheduled(fixedDelayString = "\${app.outbox.retry-interval:30000}")
    @Transactional
    fun retryFailedEvents() {
        val maxRetries = 3
        val failedEvents = outboxEventRepository.findFailedEventsForRetry(maxRetries)
        
        failedEvents.forEach { event ->
            try {
                val domainEvent = objectMapper.readValue(event.eventData, DomainEvent::class.java)
                val topic = getTopicForEvent(domainEvent.eventType)
                
                kafkaTemplate.send(topic, event.aggregateId, domainEvent)
                
                outboxEventRepository.markAsPublished(
                    event.id,
                    com.mercury.orders.events.OutboxEventStatus.PUBLISHED,
                    Instant.now()
                )
                
                tracingMetrics.incrementEventPublished(domainEvent.eventType)
            } catch (e: Exception) {
                outboxEventRepository.markAsFailed(
                    event.id,
                    com.mercury.orders.events.OutboxEventStatus.FAILED,
                    "Retry failed: ${e.message}"
                )
                
                tracingMetrics.incrementEventProcessingError(
                    event.eventType, 
                    "RETRY_FAILED"
                )
            }
        }
    }

    @Scheduled(fixedDelayString = "\${app.outbox.cleanup-interval:3600000}") // 1 hour
    @Transactional
    fun cleanupOldEvents() {
        val cutoffDate = Instant.now().minusSeconds(24 * 60 * 60) // 24 hours ago
        outboxEventRepository.deleteOldPublishedEvents(cutoffDate)
    }

    private fun handlePublishError(event: OutboxEventEntity, error: Exception) {
        val maxRetries = 3
        if (event.retryCount >= maxRetries) {
            outboxEventRepository.markAsFailed(
                event.id,
                com.mercury.orders.events.OutboxEventStatus.FAILED,
                "Max retries exceeded: ${error.message}"
            )
            
            tracingMetrics.incrementEventProcessingError(
                event.eventType, 
                "MAX_RETRIES_EXCEEDED"
            )
        } else {
            outboxEventRepository.markAsFailed(
                event.id,
                com.mercury.orders.events.OutboxEventStatus.FAILED,
                error.message ?: "Unknown error"
            )
            
            tracingMetrics.incrementEventProcessingError(
                event.eventType, 
                "PUBLISH_FAILED"
            )
        }
    }

    private fun getTopicForEvent(eventType: String): String {
        return when (eventType) {
            "OrderCreated" -> "order-events"
            "OrderCompleted" -> "order-events"
            "OrderCancelled" -> "order-events"
            else -> "domain-events"
        }
    }
}








