package com.mercury.orders.orders.controller

import com.mercury.orders.orders.domain.Order
import com.mercury.orders.orders.domain.OrderStatus
import com.mercury.orders.orders.service.CreateOrderRequest
import com.mercury.orders.orders.service.OrderService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderService: OrderService
) {

    @PostMapping
    fun createOrder(@Valid @RequestBody request: CreateOrderRequest): ResponseEntity<Order> {
        val order = orderService.createOrder(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(order)
    }

    @GetMapping("/{orderId}")
    fun getOrder(@PathVariable orderId: UUID): ResponseEntity<Order> {
        val order = orderService.getOrder(orderId)
        return if (order != null) {
            ResponseEntity.ok(order)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/customer/{customerId}")
    fun getOrdersByCustomer(@PathVariable customerId: String): ResponseEntity<List<Order>> {
        val orders = orderService.getOrdersByCustomer(customerId)
        return ResponseEntity.ok(orders)
    }

    @PutMapping("/{orderId}/status")
    fun updateOrderStatus(
        @PathVariable orderId: UUID,
        @RequestBody request: UpdateOrderStatusRequest
    ): ResponseEntity<Order> {
        return try {
            val order = orderService.updateOrderStatus(orderId, request.status)
            ResponseEntity.ok(order)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    @PostMapping("/{orderId}/cancel")
    fun cancelOrder(
        @PathVariable orderId: UUID,
        @RequestBody request: CancelOrderRequest
    ): ResponseEntity<Order> {
        return try {
            val order = orderService.cancelOrder(orderId, request.reason)
            ResponseEntity.ok(order)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }
}

data class UpdateOrderStatusRequest(
    val status: OrderStatus
)

data class CancelOrderRequest(
    val reason: String
)


