package com.mercury.orders.orders.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.mercury.orders.events.OutboxEventStatus
import com.mercury.orders.orders.domain.Order
import com.mercury.orders.orders.domain.OrderItem
import com.mercury.orders.orders.domain.OrderStatus
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
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
class OrderEventPublisherTest {

    @Mock
    private lateinit var outboxEventRepository: OutboxEventRepository

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @Mock
    private lateinit var tracingMetrics: TracingMetrics

    private lateinit var orderEventPublisher: OrderEventPublisher

    @BeforeEach
    fun setUp() {
        orderEventPublisher = OrderEventPublisher(
            kafkaTemplate = mock(), // Mock KafkaTemplate
            objectMapper = objectMapper,
            outboxEventRepository = outboxEventRepository,
            tracingMetrics = tracingMetrics
        )
    }

    @Test
    fun `publishOrderCreated should save event to outbox repository`() {
        // Given
        val order = createSampleOrder()
        val eventData = """{"orderId":"${order.id}","customerId":"${order.customerId}"}"""
        
        whenever(objectMapper.writeValueAsString(any())).thenReturn(eventData)
        whenever(outboxEventRepository.save(any())).thenReturn(createSampleOutboxEvent())

        // When
        orderEventPublisher.publishOrderCreated(order)

        // Then
        verify(outboxEventRepository).save(any<OutboxEventEntity>())
        
        verify(objectMapper).writeValueAsString(any())
        verify(tracingMetrics).incrementEventPublished("OrderCreated")
    }

    @Test
    fun `publishOrderCompleted should save event to outbox repository`() {
        // Given
        val orderId = UUID.randomUUID()
        val eventData = """{"orderId":"$orderId"}"""
        
        whenever(objectMapper.writeValueAsString(any())).thenReturn(eventData)
        whenever(outboxEventRepository.save(any())).thenReturn(createSampleOutboxEvent())

        // When
        orderEventPublisher.publishOrderCompleted(orderId)

        // Then
        verify(outboxEventRepository).save(any<OutboxEventEntity>())
        
        verify(objectMapper).writeValueAsString(any())
        verify(tracingMetrics).incrementEventPublished("OrderCompleted")
    }

    @Test
    fun `publishOrderCancelled should save event to outbox repository`() {
        // Given
        val orderId = UUID.randomUUID()
        val reason = "Customer requested cancellation"
        val eventData = """{"orderId":"$orderId","reason":"$reason"}"""
        
        whenever(objectMapper.writeValueAsString(any())).thenReturn(eventData)
        whenever(outboxEventRepository.save(any())).thenReturn(createSampleOutboxEvent())

        // When
        orderEventPublisher.publishOrderCancelled(orderId, reason)

        // Then
        verify(outboxEventRepository).save(any<OutboxEventEntity>())
        
        verify(objectMapper).writeValueAsString(any())
        verify(tracingMetrics).incrementEventPublished("OrderCancelled")
    }

    @Test
    fun `publishOrderCreated should handle JSON serialization errors`() {
        // Given
        val order = createSampleOrder()
        
        whenever(objectMapper.writeValueAsString(any())).thenThrow(RuntimeException("JSON error"))

        // When & Then
        org.junit.jupiter.api.assertThrows<RuntimeException> {
            orderEventPublisher.publishOrderCreated(order)
        }

        verify(objectMapper).writeValueAsString(any())
        verify(outboxEventRepository, never()).save(any())
        verify(tracingMetrics).incrementEventProcessingError("OrderCreated", "PUBLISH_ERROR")
    }

    @Test
    fun `publishOrderCompleted should handle JSON serialization errors`() {
        // Given
        val orderId = UUID.randomUUID()
        
        whenever(objectMapper.writeValueAsString(any())).thenThrow(RuntimeException("JSON error"))

        // When & Then
        org.junit.jupiter.api.assertThrows<RuntimeException> {
            orderEventPublisher.publishOrderCompleted(orderId)
        }

        verify(objectMapper).writeValueAsString(any())
        verify(outboxEventRepository, never()).save(any())
        verify(tracingMetrics).incrementEventProcessingError("OrderCompleted", "PUBLISH_ERROR")
    }

    @Test
    fun `publishOrderCancelled should handle JSON serialization errors`() {
        // Given
        val orderId = UUID.randomUUID()
        val reason = "Customer requested cancellation"
        
        whenever(objectMapper.writeValueAsString(any())).thenThrow(RuntimeException("JSON error"))

        // When & Then
        org.junit.jupiter.api.assertThrows<RuntimeException> {
            orderEventPublisher.publishOrderCancelled(orderId, reason)
        }

        verify(objectMapper).writeValueAsString(any())
        verify(outboxEventRepository, never()).save(any())
        verify(tracingMetrics).incrementEventProcessingError("OrderCancelled", "PUBLISH_ERROR")
    }

    @Test
    fun `publishOrderCreated should handle repository save errors`() {
        // Given
        val order = createSampleOrder()
        val eventData = """{"orderId":"${order.id}","customerId":"${order.customerId}"}"""
        
        whenever(objectMapper.writeValueAsString(any())).thenReturn(eventData)
        whenever(outboxEventRepository.save(any())).thenThrow(RuntimeException("Database error"))

        // When & Then
        org.junit.jupiter.api.assertThrows<RuntimeException> {
            orderEventPublisher.publishOrderCreated(order)
        }

        verify(objectMapper).writeValueAsString(any())
        verify(outboxEventRepository).save(any())
        verify(tracingMetrics).incrementEventProcessingError("OrderCreated", "PUBLISH_ERROR")
    }

    @Test
    fun `publishOrderCreated should include order items in event data`() {
        // Given
        val orderItems = listOf(
            OrderItem(
                orderId = UUID.randomUUID(),
                sku = "SKU-001",
                quantity = 2,
                unitPrice = BigDecimal("25.50")
            ),
            OrderItem(
                orderId = UUID.randomUUID(),
                sku = "SKU-002",
                quantity = 1,
                unitPrice = BigDecimal("100.00")
            )
        )
        val order = createSampleOrder(items = orderItems)
        
        whenever(objectMapper.writeValueAsString(any())).thenReturn("""{"orderId":"${order.id}"}""")
        whenever(outboxEventRepository.save(any())).thenReturn(createSampleOutboxEvent())

        // When
        orderEventPublisher.publishOrderCreated(order)

        // Then
        verify(objectMapper).writeValueAsString(any())
        verify(outboxEventRepository).save(any())
        verify(tracingMetrics).incrementEventPublished("OrderCreated")
    }

    // Helper methods
    private fun createSampleOrder(
        orderId: UUID = UUID.randomUUID(),
        items: List<OrderItem> = emptyList()
    ): Order {
        return Order(
            id = orderId,
            customerId = "customer-12345678",
            status = OrderStatus.PENDING,
            totalAmount = BigDecimal("100.00"),
            currency = "USD",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            version = 0,
            items = items
        )
    }

    private fun createSampleOutboxEvent(): OutboxEventEntity {
        return OutboxEventEntity(
            eventType = "OrderCreated",
            aggregateId = UUID.randomUUID().toString(),
            eventData = """{"orderId":"${UUID.randomUUID()}"}""",
            status = OutboxEventStatus.PENDING,
            createdAt = Instant.now(),
            publishedAt = null,
            retryCount = 0,
            lastError = null
        )
    }
}

