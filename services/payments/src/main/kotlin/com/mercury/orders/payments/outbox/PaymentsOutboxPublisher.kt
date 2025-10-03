package com.mercury.orders.payments.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.mercury.orders.events.DomainEvent
import com.mercury.orders.events.OutboxEventStatus
import com.mercury.orders.payments.repository.OutboxEventRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class PaymentsOutboxPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    private val outboxRepository: OutboxEventRepository,
    @Value("\${app.outbox.max-retries:3}") private val maxRetries: Int,
    @Value("\${app.outbox.batch-size:100}") private val batchSize: Int,
    @Value("\${app.outbox.retention-days:7}") private val retentionDays: Long
) {

    @Scheduled(fixedDelayString = "\${app.outbox.polling-interval:5000}")
    @Transactional
    fun publishPendingEvents() {
        val pending = outboxRepository
            .findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING)
            .take(batchSize)

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

    @Scheduled(fixedDelayString = "\${app.outbox.polling-interval:5000}")
    @Transactional
    fun retryFailedEvents() {
        val failed = outboxRepository
            .findFailedEventsForRetry(maxRetries)
            .take(batchSize)

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

    @Scheduled(cron = "\${app.outbox.cleanup-cron:0 0 * * * *}")
    @Transactional
    fun cleanupOldPublishedEvents() {
        val cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS)
        outboxRepository.deleteOldPublishedEvents(cutoff)
    }

    private fun topicFor(eventType: String): String = when (eventType) {
        "OrderCreated" -> "order-events"
        "PaymentAuthorized", "PaymentDeclined", "PaymentReversed" -> "payment-events"
        "InventoryReserved", "InventoryInsufficient", "InventoryReleased" -> "inventory-events"
        "OrderCompleted", "OrderCancelled" -> "order-events"
        else -> "domain-events"
    }
}






