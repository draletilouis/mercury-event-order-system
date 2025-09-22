package com.mercury.orders.orders.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.mercury.orders.events.*
import com.mercury.orders.orders.domain.Order
import com.mercury.orders.orders.domain.OutboxEventEntity
import com.mercury.orders.orders.repository.OutboxEventRepository
import com.mercury.orders.tracing.TracingMetrics
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class OrderEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    private val outboxEventRepository: OutboxEventRepository,
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
            val outboxEvent = OutboxEventEntity(
                eventType = event.eventType,
                aggregateId = key,
                eventData = eventData,
                status = OutboxEventStatus.PENDING
            )
            
            outboxEventRepository.save(outboxEvent)
            tracingMetrics.incrementEventPublished(event.eventType)
        } catch (e: Exception) {
            tracingMetrics.incrementEventProcessingError(event.eventType, "PUBLISH_ERROR")
            throw RuntimeException("Failed to publish event: ${event.eventType}", e)
        }
    }

    fun publishOrderCreated(order: Order) {
        val event = OrderCreatedEvent(
            orderId = order.id,
            customerId = order.customerId,
            items = order.items.map { item ->
                OrderCreatedEvent.OrderItem(
                    sku = item.sku,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice
                )
            },
            totalAmount = order.totalAmount
        )
        publish(event)
    }

    fun publishOrderCompleted(orderId: UUID) {
        val event = OrderCompletedEvent(orderId = orderId)
        publish(event)
    }

    fun publishOrderCancelled(orderId: UUID, reason: String) {
        val event = OrderCancelledEvent(orderId = orderId, reason = reason)
        publish(event)
    }
}

































