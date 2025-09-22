package com.mercury.orders.orders.validation

import com.mercury.orders.orders.dto.CancelOrderRequest
import com.mercury.orders.orders.dto.CreateOrderRequest
import com.mercury.orders.orders.dto.UpdateOrderStatusRequest
import com.mercury.orders.orders.domain.OrderStatus
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class ValidationTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        val factory = Validation.buildDefaultValidatorFactory()
        validator = factory.validator
    }

    @Test
    fun `CreateOrderRequest with valid data should pass validation`() {
        // Given
        val request = CreateOrderRequest(
            customerId = "customer-12345678",
            items = listOf(
                CreateOrderRequest.OrderItemRequest(
                    sku = "LAPTOP-001",
                    quantity = 1,
                    unitPrice = BigDecimal("999.99")
                )
            )
        )

        // When
        val violations = validator.validate(request)

        // Then
        assert(violations.isEmpty()) { "Validation should pass for valid request: ${violations.map { it.message }}" }
    }

    @Test
    fun `CreateOrderRequest with invalid customer ID should fail validation`() {
        // Given
        val request = CreateOrderRequest(
            customerId = "invalid-customer-id",
            items = listOf(
                CreateOrderRequest.OrderItemRequest(
                    sku = "LAPTOP-001",
                    quantity = 1,
                    unitPrice = BigDecimal("999.99")
                )
            )
        )

        // When
        val violations = validator.validate(request)

        // Then
        assert(violations.isNotEmpty()) { "Validation should fail for invalid customer ID" }
        assert(violations.any { it.message == "Customer ID format is invalid" })
    }

    @Test
    fun `CreateOrderRequest with blank customer ID should fail validation`() {
        // Given
        val request = CreateOrderRequest(
            customerId = "",
            items = listOf(
                CreateOrderRequest.OrderItemRequest(
                    sku = "LAPTOP-001",
                    quantity = 1,
                    unitPrice = BigDecimal("999.99")
                )
            )
        )

        // When
        val violations = validator.validate(request)

        // Then
        assert(violations.isNotEmpty()) { "Validation should fail for blank customer ID" }
        assert(violations.any { it.message == "Customer ID cannot be blank" })
    }

    @Test
    fun `CreateOrderRequest with invalid SKU should fail validation`() {
        // Given
        val request = CreateOrderRequest(
            customerId = "customer-12345678",
            items = listOf(
                CreateOrderRequest.OrderItemRequest(
                    sku = "invalid-sku!",
                    quantity = 1,
                    unitPrice = BigDecimal("999.99")
                )
            )
        )

        // When
        val violations = validator.validate(request)

        // Then
        assert(violations.isNotEmpty()) { "Validation should fail for invalid SKU" }
        assert(violations.any { it.message == "SKU format is invalid" })
    }

    @Test
    fun `CreateOrderRequest with reserved SKU should fail validation`() {
        // Given
        val request = CreateOrderRequest(
            customerId = "customer-12345678",
            items = listOf(
                CreateOrderRequest.OrderItemRequest(
                    sku = "SYSTEM",
                    quantity = 1,
                    unitPrice = BigDecimal("999.99")
                )
            )
        )

        // When
        val violations = validator.validate(request)

        // Then
        assert(violations.isNotEmpty()) { "Validation should fail for reserved SKU" }
        assert(violations.any { it.message.contains("SKU 'SYSTEM' is reserved") })
    }

    @Test
    fun `CreateOrderRequest with zero quantity should fail validation`() {
        // Given
        val request = CreateOrderRequest(
            customerId = "customer-12345678",
            items = listOf(
                CreateOrderRequest.OrderItemRequest(
                    sku = "LAPTOP-001",
                    quantity = 0,
                    unitPrice = BigDecimal("999.99")
                )
            )
        )

        // When
        val violations = validator.validate(request)

        // Then
        assert(violations.isNotEmpty()) { "Validation should fail for zero quantity" }
        assert(violations.any { it.message == "Quantity must be at least 1" })
    }

    @Test
    fun `CreateOrderRequest with negative unit price should fail validation`() {
        // Given
        val request = CreateOrderRequest(
            customerId = "customer-12345678",
            items = listOf(
                CreateOrderRequest.OrderItemRequest(
                    sku = "LAPTOP-001",
                    quantity = 1,
                    unitPrice = BigDecimal("-10.00")
                )
            )
        )

        // When
        val violations = validator.validate(request)

        // Then
        assert(violations.isNotEmpty()) { "Validation should fail for negative unit price" }
        assert(violations.any { it.message == "Unit price must be at least 0.01" })
    }

    @Test
    fun `CreateOrderRequest with duplicate SKUs should fail validation`() {
        // Given
        val request = CreateOrderRequest(
            customerId = "customer-12345678",
            items = listOf(
                CreateOrderRequest.OrderItemRequest(
                    sku = "LAPTOP-001",
                    quantity = 1,
                    unitPrice = BigDecimal("999.99")
                ),
                CreateOrderRequest.OrderItemRequest(
                    sku = "LAPTOP-001",
                    quantity = 1,
                    unitPrice = BigDecimal("999.99")
                )
            )
        )

        // When
        val violations = validator.validate(request)

        // Then
        assert(violations.isNotEmpty()) { "Validation should fail for duplicate SKUs" }
        assert(violations.any { it.message == "Duplicate SKU found: LAPTOP-001" })
    }

    @Test
    fun `CreateOrderRequest with order total exceeding limit should fail validation`() {
        // Given
        val request = CreateOrderRequest(
            customerId = "customer-12345678",
            items = listOf(
                CreateOrderRequest.OrderItemRequest(
                    sku = "EXPENSIVE-ITEM",
                    quantity = 1,
                    unitPrice = BigDecimal("100001.00")
                )
            )
        )

        // When
        val violations = validator.validate(request)

        // Then
        assert(violations.isNotEmpty()) { "Validation should fail for order total exceeding limit" }
        assert(violations.any { it.message == "Order total cannot exceed \$100000.00" })
    }

    @Test
    fun `CreateOrderRequest with empty items list should fail validation`() {
        // Given
        val request = CreateOrderRequest(
            customerId = "customer-12345678",
            items = emptyList()
        )

        // When
        val violations = validator.validate(request)

        // Then
        assert(violations.isNotEmpty()) { "Validation should fail for empty items list" }
        assert(violations.any { it.message == "Items list cannot be empty" })
    }

    @Test
    fun `UpdateOrderStatusRequest with valid status should pass validation`() {
        // Given
        val request = UpdateOrderStatusRequest(status = OrderStatus.COMPLETED)

        // When
        val violations = validator.validate(request)

        // Then
        assert(violations.isEmpty()) { "Validation should pass for valid status update request" }
    }


    @Test
    fun `CancelOrderRequest with valid reason should pass validation`() {
        // Given
        val request = CancelOrderRequest(reason = "Customer requested cancellation")

        // When
        val violations = validator.validate(request)

        // Then
        assert(violations.isEmpty()) { "Validation should pass for valid cancel request" }
    }

    @Test
    fun `CancelOrderRequest with blank reason should fail validation`() {
        // Given
        val request = CancelOrderRequest(reason = "")

        // When
        val violations = validator.validate(request)

        // Then
        assert(violations.isNotEmpty()) { "Validation should fail for blank reason" }
        assert(violations.any { it.message == "Cancellation reason cannot be blank" })
    }

    @Test
    fun `CancelOrderRequest with too long reason should fail validation`() {
        // Given
        val longReason = "a".repeat(501)
        val request = CancelOrderRequest(reason = longReason)

        // When
        val violations = validator.validate(request)

        // Then
        assert(violations.isNotEmpty()) { "Validation should fail for too long reason" }
        assert(violations.any { it.message == "Cancellation reason must be between 1 and 500 characters" })
    }

    @Test
    fun `CreateOrderRequest with too many items should fail validation`() {
        // Given
        val manyItems = (1..51).map { index ->
            CreateOrderRequest.OrderItemRequest(
                sku = "ITEM-$index",
                quantity = 1,
                unitPrice = BigDecimal("10.00")
            )
        }
        val request = CreateOrderRequest(
            customerId = "customer-12345678",
            items = manyItems
        )

        // When
        val violations = validator.validate(request)

        // Then
        assert(violations.isNotEmpty()) { "Validation should fail for too many items" }
        assert(violations.any { it.message == "Cannot create order with more than 50 items" })
    }
}

