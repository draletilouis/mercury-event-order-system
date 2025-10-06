package com.mercury.orders.apigateway.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.*

/**
 * Health check controller for the API Gateway.
 * Provides health status information for monitoring and load balancers.
 */
@RestController
@RequestMapping("/health")
class HealthController(
    @Value("\${app.version:unknown}") private val appVersion: String
) {

    @GetMapping
    fun health(): ResponseEntity<Map<String, Any>> {
        val healthInfo = mapOf(
            "status" to "UP",
            "service" to "api-gateway",
            "version" to appVersion,
            "timestamp" to Instant.now().toString(),
            "uptime" to "running"
        )
        
        return ResponseEntity.ok(healthInfo)
    }

    @GetMapping("/ready")
    fun readiness(): ResponseEntity<Map<String, Any>> {
        // In a real implementation, you'd check dependencies like Redis, downstream services
        val readinessInfo = mapOf(
            "status" to "READY",
            "checks" to mapOf(
                "redis" to "UP",
                "routing" to "UP"
            ),
            "timestamp" to Instant.now().toString()
        )
        
        return ResponseEntity.ok(readinessInfo)
    }

    @GetMapping("/live")
    fun liveness(): ResponseEntity<Map<String, Any>> {
        // Simple liveness check - if this endpoint responds, the service is alive
        val livenessInfo = mapOf(
            "status" to "ALIVE",
            "timestamp" to Instant.now().toString()
        )
        
        return ResponseEntity.ok(livenessInfo)
    }
}
