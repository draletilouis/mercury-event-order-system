package com.mercury.orders.apigateway.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue

/**
 * Configuration properties for idempotency handling in the API Gateway.
 * 
 * These properties are bound from the `app.idempotency` section of application.yml
 * and provide configuration for idempotency key handling.
 */
@ConfigurationProperties(prefix = "app.idempotency")
data class IdempotencyProperties(
    /**
     * The HTTP header name that contains the idempotency key.
     * Default: "X-Idempotency-Key"
     */
    @DefaultValue("X-Idempotency-Key")
    val keyHeader: String,
    
    /**
     * Time-to-live for idempotency keys in hours.
     * Default: 24 hours
     */
    @DefaultValue("24")
    val ttlHours: Long,
    
    /**
     * Whether to enable idempotency checking.
     * Default: true
     */
    @DefaultValue("true")
    val enabled: Boolean = true
) {
    /**
     * Get the TTL in milliseconds for Redis storage.
     */
    fun getTtlMillis(): Long = ttlHours * 60 * 60 * 1000
    
    /**
     * Validate the configuration properties.
     */
    init {
        require(ttlHours > 0) { "TTL hours must be positive" }
        require(keyHeader.isNotBlank()) { "Key header cannot be blank" }
    }
}
