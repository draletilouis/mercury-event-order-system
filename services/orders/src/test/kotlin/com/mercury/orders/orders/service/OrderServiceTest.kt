package com.mercury.orders.orders.service

import com.mercury.orders.orders.domain.Order
import com.mercury.orders.orders.domain.OrderItem
import com.mercury.orders.orders.domain.OrderStatus
import com.mercury.orders.orders.dto.CreateOrderRequest
import com.mercury.orders.orders.repository.OrderRepository
import com.mercury.orders.tracing.TracingMetrics
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.Mockito.times
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
class OrderServiceTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var orderEventPublisher: OrderEventPublisher

    @Mock
    private lateinit var tracingMetrics: TracingMetrics

    private lateinit var orderService: OrderService

    @BeforeEach
    fun setUp() {
        orderService = OrderService(orderRepository, orderEventPublisher, tracingMetrics)
    }

    @Test
    fun `createOrder should create order with correct total amount and items`() {
        // Given
        val customerId = "customer-12345678"
        val items = listOf(
            CreateOrderRequest.OrderItemRequest("SKU-001", 2, BigDecimal("25.50")),
            CreateOrderRequest.OrderItemRequest("SKU-002", 1, BigDecimal("100.00"))
        )
        val request = CreateOrderRequest(customerId, items)
        
        val expectedOrder = createSampleOrder(customerId, items)
        whenever(orderRepository.save(any())).thenReturn(expectedOrder)

        // When
        val result = orderService.createOrder(request)

        // Then
        verify(orderRepository, times(2)).save(any()) // Once for initial save, once for status update
        verify(orderEventPublisher).publishOrderCreated(any())
        verify(tracingMetrics).incrementOrderCreated()
        
        // Verify total amount calculation
        val expectedTotal = BigDecimal("25.50").multiply(BigDecimal.valueOf(2)) + BigDecimal("100.00")
        assert(result.totalAmount == expectedTotal)
    }

    @Test
    fun `createOrder should transition order to PAYMENT_PENDING status`() {
        // Given
        val customerId = "customer-12345678"
        val items = listOf(CreateOrderRequest.OrderItemRequest("SKU-001", 1, BigDecimal("50.00")))
        val request = CreateOrderRequest(customerId, items)
        
        val initialOrder = createSampleOrder(customerId, items, OrderStatus.PENDING)
        whenever(orderRepository.save(any())).thenReturn(initialOrder)

        // When
        val result = orderService.createOrder(request)

        // Then
        val statusUpdateCaptor = ArgumentCaptor.forClass(Order::class.java)
        verify(orderRepository, times(2)).save(statusUpdateCaptor.capture())
        assert(statusUpdateCaptor.allValues.last().status == OrderStatus.PAYMENT_PENDING)
    }

    @Test
    fun `getOrder should return order when found`() {
        // Given
        val orderId = UUID.randomUUID()
        val expectedOrder = createSampleOrder("customer-12345678", emptyList())
        whenever(orderRepository.findById(orderId)).thenReturn(Optional.of(expectedOrder))

        // When
        val result = orderService.getOrder(orderId)

        // Then
        assert(result == expectedOrder)
        verify(orderRepository).findById(orderId)
    }

    @Test
    fun `getOrder should return null when order not found`() {
        // Given
        val orderId = UUID.randomUUID()
        whenever(orderRepository.findById(orderId)).thenReturn(Optional.empty())

        // When
        val result = orderService.getOrder(orderId)

        // Then
        assert(result == null)
        verify(orderRepository).findById(orderId)
    }

    @Test
    fun `getOrdersByCustomer should return customer orders`() {
        // Given
        val customerId = "customer-12345678"
        val expectedOrders = listOf(
            createSampleOrder(customerId, emptyList()),
            createSampleOrder(customerId, emptyList())
        )
        whenever(orderRepository.findByCustomerId(customerId)).thenReturn(expectedOrders)

        // When
        val result = orderService.getOrdersByCustomer(customerId)

        // Then
        assert(result == expectedOrders)
        verify(orderRepository).findByCustomerId(customerId)
    }

    @Test
    fun `updateOrderStatus should update status and publish events for COMPLETED`() {
        // Given
        val orderId = UUID.randomUUID()
        val existingOrder = createSampleOrder("customer-12345678", emptyList(), OrderStatus.INVENTORY_PENDING)
        val updatedOrder = existingOrder.copy(status = OrderStatus.COMPLETED)
        
        whenever(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder))
        whenever(orderRepository.save(any())).thenReturn(updatedOrder)

        // When
        val result = orderService.updateOrderStatus(orderId, OrderStatus.COMPLETED)

        // Then
        verify(orderRepository).findById(orderId)
        verify(orderRepository).save(any())
        verify(orderEventPublisher).publishOrderCompleted(orderId)
        verify(tracingMetrics).incrementOrderCompleted()
        assert(result.status == OrderStatus.COMPLETED)
    }

    @Test
    fun `updateOrderStatus should update status and publish events for CANCELLED`() {
        // Given
        val orderId = UUID.randomUUID()
        val existingOrder = createSampleOrder("customer-12345678", emptyList(), OrderStatus.PAYMENT_PENDING)
        val updatedOrder = existingOrder.copy(status = OrderStatus.CANCELLED)
        
        whenever(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder))
        whenever(orderRepository.save(any())).thenReturn(updatedOrder)

        // When
        val result = orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED)

        // Then
        verify(orderRepository).findById(orderId)
        verify(orderRepository).save(any())
        verify(orderEventPublisher).publishOrderCancelled(orderId, "Order cancelled")
        verify(tracingMetrics).incrementOrderCancelled()
        assert(result.status == OrderStatus.CANCELLED)
    }

    @Test
    fun `updateOrderStatus should not publish events for other status transitions`() {
        // Given
        val orderId = UUID.randomUUID()
        val existingOrder = createSampleOrder("customer-12345678", emptyList(), OrderStatus.PENDING)
        val updatedOrder = existingOrder.copy(status = OrderStatus.PAYMENT_PENDING)
        
        whenever(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder))
        whenever(orderRepository.save(any())).thenReturn(updatedOrder)

        // When
        val result = orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_PENDING)

        // Then
        verify(orderRepository).findById(orderId)
        verify(orderRepository).save(any())
        verify(orderEventPublisher, never()).publishOrderCompleted(any())
        verify(orderEventPublisher, never()).publishOrderCancelled(any(), any())
        verify(tracingMetrics, never()).incrementOrderCompleted()
        verify(tracingMetrics, never()).incrementOrderCancelled()
        assert(result.status == OrderStatus.PAYMENT_PENDING)
    }

    @Test
    fun `updateOrderStatus should throw exception when order not found`() {
        // Given
        val orderId = UUID.randomUUID()
        whenever(orderRepository.findById(orderId)).thenReturn(Optional.empty())

        // When & Then
        assertThrows<IllegalArgumentException> {
            orderService.updateOrderStatus(orderId, OrderStatus.COMPLETED)
        }
        verify(orderRepository).findById(orderId)
        verify(orderRepository, never()).save(any())
    }

    @Test
    fun `updateOrderStatus should throw exception for invalid status transition`() {
        // Given
        val orderId = UUID.randomUUID()
        val existingOrder = createSampleOrder("customer-12345678", emptyList(), OrderStatus.COMPLETED)
        whenever(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder))

        // When & Then
        assertThrows<IllegalStateException> {
            orderService.updateOrderStatus(orderId, OrderStatus.PENDING)
        }
        verify(orderRepository).findById(orderId)
        verify(orderRepository, never()).save(any())
    }

    @Test
    fun `cancelOrder should cancel order and publish event`() {
        // Given
        val orderId = UUID.randomUUID()
        val reason = "Customer requested cancellation"
        val existingOrder = createSampleOrder("customer-12345678", emptyList(), OrderStatus.PAYMENT_PENDING)
        val cancelledOrder = existingOrder.copy(status = OrderStatus.CANCELLED)
        
        whenever(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder))
        whenever(orderRepository.save(any())).thenReturn(cancelledOrder)

        // When
        val result = orderService.cancelOrder(orderId, reason)

        // Then
        verify(orderRepository).findById(orderId)
        verify(orderRepository).save(any())
        verify(orderEventPublisher).publishOrderCancelled(orderId, reason)
        verify(tracingMetrics).incrementOrderCancelled()
        assert(result.status == OrderStatus.CANCELLED)
    }

    @Test
    fun `cancelOrder should throw exception when order not found`() {
        // Given
        val orderId = UUID.randomUUID()
        val reason = "Customer requested cancellation"
        whenever(orderRepository.findById(orderId)).thenReturn(Optional.empty())

        // When & Then
        assertThrows<IllegalArgumentException> {
            orderService.cancelOrder(orderId, reason)
        }
        verify(orderRepository).findById(orderId)
        verify(orderRepository, never()).save(any())
    }

    @Test
    fun `cancelOrder should throw exception when trying to cancel completed order`() {
        // Given
        val orderId = UUID.randomUUID()
        val reason = "Customer requested cancellation"
        val existingOrder = createSampleOrder("customer-12345678", emptyList(), OrderStatus.COMPLETED)
        whenever(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder))

        // When & Then
        assertThrows<IllegalStateException> {
            orderService.cancelOrder(orderId, reason)
        }
        verify(orderRepository).findById(orderId)
        verify(orderRepository, never()).save(any())
    }

    @Test
    fun `calculateTotalAmount should handle zero quantity items`() {
        // Given
        val customerId = "customer-12345678"
        val items = listOf(
            CreateOrderRequest.OrderItemRequest("SKU-001", 0, BigDecimal("25.50"))
        )
        val request = CreateOrderRequest(customerId, items)
        
        val expectedOrder = createSampleOrder(customerId, items, OrderStatus.PENDING)
        whenever(orderRepository.save(any())).thenReturn(expectedOrder)

        // When
        val result = orderService.createOrder(request)

        // Then
        assert(result.totalAmount.compareTo(BigDecimal.ZERO) == 0)
    }

    @Test
    fun `calculateTotalAmount should handle large quantities`() {
        // Given
        val customerId = "customer-12345678"
        val items = listOf(
            CreateOrderRequest.OrderItemRequest("SKU-001", 1000, BigDecimal("0.01"))
        )
        val request = CreateOrderRequest(customerId, items)
        
        val expectedOrder = createSampleOrder(customerId, items, OrderStatus.PENDING)
        whenever(orderRepository.save(any())).thenReturn(expectedOrder)

        // When
        val result = orderService.createOrder(request)

        // Then
        val expectedTotal = BigDecimal("0.01").multiply(BigDecimal.valueOf(1000))
        assert(result.totalAmount == expectedTotal)
    }

    // Helper methods
    private fun createSampleOrder(
        customerId: String,
        items: List<CreateOrderRequest.OrderItemRequest>,
        status: OrderStatus = OrderStatus.PENDING
    ): Order {
        val totalAmount = items.sumOf { item ->
            item.unitPrice.multiply(BigDecimal.valueOf(item.quantity.toLong()))
        }
        
        val orderItems = items.map { item ->
            OrderItem(
                orderId = UUID.randomUUID(),
                sku = item.sku,
                quantity = item.quantity,
                unitPrice = item.unitPrice
            )
        }
        
        return Order(
            customerId = customerId,
            status = status,
            totalAmount = totalAmount,
            currency = "USD",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            version = 0,
            items = orderItems
        )
    }
}

