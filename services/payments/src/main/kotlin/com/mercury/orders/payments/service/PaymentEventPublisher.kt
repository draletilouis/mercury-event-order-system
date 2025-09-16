package com.mercury.orders.payments.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.mercury.orders.events.*
import com.mercury.orders.payments.domain.OutboxEventEntity
import com.mercury.orders.payments.repository.OutboxEventRepository
import com.mercury.orders.tracing.TracingMetrics
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

@Service
class PaymentEventPublisher(
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

    fun publishPaymentAuthorized(orderId: UUID, paymentId: String, authorizedAmount: BigDecimal) {
        val event = PaymentAuthorizedEvent(
            orderId = orderId,
            paymentId = paymentId,
            authorizedAmount = authorizedAmount
        )
        publish(event)
    }

    fun publishPaymentDeclined(orderId: UUID, paymentId: String, reason: String, declinedAmount: BigDecimal) {
        val event = PaymentDeclinedEvent(
            orderId = orderId,
            paymentId = paymentId,
            reason = reason,
            declinedAmount = declinedAmount
        )
        publish(event)
    }

    fun publishPaymentReversed(orderId: UUID, paymentId: String, reversedAmount: BigDecimal) {
        val event = PaymentReversedEvent(
            orderId = orderId,
            paymentId = paymentId,
            reversedAmount = reversedAmount
        )
        publish(event)
    }
}


