package com.mercury.orders.orders.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [SkuValidatorImpl::class])
annotation class ValidSku(
    val message: String = "SKU format is invalid",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class SkuValidatorImpl : ConstraintValidator<ValidSku, String> {
    
    private val skuPattern = Regex("^[A-Z0-9]+(-[A-Z0-9]+)*$")
    private val reservedSkus = setOf("SYSTEM", "ADMIN", "TEST")
    
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return true // Let @NotNull handle null validation
        
        // Check format
        if (!skuPattern.matches(value)) return false
        
        // Check if it's a reserved SKU
        if (reservedSkus.contains(value.uppercase())) {
            context?.disableDefaultConstraintViolation()
            context?.buildConstraintViolationWithTemplate("SKU '${value}' is reserved and cannot be used")
                ?.addConstraintViolation()
            return false
        }
        
        // Check for reasonable length
        if (value.length < 3 || value.length > 50) return false
        
        return true
    }
}
