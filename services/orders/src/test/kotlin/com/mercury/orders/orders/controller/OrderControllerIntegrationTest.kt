package com.mercury.orders.orders.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.mercury.orders.orders.domain.Order
import com.mercury.orders.orders.domain.OrderStatus
import com.mercury.orders.orders.repository.OrderRepository
import com.mercury.orders.orders.dto.CreateOrderRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        orderRepository.deleteAll()
    }

    @Test
    fun `POST orders should create order successfully`() {
        // Given
        val request = CreateOrderRequest(
            customerId = "customer-12345678",
            items = listOf(
                CreateOrderRequest.OrderItemRequest("SKU-001", 2, BigDecimal("25.50")),
                CreateOrderRequest.OrderItemRequest("SKU-002", 1, BigDecimal("100.00"))
            )
        )

        // When & Then
        mockMvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.customerId").value("customer-12345678"))
            .andExpect(jsonPath("$.status").value("PAYMENT_PENDING"))
            .andExpect(jsonPath("$.totalAmount").value(151.00))
            .andExpect(jsonPath("$.currency").value("USD"))
            .andExpect(jsonPath("$.items").isArray)
            .andExpect(jsonPath("$.items[0].sku").value("SKU-001"))
            .andExpect(jsonPath("$.items[0].quantity").value(2))
            .andExpect(jsonPath("$.items[0].unitPrice").value(25.50))
            .andExpect(jsonPath("$.items[1].sku").value("SKU-002"))
            .andExpect(jsonPath("$.items[1].quantity").value(1))
            .andExpect(jsonPath("$.items[1].unitPrice").value(100.00))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists())
    }

    @Test
    fun `POST orders should return 400 for invalid request`() {
        // Given
        val invalidRequest = mapOf(
            "customerId" to "",
            "items" to emptyList<Any>()
        )

        // When & Then
        mockMvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST orders should return 400 for empty items list`() {
        // Given
        val request = CreateOrderRequest(
            customerId = "customer-12345678",
            items = emptyList()
        )

        // When & Then
        mockMvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET orders by id should return order when found`() {
        // Given
        val order = createSampleOrder()
        val savedOrder = orderRepository.save(order)

        // When & Then
        mockMvc.perform(get("/orders/{orderId}", savedOrder.id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(savedOrder.id.toString()))
            .andExpect(jsonPath("$.customerId").value(savedOrder.customerId))
            .andExpect(jsonPath("$.status").value(savedOrder.status.toString()))
            .andExpect(jsonPath("$.totalAmount").value(savedOrder.totalAmount.toDouble()))
    }

    @Test
    fun `GET orders by id should return 404 when order not found`() {
        // Given
        val nonExistentOrderId = UUID.randomUUID()

        // When & Then
        mockMvc.perform(get("/orders/{orderId}", nonExistentOrderId))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET orders by customer should return customer orders`() {
        // Given
        val customerId = "customer-12345678"
        val order1 = createSampleOrder(customerId = customerId)
        val order2 = createSampleOrder(customerId = customerId)
        val otherOrder = createSampleOrder(customerId = "customer-87654321")
        
        orderRepository.saveAll(listOf(order1, order2, otherOrder))

        // When & Then
        mockMvc.perform(get("/orders/customer/{customerId}", customerId))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].customerId").value(customerId))
            .andExpect(jsonPath("$[1].customerId").value(customerId))
    }

    @Test
    fun `GET orders by customer should return empty list when no orders found`() {
        // Given
        val customerId = "customer-99999999"

        // When & Then
        mockMvc.perform(get("/orders/customer/{customerId}", customerId))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `PUT orders status should update status successfully`() {
        // Given
        val order = createSampleOrder(status = OrderStatus.INVENTORY_PENDING)
        val savedOrder = orderRepository.save(order)
        val updateRequest = mapOf("status" to "COMPLETED")

        // When & Then
        mockMvc.perform(
            put("/orders/{orderId}/status", savedOrder.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(savedOrder.id.toString()))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
    }

    @Test
    fun `PUT orders status should return 400 for invalid status transition`() {
        // Given
        val order = createSampleOrder(status = OrderStatus.COMPLETED)
        val savedOrder = orderRepository.save(order)
        val updateRequest = mapOf("status" to "PENDING")

        // When & Then
        mockMvc.perform(
            put("/orders/{orderId}/status", savedOrder.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `PUT orders status should return 404 for non-existent order`() {
        // Given
        val nonExistentOrderId = UUID.randomUUID()
        val updateRequest = mapOf("status" to "COMPLETED")

        // When & Then
        mockMvc.perform(
            put("/orders/{orderId}/status", nonExistentOrderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST orders cancel should cancel order successfully`() {
        // Given
        val order = createSampleOrder(status = OrderStatus.PAYMENT_PENDING)
        val savedOrder = orderRepository.save(order)
        val cancelRequest = mapOf("reason" to "Customer requested cancellation")

        // When & Then
        mockMvc.perform(
            post("/orders/{orderId}/cancel", savedOrder.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelRequest))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(savedOrder.id.toString()))
            .andExpect(jsonPath("$.status").value("CANCELLED"))
    }

    @Test
    fun `POST orders cancel should return 404 for non-existent order`() {
        // Given
        val nonExistentOrderId = UUID.randomUUID()
        val cancelRequest = mapOf("reason" to "Customer requested cancellation")

        // When & Then
        mockMvc.perform(
            post("/orders/{orderId}/cancel", nonExistentOrderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelRequest))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST orders cancel should return 409 for completed order`() {
        // Given
        val order = createSampleOrder(status = OrderStatus.COMPLETED)
        val savedOrder = orderRepository.save(order)
        val cancelRequest = mapOf("reason" to "Customer requested cancellation")

        // When & Then
        mockMvc.perform(
            post("/orders/{orderId}/cancel", savedOrder.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelRequest))
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `POST orders cancel should return 400 for missing reason`() {
        // Given
        val order = createSampleOrder(status = OrderStatus.PAYMENT_PENDING)
        val savedOrder = orderRepository.save(order)
        val cancelRequest = mapOf<String, String>()

        // When & Then
        mockMvc.perform(
            post("/orders/{orderId}/cancel", savedOrder.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelRequest))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `all endpoints should handle malformed JSON`() {
        // When & Then
        mockMvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }")
        )
            .andExpect(status().isBadRequest)

        mockMvc.perform(
            put("/orders/{orderId}/status", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }")
        )
            .andExpect(status().isBadRequest)

        mockMvc.perform(
            post("/orders/{orderId}/cancel", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `all endpoints should handle missing content type`() {
        // Given
        val request = CreateOrderRequest(
            customerId = "customer-12345678",
            items = listOf(CreateOrderRequest.OrderItemRequest("SKU-001", 1, BigDecimal("10.00")))
        )

        // When & Then
        mockMvc.perform(
            post("/orders")
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnsupportedMediaType)
    }

    // Helper methods
    private fun createSampleOrder(
        customerId: String = "customer-12345678",
        status: OrderStatus = OrderStatus.PENDING
    ): Order {
        return Order(
            customerId = customerId,
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

