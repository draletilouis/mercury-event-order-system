package com.mercury.orders.orders.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class HealthController {

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        val response = mapOf(
            "status" to "up",
            "timestamp" to Instant.now().toString(),
            "service" to "orders-service"
        )
        
        return ResponseEntity.ok(response)
    }

    @GetMapping("/health/ready")
    fun readiness(): ResponseEntity<Map<String, String>> {
        // Basic readiness check - service is ready if it can respond
        return ResponseEntity.ok(mapOf(
            "status" to "ready",
            "timestamp" to Instant.now().toString()
        ))
    }

    @GetMapping("/health/live")
    fun liveness(): ResponseEntity<Map<String, String>> {
        // Basic liveness check - service is alive if it can respond
        return ResponseEntity.ok(mapOf(
            "status" to "alive",
            "timestamp" to Instant.now().toString()
        ))
    }
}
