package com.mercury.orders.orders.eventhandler

import com.mercury.orders.events.InventoryInsufficientEvent
import com.mercury.orders.events.InventoryReservedEvent
import com.mercury.orders.orders.domain.Order
import com.mercury.orders.orders.domain.OrderStatus
import com.mercury.orders.orders.repository.OrderRepository
import com.mercury.orders.orders.service.OrderService
import com.mercury.orders.tracing.TracingMetrics
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
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
class InventoryEventHandlerTest {

    @Mock
    private lateinit var orderService: OrderService

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var tracingMetrics: TracingMetrics

    private lateinit var inventoryEventHandler: InventoryEventHandler

    @BeforeEach
    fun setUp() {
        inventoryEventHandler = InventoryEventHandler(orderService, tracingMetrics)
    }

    @Test
    fun `handleInventoryReserved should complete order when order exists and is INVENTORY_PENDING`() {
        // Given
        val orderId = UUID.randomUUID()
        val event = InventoryReservedEvent(
            orderId = orderId,
            reservedItems = listOf(
                InventoryReservedEvent.ReservedItem(
                    sku = "TEST-SKU",
                    quantity = 1,
                    reservationId = "reservation-123"
                )
            )
        )
        val existingOrder = createSampleOrder(orderId, OrderStatus.INVENTORY_PENDING)
        
        whenever(orderService.getOrder(orderId)).thenReturn(existingOrder)

        // When
        inventoryEventHandler.handleInventoryReserved(event)

        // Then
        verify(orderService).getOrder(orderId)
        verify(orderService).updateOrderStatus(orderId, OrderStatus.COMPLETED)
        verify(tracingMetrics).incrementEventConsumed("InventoryReserved")
    }

    @Test
    fun `handleInventoryReserved should not complete order when order is not INVENTORY_PENDING`() {
        // Given
        val orderId = UUID.randomUUID()
        val event = InventoryReservedEvent(
            orderId = orderId,
            reservedItems = listOf(
                InventoryReservedEvent.ReservedItem(
                    sku = "TEST-SKU",
                    quantity = 1,
                    reservationId = "reservation-123"
                )
            )
        )
        val existingOrder = createSampleOrder(orderId, OrderStatus.COMPLETED)
        
        whenever(orderService.getOrder(orderId)).thenReturn(existingOrder)

        // When
        inventoryEventHandler.handleInventoryReserved(event)

        // Then
        verify(orderService).getOrder(orderId)
        verify(orderService, never()).updateOrderStatus(any(), any())
        verify(tracingMetrics).incrementEventConsumed("InventoryReserved")
    }

    @Test
    fun `handleInventoryReserved should not complete order when order does not exist`() {
        // Given
        val orderId = UUID.randomUUID()
        val event = InventoryReservedEvent(
            orderId = orderId,
            reservedItems = listOf(
                InventoryReservedEvent.ReservedItem(
                    sku = "TEST-SKU",
                    quantity = 1,
                    reservationId = "reservation-123"
                )
            )
        )
        
        whenever(orderService.getOrder(orderId)).thenReturn(null)

        // When
        inventoryEventHandler.handleInventoryReserved(event)

        // Then
        verify(orderService).getOrder(orderId)
        verify(orderService, never()).updateOrderStatus(any(), any())
        verify(tracingMetrics).incrementEventConsumed("InventoryReserved")
    }

    @Test
    fun `handleInventoryReserved should handle exceptions and increment error metrics`() {
        // Given
        val orderId = UUID.randomUUID()
        val event = InventoryReservedEvent(
            orderId = orderId,
            reservedItems = listOf(
                InventoryReservedEvent.ReservedItem(
                    sku = "TEST-SKU",
                    quantity = 1,
                    reservationId = "reservation-123"
                )
            )
        )
        
        whenever(orderService.getOrder(orderId)).thenThrow(RuntimeException("Database error"))

        // When & Then
        org.junit.jupiter.api.assertThrows<RuntimeException> {
            inventoryEventHandler.handleInventoryReserved(event)
        }

        verify(orderService).getOrder(orderId)
        verify(orderService, never()).updateOrderStatus(any(), any())
        verify(tracingMetrics).incrementEventProcessingError("InventoryReserved", "PROCESSING_ERROR")
    }

    @Test
    fun `handleInventoryInsufficient should cancel order when order exists and is INVENTORY_PENDING`() {
        // Given
        val orderId = UUID.randomUUID()
        val insufficientItems = listOf(
            InventoryInsufficientEvent.InsufficientItem("SKU-001", 10, 5),
            InventoryInsufficientEvent.InsufficientItem("SKU-002", 3, 1)
        )
        val event = InventoryInsufficientEvent(orderId = orderId, insufficientItems = insufficientItems)
        val existingOrder = createSampleOrder(orderId, OrderStatus.INVENTORY_PENDING)
        
        whenever(orderService.getOrder(orderId)).thenReturn(existingOrder)

        // When
        inventoryEventHandler.handleInventoryInsufficient(event)

        // Then
        verify(orderService).getOrder(orderId)
        verify(orderService).cancelOrder(
            orderId, 
            "Insufficient inventory: SKU-001 (requested: 10, available: 5), SKU-002 (requested: 3, available: 1)"
        )
        verify(tracingMetrics).incrementEventConsumed("InventoryInsufficient")
    }

    @Test
    fun `handleInventoryInsufficient should not cancel order when order is not INVENTORY_PENDING`() {
        // Given
        val orderId = UUID.randomUUID()
        val insufficientItems = listOf(
            InventoryInsufficientEvent.InsufficientItem("SKU-001", 10, 5)
        )
        val event = InventoryInsufficientEvent(orderId = orderId, insufficientItems = insufficientItems)
        val existingOrder = createSampleOrder(orderId, OrderStatus.COMPLETED)
        
        whenever(orderService.getOrder(orderId)).thenReturn(existingOrder)

        // When
        inventoryEventHandler.handleInventoryInsufficient(event)

        // Then
        verify(orderService).getOrder(orderId)
        verify(orderService, never()).cancelOrder(any(), any())
        verify(tracingMetrics).incrementEventConsumed("InventoryInsufficient")
    }

    @Test
    fun `handleInventoryInsufficient should not cancel order when order does not exist`() {
        // Given
        val orderId = UUID.randomUUID()
        val insufficientItems = listOf(
            InventoryInsufficientEvent.InsufficientItem("SKU-001", 10, 5)
        )
        val event = InventoryInsufficientEvent(orderId = orderId, insufficientItems = insufficientItems)
        
        whenever(orderService.getOrder(orderId)).thenReturn(null)

        // When
        inventoryEventHandler.handleInventoryInsufficient(event)

        // Then
        verify(orderService).getOrder(orderId)
        verify(orderService, never()).cancelOrder(any(), any())
        verify(tracingMetrics).incrementEventConsumed("InventoryInsufficient")
    }

    @Test
    fun `handleInventoryInsufficient should handle exceptions and increment error metrics`() {
        // Given
        val orderId = UUID.randomUUID()
        val insufficientItems = listOf(
            InventoryInsufficientEvent.InsufficientItem("SKU-001", 10, 5)
        )
        val event = InventoryInsufficientEvent(orderId = orderId, insufficientItems = insufficientItems)
        
        whenever(orderService.getOrder(orderId)).thenThrow(RuntimeException("Database error"))

        // When & Then
        org.junit.jupiter.api.assertThrows<RuntimeException> {
            inventoryEventHandler.handleInventoryInsufficient(event)
        }

        verify(orderService).getOrder(orderId)
        verify(orderService, never()).cancelOrder(any(), any())
        verify(tracingMetrics).incrementEventProcessingError("InventoryInsufficient", "PROCESSING_ERROR")
    }

    @Test
    fun `handleInventoryInsufficient should handle empty insufficient items list`() {
        // Given
        val orderId = UUID.randomUUID()
        val event = InventoryInsufficientEvent(orderId = orderId, insufficientItems = emptyList())
        val existingOrder = createSampleOrder(orderId, OrderStatus.INVENTORY_PENDING)
        
        whenever(orderService.getOrder(orderId)).thenReturn(existingOrder)

        // When
        inventoryEventHandler.handleInventoryInsufficient(event)

        // Then
        verify(orderService).getOrder(orderId)
        verify(orderService).cancelOrder(orderId, "Insufficient inventory: ")
        verify(tracingMetrics).incrementEventConsumed("InventoryInsufficient")
    }

    @Test
    fun `handleInventoryInsufficient should handle single insufficient item`() {
        // Given
        val orderId = UUID.randomUUID()
        val insufficientItems = listOf(
            InventoryInsufficientEvent.InsufficientItem("SKU-001", 10, 5)
        )
        val event = InventoryInsufficientEvent(orderId = orderId, insufficientItems = insufficientItems)
        val existingOrder = createSampleOrder(orderId, OrderStatus.INVENTORY_PENDING)
        
        whenever(orderService.getOrder(orderId)).thenReturn(existingOrder)

        // When
        inventoryEventHandler.handleInventoryInsufficient(event)

        // Then
        verify(orderService).getOrder(orderId)
        verify(orderService).cancelOrder(orderId, "Insufficient inventory: SKU-001 (requested: 10, available: 5)")
        verify(tracingMetrics).incrementEventConsumed("InventoryInsufficient")
    }

    // Helper methods
    private fun createSampleOrder(
        orderId: UUID = UUID.randomUUID(),
        status: OrderStatus = OrderStatus.PENDING
    ): Order {
        return Order(
            id = orderId,
            customerId = "customer-12345678",
            status = status,
            totalAmount = BigDecimal("100.00"),
            currency = "USD",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            version = 0,
            items = emptyList()
        )
    }
}

