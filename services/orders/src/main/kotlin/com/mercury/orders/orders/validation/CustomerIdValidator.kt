package com.mercury.orders.orders.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [CustomerIdValidatorImpl::class])
annotation class ValidCustomerId(
    val message: String = "Customer ID format is invalid",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class CustomerIdValidatorImpl : ConstraintValidator<ValidCustomerId, String> {
    
    private val customerIdPattern = Regex("^customer-[a-zA-Z0-9]{8,32}$")
    
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return true // Let @NotNull handle null validation
        
        return customerIdPattern.matches(value)
    }
}
