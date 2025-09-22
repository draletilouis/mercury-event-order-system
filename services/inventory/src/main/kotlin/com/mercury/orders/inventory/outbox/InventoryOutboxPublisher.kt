package com.mercury.orders.inventory.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.mercury.orders.events.DomainEvent
import com.mercury.orders.events.OutboxEventStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class InventoryOutboxPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    private val outboxRepository: InventoryOutboxRepository
) {
    @Scheduled(fixedDelayString = "\${app.outbox.polling-interval:5000}")
    @Transactional
    fun publishPendingEvents() {
        val pending = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING)
        pending.forEach { event ->
            try {
                val domainEvent = objectMapper.readValue(event.eventData, DomainEvent::class.java)
                val topic = topicFor(domainEvent.eventType)
                kafkaTemplate.send(topic, event.aggregateId, domainEvent)
                outboxRepository.markAsPublished(event.id, OutboxEventStatus.PUBLISHED, Instant.now())
            } catch (e: Exception) {
                outboxRepository.markAsFailed(event.id, OutboxEventStatus.FAILED, e.message ?: "Unknown error")
            }
        }
    }

    @Scheduled(fixedDelayString = "\${app.outbox.retry-interval:30000}")
    @Transactional
    fun retryFailedEvents() {
        val failed = outboxRepository.findFailedEventsForRetry(3)
        failed.forEach { event ->
            try {
                val domainEvent = objectMapper.readValue(event.eventData, DomainEvent::class.java)
                val topic = topicFor(domainEvent.eventType)
                kafkaTemplate.send(topic, event.aggregateId, domainEvent)
                outboxRepository.markAsPublished(event.id, OutboxEventStatus.PUBLISHED, Instant.now())
            } catch (e: Exception) {
                outboxRepository.markAsFailed(event.id, OutboxEventStatus.FAILED, "Retry failed: ${e.message}")
            }
        }
    }

    private fun topicFor(eventType: String): String = when (eventType) {
        "OrderCreated" -> "order-events"
        "PaymentAuthorized", "PaymentDeclined", "PaymentReversed" -> "payment-events"
        "InventoryReserved", "InventoryInsufficient", "InventoryReleased" -> "inventory-events"
        "OrderCompleted", "OrderCancelled" -> "order-events"
        else -> "domain-events"
    }
}


