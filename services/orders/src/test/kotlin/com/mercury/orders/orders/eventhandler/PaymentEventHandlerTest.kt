package com.mercury.orders.orders.eventhandler

import com.mercury.orders.events.PaymentAuthorizedEvent
import com.mercury.orders.events.PaymentDeclinedEvent
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
class PaymentEventHandlerTest {

    @Mock
    private lateinit var orderService: OrderService

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var tracingMetrics: TracingMetrics

    private lateinit var paymentEventHandler: PaymentEventHandler

    @BeforeEach
    fun setUp() {
        paymentEventHandler = PaymentEventHandler(orderService, tracingMetrics)
    }

    @Test
    fun `handlePaymentAuthorized should transition order to INVENTORY_PENDING when order exists and is PAYMENT_PENDING`() {
        // Given
        val orderId = UUID.randomUUID()
        val event = PaymentAuthorizedEvent(
            orderId = orderId,
            paymentId = "payment-123",
            authorizedAmount = BigDecimal("100.00")
        )
        val existingOrder = createSampleOrder(orderId, OrderStatus.PAYMENT_PENDING)
        
        whenever(orderService.getOrder(orderId)).thenReturn(existingOrder)

        // When
        paymentEventHandler.handlePaymentAuthorized(event)

        // Then
        verify(orderService).getOrder(orderId)
        verify(orderService).updateOrderStatus(orderId, OrderStatus.INVENTORY_PENDING)
        verify(tracingMetrics).incrementEventConsumed("PaymentAuthorized")
    }

    @Test
    fun `handlePaymentAuthorized should not transition order when order is not PAYMENT_PENDING`() {
        // Given
        val orderId = UUID.randomUUID()
        val event = PaymentAuthorizedEvent(
            orderId = orderId,
            paymentId = "payment-123",
            authorizedAmount = BigDecimal("100.00")
        )
        val existingOrder = createSampleOrder(orderId, OrderStatus.COMPLETED)
        
        whenever(orderService.getOrder(orderId)).thenReturn(existingOrder)

        // When
        paymentEventHandler.handlePaymentAuthorized(event)

        // Then
        verify(orderService).getOrder(orderId)
        verify(orderService, never()).updateOrderStatus(any(), any())
        verify(tracingMetrics).incrementEventConsumed("PaymentAuthorized")
    }

    @Test
    fun `handlePaymentAuthorized should not transition order when order does not exist`() {
        // Given
        val orderId = UUID.randomUUID()
        val event = PaymentAuthorizedEvent(
            orderId = orderId,
            paymentId = "payment-123",
            authorizedAmount = BigDecimal("100.00")
        )
        
        whenever(orderService.getOrder(orderId)).thenReturn(null)

        // When
        paymentEventHandler.handlePaymentAuthorized(event)

        // Then
        verify(orderService).getOrder(orderId)
        verify(orderService, never()).updateOrderStatus(any(), any())
        verify(tracingMetrics).incrementEventConsumed("PaymentAuthorized")
    }

    @Test
    fun `handlePaymentAuthorized should handle exceptions and increment error metrics`() {
        // Given
        val orderId = UUID.randomUUID()
        val event = PaymentAuthorizedEvent(
            orderId = orderId,
            paymentId = "payment-123",
            authorizedAmount = BigDecimal("100.00")
        )
        
        whenever(orderService.getOrder(orderId)).thenThrow(RuntimeException("Database error"))

        // When & Then
        org.junit.jupiter.api.assertThrows<RuntimeException> {
            paymentEventHandler.handlePaymentAuthorized(event)
        }

        verify(orderService).getOrder(orderId)
        verify(orderService, never()).updateOrderStatus(any(), any())
        verify(tracingMetrics).incrementEventProcessingError("PaymentAuthorized", "PROCESSING_ERROR")
    }

    @Test
    fun `handlePaymentDeclined should cancel order when order exists and is PAYMENT_PENDING`() {
        // Given
        val orderId = UUID.randomUUID()
        val reason = "Insufficient funds"
        val event = PaymentDeclinedEvent(
            orderId = orderId,
            paymentId = "payment-123",
            reason = reason,
            declinedAmount = BigDecimal("100.00")
        )
        val existingOrder = createSampleOrder(orderId, OrderStatus.PAYMENT_PENDING)
        
        whenever(orderService.getOrder(orderId)).thenReturn(existingOrder)

        // When
        paymentEventHandler.handlePaymentDeclined(event)

        // Then
        verify(orderService).getOrder(orderId)
        verify(orderService).cancelOrder(orderId, "Payment declined: $reason")
        verify(tracingMetrics).incrementEventConsumed("PaymentDeclined")
    }

    @Test
    fun `handlePaymentDeclined should not cancel order when order is not PAYMENT_PENDING`() {
        // Given
        val orderId = UUID.randomUUID()
        val reason = "Insufficient funds"
        val event = PaymentDeclinedEvent(
            orderId = orderId,
            paymentId = "payment-123",
            reason = reason,
            declinedAmount = BigDecimal("100.00")
        )
        val existingOrder = createSampleOrder(orderId, OrderStatus.COMPLETED)
        
        whenever(orderService.getOrder(orderId)).thenReturn(existingOrder)

        // When
        paymentEventHandler.handlePaymentDeclined(event)

        // Then
        verify(orderService).getOrder(orderId)
        verify(orderService, never()).cancelOrder(any(), any())
        verify(tracingMetrics).incrementEventConsumed("PaymentDeclined")
    }

    @Test
    fun `handlePaymentDeclined should not cancel order when order does not exist`() {
        // Given
        val orderId = UUID.randomUUID()
        val reason = "Insufficient funds"
        val event = PaymentDeclinedEvent(
            orderId = orderId,
            paymentId = "payment-123",
            reason = reason,
            declinedAmount = BigDecimal("100.00")
        )
        
        whenever(orderService.getOrder(orderId)).thenReturn(null)

        // When
        paymentEventHandler.handlePaymentDeclined(event)

        // Then
        verify(orderService).getOrder(orderId)
        verify(orderService, never()).cancelOrder(any(), any())
        verify(tracingMetrics).incrementEventConsumed("PaymentDeclined")
    }

    @Test
    fun `handlePaymentDeclined should handle exceptions and increment error metrics`() {
        // Given
        val orderId = UUID.randomUUID()
        val reason = "Insufficient funds"
        val event = PaymentDeclinedEvent(
            orderId = orderId,
            paymentId = "payment-123",
            reason = reason,
            declinedAmount = BigDecimal("100.00")
        )
        
        whenever(orderService.getOrder(orderId)).thenThrow(RuntimeException("Database error"))

        // When & Then
        org.junit.jupiter.api.assertThrows<RuntimeException> {
            paymentEventHandler.handlePaymentDeclined(event)
        }

        verify(orderService).getOrder(orderId)
        verify(orderService, never()).cancelOrder(any(), any())
        verify(tracingMetrics).incrementEventProcessingError("PaymentDeclined", "PROCESSING_ERROR")
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

