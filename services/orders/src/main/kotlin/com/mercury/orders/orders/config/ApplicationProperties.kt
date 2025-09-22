package com.mercury.orders.orders.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.math.BigDecimal

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "app")
data class ApplicationProperties(
    var version: String = "0.1.0",
    var outbox: OutboxProperties = OutboxProperties(),
    var business: BusinessProperties = BusinessProperties()
) {
    data class OutboxProperties(
        var pollingInterval: Long = 5000,
        var retryInterval: Long = 30000,
        var cleanupInterval: Long = 3600000,
        var maxRetries: Int = 3,
        var batchSize: Int = 100
    )
    
    data class BusinessProperties(
        var maxOrderValue: BigDecimal = BigDecimal("100000.00"),
        var maxItemsPerOrder: Int = 50,
        var maxQuantityPerItem: Int = 100,
        var maxQuantityPerSingleItem: Int = 100
    )
}
