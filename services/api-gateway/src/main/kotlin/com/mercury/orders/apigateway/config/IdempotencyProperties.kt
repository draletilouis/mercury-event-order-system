package com.mercury.orders.apigateway.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.idempotency")
data class IdempotencyProperties(
    var keyHeader: String = "X-Idempotency-Key",
    var ttlHours: Long = 24
)


