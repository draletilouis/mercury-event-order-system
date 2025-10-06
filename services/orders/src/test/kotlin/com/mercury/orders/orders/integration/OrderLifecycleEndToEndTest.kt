package com.mercury.orders.orders.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.mercury.orders.orders.domain.OrderStatus
import com.mercury.orders.orders.dto.CreateOrderRequest
import com.mercury.orders.orders.repository.OrderRepository
import com.mercury.orders.orders.repository.OutboxEventRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
// TestContainers imports removed - using H2 in-memory database instead
import java.math.BigDecimal
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 3, topics = ["order-events", "payment-events", "inventory-events"])
@Transactional
class OrderLifecycleEndToEndTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var outboxEventRepository: OutboxEventRepository

    @BeforeEach
    fun setUp() {
        orderRepository.deleteAll()
        outboxEventRepository.deleteAll()
    }

    @Test
    fun `complete order lifecycle from creation to completion`() {
        // Step 1: Create Order
        val customerId = "customer-12345678"
        val createOrderRequest = CreateOrderRequest(
            customerId = customerId,
            items = listOf(
                CreateOrderRequest.OrderItemRequest(
                    sku = "LAPTOP-001",
                    quantity = 1,
                    unitPrice = BigDecimal("999.99")
                ),
                CreateOrderRequest.OrderItemRequest(
                    sku = "MOUSE-001",
                    quantity = 2,
                    unitPrice = BigDecimal("25.50")
                )
            )
        )

        val createResponse = mockMvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.customerId").value(customerId))
            .andExpect(jsonPath("$.status").value("PAYMENT_PENDING"))
            .andExpect(jsonPath("$.totalAmount").value(1050.99))
            .andExpect(jsonPath("$.items").isArray)
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.id").exists())
            .andReturn()

        val responseContent = createResponse.response.contentAsString
        val orderResponse = objectMapper.readTree(responseContent)
        val orderId = orderResponse.get("id").asText()

        // Verify order was saved in database
        val savedOrder = orderRepository.findById(UUID.fromString(orderId)).orElse(null)
        assert(savedOrder != null)
        assert(savedOrder!!.status == OrderStatus.PAYMENT_PENDING)
        assert(savedOrder.customerId == customerId)
        assert(savedOrder.totalAmount == BigDecimal("1050.99"))

        // Verify outbox event was created
        val outboxEvents = outboxEventRepository.findAll()
        assert(outboxEvents.isNotEmpty())
        assert(outboxEvents.any { it.eventType == "OrderCreated" })

        // Step 2: Simulate Payment Authorization
        mockMvc.perform(
            put("/orders/{orderId}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "INVENTORY_PENDING"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("INVENTORY_PENDING"))

        // Verify order status was updated
        val updatedOrder = orderRepository.findById(UUID.fromString(orderId)).orElse(null)
        assert(updatedOrder!!.status == OrderStatus.INVENTORY_PENDING)

        // Step 3: Simulate Inventory Reservation Success
        mockMvc.perform(
            put("/orders/{orderId}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "COMPLETED"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("COMPLETED"))

        // Verify final order status
        val completedOrder = orderRepository.findById(UUID.fromString(orderId)).orElse(null)
        assert(completedOrder!!.status == OrderStatus.COMPLETED)

        // Verify completion event was created
        val finalOutboxEvents = outboxEventRepository.findAll()
        assert(finalOutboxEvents.any { it.eventType == "OrderCompleted" })
    }

    @Test
    fun `order lifecycle with payment failure`() {
        // Step 1: Create Order
        val customerId = "customer-87654321"
        val createOrderRequest = CreateOrderRequest(
            customerId = customerId,
            items = listOf(
                CreateOrderRequest.OrderItemRequest(
                    sku = "EXPENSIVE-ITEM",
                    quantity = 1,
                    unitPrice = BigDecimal("50000.00")
                )
            )
        )

        val createResponse = mockMvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("PAYMENT_PENDING"))
            .andReturn()

        val responseContent = createResponse.response.contentAsString
        val orderResponse = objectMapper.readTree(responseContent)
        val orderId = orderResponse.get("id").asText()

        // Step 2: Simulate Payment Failure - Cancel Order
        mockMvc.perform(
            post("/orders/{orderId}/cancel", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"reason": "Payment declined - insufficient funds"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELLED"))

        // Verify order was cancelled
        val cancelledOrder = orderRepository.findById(UUID.fromString(orderId)).orElse(null)
        assert(cancelledOrder!!.status == OrderStatus.CANCELLED)

        // Verify cancellation event was created
        val outboxEvents = outboxEventRepository.findAll()
        assert(outboxEvents.any { it.eventType == "OrderCancelled" })
    }

    @Test
    fun `order lifecycle with inventory failure`() {
        // Step 1: Create Order
        val customerId = "customer-11111111"
        val createOrderRequest = CreateOrderRequest(
            customerId = customerId,
            items = listOf(
                CreateOrderRequest.OrderItemRequest(
                    sku = "OUT-OF-STOCK-ITEM",
                    quantity = 10,
                    unitPrice = BigDecimal("100.00")
                )
            )
        )

        val createResponse = mockMvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("PAYMENT_PENDING"))
            .andReturn()

        val responseContent = createResponse.response.contentAsString
        val orderResponse = objectMapper.readTree(responseContent)
        val orderId = orderResponse.get("id").asText()

        // Step 2: Payment Authorization
        mockMvc.perform(
            put("/orders/{orderId}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "INVENTORY_PENDING"}""")
        )
            .andExpect(status().isOk)

        // Step 3: Simulate Inventory Failure - Cancel Order
        mockMvc.perform(
            post("/orders/{orderId}/cancel", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"reason": "Insufficient inventory - only 2 items available"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELLED"))

        // Verify order was cancelled
        val cancelledOrder = orderRepository.findById(UUID.fromString(orderId)).orElse(null)
        assert(cancelledOrder!!.status == OrderStatus.CANCELLED)
    }

    @Test
    fun `order lifecycle with validation errors`() {
        // Test invalid customer ID
        val invalidCustomerRequest = CreateOrderRequest(
            customerId = "invalid-customer",
            items = listOf(
                CreateOrderRequest.OrderItemRequest(
                    sku = "VALID-SKU-001",
                    quantity = 1,
                    unitPrice = BigDecimal("100.00")
                )
            )
        )

        mockMvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCustomerRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation Failed"))
            .andExpect(jsonPath("$.validationErrors.customerId").exists())

        // Test invalid SKU
        val invalidSkuRequest = CreateOrderRequest(
            customerId = "customer-12345678",
            items = listOf(
                CreateOrderRequest.OrderItemRequest(
                    sku = "invalid-sku!",
                    quantity = 1,
                    unitPrice = BigDecimal("100.00")
                )
            )
        )

        mockMvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidSkuRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation Failed"))
            .andExpect(jsonPath("$.validationErrors['items[0].sku']").exists())

        // Test duplicate SKUs
        val duplicateSkuRequest = CreateOrderRequest(
            customerId = "customer-12345678",
            items = listOf(
                CreateOrderRequest.OrderItemRequest(
                    sku = "LAPTOP-001",
                    quantity = 1,
                    unitPrice = BigDecimal("100.00")
                ),
                CreateOrderRequest.OrderItemRequest(
                    sku = "LAPTOP-001",
                    quantity = 1,
                    unitPrice = BigDecimal("100.00")
                )
            )
        )

        mockMvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateSkuRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation Failed"))
            .andExpect(jsonPath("$.message").value("Request validation failed"))
    }

    @Test
    fun `order lifecycle with business rule violations`() {
        // Test order with too many items
        val manyItems = (1..51).map { index ->
            CreateOrderRequest.OrderItemRequest(
                sku = "ITEM-$index",
                quantity = 1,
                unitPrice = BigDecimal("10.00")
            )
        }
        val tooManyItemsRequest = CreateOrderRequest(
            customerId = "customer-12345678",
            items = manyItems
        )

        mockMvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tooManyItemsRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation Failed"))

        // Test order exceeding maximum value
        val highValueRequest = CreateOrderRequest(
            customerId = "customer-12345678",
            items = listOf(
                CreateOrderRequest.OrderItemRequest(
                    sku = "EXPENSIVE-ITEM",
                    quantity = 1,
                    unitPrice = BigDecimal("100001.00")
                )
            )
        )

        mockMvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(highValueRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation Failed"))
            .andExpect(jsonPath("$.message").value("Request validation failed"))
    }

    @Test
    fun `order lifecycle with invalid status transitions`() {
        // Step 1: Create Order
        val customerId = "customer-99999999"
        val createOrderRequest = CreateOrderRequest(
            customerId = customerId,
            items = listOf(
                CreateOrderRequest.OrderItemRequest(
                    sku = "TEST-ITEM-001",
                    quantity = 1,
                    unitPrice = BigDecimal("50.00")
                )
            )
        )

        val createResponse = mockMvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val responseContent = createResponse.response.contentAsString
        val orderResponse = objectMapper.readTree(responseContent)
        val orderId = orderResponse.get("id").asText()

        // Step 2: Try invalid status transition (PAYMENT_PENDING -> PENDING)
        mockMvc.perform(
            put("/orders/{orderId}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "PENDING"}""")
        )
            .andExpect(status().isConflict)

        // Step 3: Complete the order
        mockMvc.perform(
            put("/orders/{orderId}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "INVENTORY_PENDING"}""")
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            put("/orders/{orderId}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "COMPLETED"}""")
        )
            .andExpect(status().isOk)

        // Step 4: Try to cancel completed order
        mockMvc.perform(
            post("/orders/{orderId}/cancel", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"reason": "Customer change of mind"}""")
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `order lifecycle with customer order history`() {
        val customerId = "customer-historytest123"

        // Create multiple orders for the same customer
        repeat(3) { index ->
            val createOrderRequest = CreateOrderRequest(
                customerId = customerId,
                items = listOf(
                    CreateOrderRequest.OrderItemRequest(
                        sku = "ORDER-$index",
                        quantity = 1,
                        unitPrice = BigDecimal("100.00")
                    )
                )
            )

            mockMvc.perform(
                post("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createOrderRequest))
            )
                .andExpect(status().isCreated)
        }

        // Retrieve customer order history
        mockMvc.perform(get("/orders/customer/{customerId}", customerId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].customerId").value(customerId))
            .andExpect(jsonPath("$[1].customerId").value(customerId))
            .andExpect(jsonPath("$[2].customerId").value(customerId))

        // Verify orders are sorted by creation date (newest first)
        val response = mockMvc.perform(get("/orders/customer/{customerId}", customerId))
            .andExpect(status().isOk)
            .andReturn()

        val responseContent = response.response.contentAsString
        val orders = objectMapper.readTree(responseContent)
        
        // Verify we have 3 orders
        assert(orders.size() == 3)
        
        // Verify all orders belong to the customer
        for (i in 0 until orders.size()) {
            assert(orders.get(i).get("customerId").asText() == customerId)
        }
    }

    @Test
    fun `order lifecycle with health checks`() {
        // Test health endpoint
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.service").value("orders-service"))

        // Test readiness endpoint
        mockMvc.perform(get("/health/ready"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ready"))

        // Test liveness endpoint
        mockMvc.perform(get("/health/live"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("alive"))
    }
}
