package com.mercury.orders.orders.validation

import com.mercury.orders.orders.dto.CreateOrderRequest
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass
import java.math.BigDecimal

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [OrderValidatorImpl::class])
annotation class ValidOrder(
    val message: String = "Order validation failed",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class OrderValidatorImpl : ConstraintValidator<ValidOrder, CreateOrderRequest> {
    
    private val maxOrderValue = BigDecimal("100000.00")
    private val maxItemsPerOrder = 50
    
    override fun isValid(order: CreateOrderRequest?, context: ConstraintValidatorContext?): Boolean {
        if (order == null) return true // Let @NotNull handle null validation
        
        var isValid = true
        
        // Check for duplicate SKUs
        val skuSet = mutableSetOf<String>()
        for ((index, item) in order.items.withIndex()) {
            if (!skuSet.add(item.sku.uppercase())) {
                context?.disableDefaultConstraintViolation()
                context?.buildConstraintViolationWithTemplate("Duplicate SKU found: ${item.sku}")
                    ?.addPropertyNode("items[$index].sku")
                    ?.addConstraintViolation()
                isValid = false
            }
        }
        
        // Check total order value
        val totalValue = order.items.sumOf { item ->
            item.unitPrice.multiply(BigDecimal.valueOf(item.quantity.toLong()))
        }
        
        if (totalValue > maxOrderValue) {
            context?.disableDefaultConstraintViolation()
            context?.buildConstraintViolationWithTemplate("Order total cannot exceed $${maxOrderValue}")
                ?.addPropertyNode("items")
                ?.addConstraintViolation()
            isValid = false
        }
        
        // Check if order has zero value
        if (totalValue <= BigDecimal.ZERO) {
            context?.disableDefaultConstraintViolation()
            context?.buildConstraintViolationWithTemplate("Order total must be greater than zero")
                ?.addPropertyNode("items")
                ?.addConstraintViolation()
            isValid = false
        }
        
        // Check for reasonable item quantities (business rule)
        for ((index, item) in order.items.withIndex()) {
            if (item.quantity > 100) {
                context?.disableDefaultConstraintViolation()
                context?.buildConstraintViolationWithTemplate("Individual item quantity cannot exceed 100 units")
                    ?.addPropertyNode("items[$index].quantity")
                    ?.addConstraintViolation()
                isValid = false
            }
        }
        
        return isValid
    }
}
