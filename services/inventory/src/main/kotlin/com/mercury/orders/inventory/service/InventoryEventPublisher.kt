package com.mercury.orders.inventory.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.mercury.orders.events.*
import com.mercury.orders.inventory.outbox.InventoryOutboxEvent
import com.mercury.orders.inventory.outbox.InventoryOutboxJpaRepository
import com.mercury.orders.tracing.TracingMetrics
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class InventoryEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    private val outboxRepository: InventoryOutboxJpaRepository,
    private val tracingMetrics: TracingMetrics
) : EventPublisher {

    @Transactional
    override fun publish(event: DomainEvent) {
        publishWithKey(event, event.eventId.toString())
    }

    @Transactional
    override fun publishWithKey(event: DomainEvent, key: String) {
        try {
            val eventData = objectMapper.writeValueAsString(event)
            val outboxEvent = InventoryOutboxEvent(
                id = UUID.randomUUID(),
                eventType = event.eventType,
                aggregateId = key,
                eventData = eventData,
                status = OutboxEventStatus.PENDING
            )
            
            outboxRepository.save(outboxEvent)
            tracingMetrics.incrementEventPublished(event.eventType)
        } catch (e: Exception) {
            tracingMetrics.incrementEventProcessingError(event.eventType, "PUBLISH_ERROR")
            throw RuntimeException("Failed to publish event: ${event.eventType}", e)
        }
    }

    fun publishInventoryReserved(orderId: UUID, reservedItems: List<InventoryReservedEvent.ReservedItem>) {
        val event = InventoryReservedEvent(
            orderId = orderId,
            reservedItems = reservedItems
        )
        publish(event)
    }

    fun publishInventoryInsufficient(orderId: UUID, insufficientItems: List<InventoryInsufficientEvent.InsufficientItem>) {
        val event = InventoryInsufficientEvent(
            orderId = orderId,
            insufficientItems = insufficientItems
        )
        publish(event)
    }

    fun publishInventoryReleased(orderId: UUID, releasedItems: List<InventoryReleasedEvent.ReleasedItem>) {
        val event = InventoryReleasedEvent(
            orderId = orderId,
            releasedItems = releasedItems
        )
        publish(event)
    }
}


