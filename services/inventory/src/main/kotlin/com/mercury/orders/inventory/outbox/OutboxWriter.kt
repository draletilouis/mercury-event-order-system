package com.mercury.orders.inventory.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.mercury.orders.events.DomainEvent
import com.mercury.orders.events.OutboxEventStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
class OutboxWriter(
    private val objectMapper: ObjectMapper,
    private val outboxRepository: InventoryOutboxJpaRepository
) {
    @Transactional
    fun enqueueEvent(event: DomainEvent, aggregateId: String) {
        val payload = objectMapper.writeValueAsString(event)
        val outboxEvent = InventoryOutboxEvent(
            id = UUID.randomUUID(),
            eventType = event.eventType,
            aggregateId = aggregateId,
            eventData = payload,
            status = OutboxEventStatus.PENDING
        )
        outboxRepository.save(outboxEvent)
    }
}


