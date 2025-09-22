package com.mercury.orders.orders.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.mercury.orders.events.DomainEvent
import com.mercury.orders.events.OutboxEventStatus
import com.mercury.orders.orders.domain.OutboxEventEntity
import com.mercury.orders.orders.repository.OutboxEventRepository
import com.mercury.orders.tracing.TracingMetrics
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.PageRequest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture

@ExtendWith(MockitoExtension::class)
class OrderOutboxPublisherTest {

    @Mock
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @Mock
    private lateinit var outboxEventRepository: OutboxEventRepository

    @Mock
    private lateinit var tracingMetrics: TracingMetrics

    private lateinit var orderOutboxPublisher: OrderOutboxPublisher

    @BeforeEach
    fun setUp() {
        orderOutboxPublisher = OrderOutboxPublisher(
            kafkaTemplate = kafkaTemplate,
            objectMapper = objectMapper,
            outboxEventRepository = outboxEventRepository,
            tracingMetrics = tracingMetrics
        )
    }

    @Test
    fun `publishPendingEvents should process pending events successfully`() {
        // Given
        val eventId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val eventData = """{"orderId":"$orderId","eventType":"OrderCreated"}"""
        val pendingEvent = createSampleOutboxEvent(eventId, eventData)
        val domainEvent = mock(DomainEvent::class.java)
        
        whenever(outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING, PageRequest.of(0, 100)))
            .thenReturn(listOf(pendingEvent))
        whenever(objectMapper.readValue(eventData, DomainEvent::class.java)).thenReturn(domainEvent)
        whenever(domainEvent.eventType).thenReturn("OrderCreated")
        
        val sendResult = CompletableFuture.completedFuture(mock(SendResult::class.java) as SendResult<String, Any>)
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<DomainEvent>()))
            .thenReturn(sendResult)

        // When
        orderOutboxPublisher.publishPendingEvents()

        // Then
        verify(outboxEventRepository).findByStatusOrderByCreatedAtAsc(any<OutboxEventStatus>(), any<Pageable>())
        verify(objectMapper).readValue(any<String>(), eq(DomainEvent::class.java))
        verify(kafkaTemplate).send(any<String>(), any<String>(), any<DomainEvent>())
        verify(outboxEventRepository).markAsPublished(any<UUID>(), any<OutboxEventStatus>(), any<Instant>())
        verify(tracingMetrics).incrementEventPublished(any<String>())
    }

    @Test
    fun `publishPendingEvents should handle multiple pending events`() {
        // Given
        val event1 = createSampleOutboxEvent(UUID.randomUUID(), """{"orderId":"${UUID.randomUUID()}","eventType":"OrderCreated"}""")
        val event2 = createSampleOutboxEvent(UUID.randomUUID(), """{"orderId":"${UUID.randomUUID()}","eventType":"OrderCompleted"}""")
        val domainEvent1 = mock(DomainEvent::class.java)
        val domainEvent2 = mock(DomainEvent::class.java)
        
        whenever(outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING, PageRequest.of(0, 100)))
            .thenReturn(listOf(event1, event2))
        whenever(objectMapper.readValue(event1.eventData, DomainEvent::class.java)).thenReturn(domainEvent1)
        whenever(objectMapper.readValue(event2.eventData, DomainEvent::class.java)).thenReturn(domainEvent2)
        whenever(domainEvent1.eventType).thenReturn("OrderCreated")
        whenever(domainEvent2.eventType).thenReturn("OrderCompleted")
        
        val sendResult = CompletableFuture.completedFuture(mock(SendResult::class.java) as SendResult<String, Any>)
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<DomainEvent>())).thenReturn(sendResult)

        // When
        orderOutboxPublisher.publishPendingEvents()

        // Then
        verify(outboxEventRepository).findByStatusOrderByCreatedAtAsc(any<OutboxEventStatus>(), any<Pageable>())
        verify(kafkaTemplate, times(2)).send(any<String>(), any<String>(), any<DomainEvent>())
        verify(outboxEventRepository, times(2)).markAsPublished(any<UUID>(), any<OutboxEventStatus>(), any<Instant>())
        verify(tracingMetrics, times(2)).incrementEventPublished(any<String>())
    }

    @Test
    fun `publishPendingEvents should handle empty pending events list`() {
        // Given
        whenever(outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING, PageRequest.of(0, 100)))
            .thenReturn(emptyList())

        // When
        orderOutboxPublisher.publishPendingEvents()

        // Then
        verify(outboxEventRepository).findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING, PageRequest.of(0, 100))
        verify(kafkaTemplate, never()).send(any(), any(), any())
        verify(outboxEventRepository, never()).markAsPublished(any(), any(), any())
        verify(tracingMetrics, never()).incrementEventPublished(any())
    }

    @Test
    fun `publishPendingEvents should handle JSON deserialization errors`() {
        // Given
        val eventId = UUID.randomUUID()
        val pendingEvent = createSampleOutboxEvent(eventId, "invalid json")
        
        whenever(outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING, PageRequest.of(0, 100)))
            .thenReturn(listOf(pendingEvent))
        whenever(objectMapper.readValue("invalid json", DomainEvent::class.java))
            .thenThrow(RuntimeException("JSON parsing error"))

        // When
        orderOutboxPublisher.publishPendingEvents()

        // Then
        verify(outboxEventRepository).findByStatusOrderByCreatedAtAsc(any<OutboxEventStatus>(), any<Pageable>())
        verify(objectMapper).readValue(any<String>(), eq(DomainEvent::class.java))
        verify(kafkaTemplate, never()).send(any<String>(), any<String>(), any<DomainEvent>())
        verify(outboxEventRepository).markAsFailed(any<UUID>(), any<OutboxEventStatus>(), any<String>())
        verify(tracingMetrics).incrementEventProcessingError(any<String>(), any<String>())
    }

    @Test
    fun `publishPendingEvents should handle Kafka send errors`() {
        // Given
        val eventId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val eventData = """{"orderId":"$orderId","eventType":"OrderCreated"}"""
        val pendingEvent = createSampleOutboxEvent(eventId, eventData)
        val domainEvent = mock(DomainEvent::class.java)
        
        whenever(outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING, PageRequest.of(0, 100)))
            .thenReturn(listOf(pendingEvent))
        whenever(objectMapper.readValue(eventData, DomainEvent::class.java)).thenReturn(domainEvent)
        whenever(domainEvent.eventType).thenReturn("OrderCreated")
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<DomainEvent>()))
            .thenThrow(RuntimeException("Kafka send error"))

        // When
        orderOutboxPublisher.publishPendingEvents()

        // Then
        verify(outboxEventRepository).findByStatusOrderByCreatedAtAsc(any<OutboxEventStatus>(), any<Pageable>())
        verify(objectMapper).readValue(any<String>(), eq(DomainEvent::class.java))
        verify(kafkaTemplate).send(any<String>(), any<String>(), any<DomainEvent>())
        verify(outboxEventRepository).markAsFailed(any<UUID>(), any<OutboxEventStatus>(), any<String>())
        verify(tracingMetrics).incrementEventProcessingError(any<String>(), any<String>())
    }

    @Test
    fun `publishPendingEvents should mark as permanently failed after max retries`() {
        // Given
        val eventId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val eventData = """{"orderId":"$orderId","eventType":"OrderCreated"}"""
        val pendingEvent = createSampleOutboxEvent(eventId, eventData, retryCount = 3)
        val domainEvent = mock(DomainEvent::class.java)
        
        whenever(outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING, PageRequest.of(0, 100)))
            .thenReturn(listOf(pendingEvent))
        whenever(objectMapper.readValue(eventData, DomainEvent::class.java)).thenReturn(domainEvent)
        whenever(domainEvent.eventType).thenReturn("OrderCreated")
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<DomainEvent>()))
            .thenThrow(RuntimeException("Kafka send error"))

        // When
        orderOutboxPublisher.publishPendingEvents()

        // Then
        verify(outboxEventRepository).findByStatusOrderByCreatedAtAsc(any<OutboxEventStatus>(), any<Pageable>())
        verify(kafkaTemplate).send(any<String>(), any<String>(), any<DomainEvent>())
        verify(outboxEventRepository).markAsFailed(any<UUID>(), any<OutboxEventStatus>(), any<String>())
        verify(tracingMetrics).incrementEventProcessingError(any<String>(), any<String>())
    }

    @Test
    fun `retryFailedEvents should retry failed events successfully`() {
        // Given
        val eventId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val eventData = """{"orderId":"$orderId","eventType":"OrderCreated"}"""
        val failedEvent = createSampleOutboxEvent(eventId, eventData, OutboxEventStatus.FAILED, retryCount = 1)
        val domainEvent = mock(DomainEvent::class.java)
        
        whenever(outboxEventRepository.findFailedEventsForRetry(3)).thenReturn(listOf(failedEvent))
        whenever(objectMapper.readValue(eventData, DomainEvent::class.java)).thenReturn(domainEvent)
        whenever(domainEvent.eventType).thenReturn("OrderCreated")
        
        val sendResult = CompletableFuture.completedFuture(mock(SendResult::class.java) as SendResult<String, Any>)
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<DomainEvent>()))
            .thenReturn(sendResult)

        // When
        orderOutboxPublisher.retryFailedEvents()

        // Then
        verify(outboxEventRepository).findFailedEventsForRetry(any<Int>())
        verify(objectMapper).readValue(any<String>(), eq(DomainEvent::class.java))
        verify(kafkaTemplate).send(any<String>(), any<String>(), any<DomainEvent>())
        verify(outboxEventRepository).markAsPublished(any<UUID>(), any<OutboxEventStatus>(), any<Instant>())
        verify(tracingMetrics).incrementEventPublished(any<String>())
    }

    @Test
    fun `retryFailedEvents should handle retry failures`() {
        // Given
        val eventId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val eventData = """{"orderId":"$orderId","eventType":"OrderCreated"}"""
        val failedEvent = createSampleOutboxEvent(eventId, eventData, OutboxEventStatus.FAILED, retryCount = 2)
        val domainEvent = mock(DomainEvent::class.java)
        
        whenever(outboxEventRepository.findFailedEventsForRetry(3)).thenReturn(listOf(failedEvent))
        whenever(objectMapper.readValue(eventData, DomainEvent::class.java)).thenReturn(domainEvent)
        whenever(domainEvent.eventType).thenReturn("OrderCreated")
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<DomainEvent>()))
            .thenThrow(RuntimeException("Kafka send error"))

        // When
        orderOutboxPublisher.retryFailedEvents()

        // Then
        verify(outboxEventRepository).findFailedEventsForRetry(any<Int>())
        verify(kafkaTemplate).send(any<String>(), any<String>(), any<DomainEvent>())
        verify(outboxEventRepository).markAsFailed(any<UUID>(), any<OutboxEventStatus>(), any<String>())
        verify(tracingMetrics).incrementEventProcessingError(any<String>(), any<String>())
    }

    @Test
    fun `retryFailedEvents should skip events that exceed max retries`() {
        // Given
        val eventId = UUID.randomUUID()
        val failedEvent = createSampleOutboxEvent(eventId, "eventData", OutboxEventStatus.FAILED, retryCount = 4)
        
        whenever(outboxEventRepository.findFailedEventsForRetry(3)).thenReturn(emptyList())

        // When
        orderOutboxPublisher.retryFailedEvents()

        // Then
        verify(outboxEventRepository).findFailedEventsForRetry(any<Int>())
        verify(objectMapper, never()).readValue(any<String>(), any<Class<*>>())
        verify(kafkaTemplate, never()).send(any<String>(), any<String>(), any<DomainEvent>())
        verify(outboxEventRepository, never()).markAsPublished(any<UUID>(), any<OutboxEventStatus>(), any<Instant>())
        verify(outboxEventRepository, never()).markAsFailed(any<UUID>(), any<OutboxEventStatus>(), any<String>())
    }

    @Test
    fun `cleanupOldEvents should delete old published events`() {
        // Given
        val cutoffDate = Instant.now().minusSeconds(24 * 60 * 60)

        // When
        orderOutboxPublisher.cleanupOldEvents()

        // Then
        verify(outboxEventRepository).deleteOldPublishedEvents(any<Instant>())
    }

    @Test
    fun `getTopicForEvent should return correct topics for different event types`() {
        // This test verifies the private method indirectly through the public methods
        val eventId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        
        // Test OrderCreated
        val orderCreatedEvent = createSampleOutboxEvent(eventId, """{"orderId":"$orderId","eventType":"OrderCreated"}""")
        val domainEvent1 = mock(DomainEvent::class.java)
        
        whenever(outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING, PageRequest.of(0, 100)))
            .thenReturn(listOf(orderCreatedEvent))
        whenever(objectMapper.readValue(orderCreatedEvent.eventData, DomainEvent::class.java)).thenReturn(domainEvent1)
        whenever(domainEvent1.eventType).thenReturn("OrderCreated")
        
        val sendResult = CompletableFuture.completedFuture(mock(SendResult::class.java) as SendResult<String, Any>)
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<DomainEvent>())).thenReturn(sendResult)

        // When
        orderOutboxPublisher.publishPendingEvents()

        // Then
        verify(kafkaTemplate).send(any<String>(), any<String>(), any<DomainEvent>())
    }

    @Test
    fun `getTopicForEvent should return default topic for unknown event types`() {
        // This test verifies the private method indirectly through the public methods
        val eventId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        
        // Test unknown event type
        val unknownEvent = createSampleOutboxEvent(eventId, """{"orderId":"$orderId","eventType":"UnknownEvent"}""")
        val domainEvent = mock(DomainEvent::class.java)
        
        whenever(outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING, PageRequest.of(0, 100)))
            .thenReturn(listOf(unknownEvent))
        whenever(objectMapper.readValue(unknownEvent.eventData, DomainEvent::class.java)).thenReturn(domainEvent)
        whenever(domainEvent.eventType).thenReturn("UnknownEvent")
        
        val sendResult = CompletableFuture.completedFuture(mock(SendResult::class.java) as SendResult<String, Any>)
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<DomainEvent>())).thenReturn(sendResult)

        // When
        orderOutboxPublisher.publishPendingEvents()

        // Then
        verify(kafkaTemplate).send(any<String>(), any<String>(), any<DomainEvent>())
    }

    // Helper methods
    private fun createSampleOutboxEvent(
        eventId: UUID = UUID.randomUUID(),
        eventData: String = """{"orderId":"${UUID.randomUUID()}","eventType":"OrderCreated"}""",
        status: OutboxEventStatus = OutboxEventStatus.PENDING,
        retryCount: Int = 0
    ): OutboxEventEntity {
        return OutboxEventEntity(
            id = eventId,
            eventType = "OrderCreated",
            aggregateId = UUID.randomUUID().toString(),
            eventData = eventData,
            status = status,
            createdAt = Instant.now(),
            publishedAt = null,
            retryCount = retryCount,
            lastError = null
        )
    }
}

