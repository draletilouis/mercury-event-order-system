package com.mercury.orders.orders.service

import com.mercury.orders.orders.domain.Order
import com.mercury.orders.orders.domain.OrderItem
import com.mercury.orders.orders.domain.OrderStatus
import com.mercury.orders.orders.repository.OrderRepository
import com.mercury.orders.tracing.TracingMetrics
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

@Service
@Transactional
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderEventPublisher: OrderEventPublisher,
    private val tracingMetrics: TracingMetrics
) {

    fun createOrder(request: CreateOrderRequest): Order {
        val order = Order(
            customerId = request.customerId,
            totalAmount = calculateTotalAmount(request.items),
            items = request.items.map { item ->
                OrderItem(
                    orderId = UUID.randomUUID(), // Will be updated after order creation
                    sku = item.sku,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice
                )
            }
        )
        
        val savedOrder = orderRepository.save(order)
        
        // Update order items with correct order ID
        val updatedItems = savedOrder.items.map { item ->
            item.copy(orderId = savedOrder.id)
        }
        val finalOrder = savedOrder.copy(items = updatedItems)
        
        // Publish OrderCreated event
        orderEventPublisher.publishOrderCreated(finalOrder)
        
        // Transition to PAYMENT_PENDING status
        val orderWithStatus = finalOrder.transitionTo(OrderStatus.PAYMENT_PENDING)
        val finalSavedOrder = orderRepository.save(orderWithStatus)
        
        tracingMetrics.incrementOrderCreated()
        
        return finalSavedOrder
    }

    fun getOrder(orderId: UUID): Order? {
        return orderRepository.findById(orderId).orElse(null)
    }

    fun getOrdersByCustomer(customerId: String): List<Order> {
        return orderRepository.findByCustomerId(customerId)
    }

    fun updateOrderStatus(orderId: UUID, newStatus: OrderStatus): Order {
        val order = orderRepository.findById(orderId)
            .orElseThrow { IllegalArgumentException("Order not found: $orderId") }
        
        val updatedOrder = order.transitionTo(newStatus)
        val savedOrder = orderRepository.save(updatedOrder)
        
        // Publish appropriate events based on status change
        when (newStatus) {
            OrderStatus.COMPLETED -> {
                orderEventPublisher.publishOrderCompleted(orderId)
                tracingMetrics.incrementOrderCompleted()
            }
            OrderStatus.CANCELLED -> {
                orderEventPublisher.publishOrderCancelled(orderId, "Order cancelled")
                tracingMetrics.incrementOrderCancelled()
            }
            else -> { /* No event needed for other transitions */ }
        }
        
        return savedOrder
    }

    fun cancelOrder(orderId: UUID, reason: String): Order {
        val order = orderRepository.findById(orderId)
            .orElseThrow { IllegalArgumentException("Order not found: $orderId") }
        
        if (order.status == OrderStatus.COMPLETED) {
            throw IllegalStateException("Cannot cancel a completed order")
        }
        
        val cancelledOrder = order.transitionTo(OrderStatus.CANCELLED)
        val savedOrder = orderRepository.save(cancelledOrder)
        
        orderEventPublisher.publishOrderCancelled(orderId, reason)
        tracingMetrics.incrementOrderCancelled()
        
        return savedOrder
    }

    private fun calculateTotalAmount(items: List<CreateOrderRequest.OrderItemRequest>): BigDecimal {
        return items.sumOf { item ->
            item.unitPrice.multiply(BigDecimal.valueOf(item.quantity.toLong()))
        }
    }
}

data class CreateOrderRequest(
    val customerId: String,
    val items: List<OrderItemRequest>
) {
    data class OrderItemRequest(
        val sku: String,
        val quantity: Int,
        val unitPrice: BigDecimal
    )
}


