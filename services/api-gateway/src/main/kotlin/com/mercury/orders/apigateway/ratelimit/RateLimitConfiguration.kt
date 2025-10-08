package com.mercury.orders.apigateway.ratelimit

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Configuration for rate limiting in the API Gateway.
 * 
 * Provides key resolvers for different rate limiting strategies.
 */
@Configuration
class RateLimitConfiguration {

    /**
     * Key resolver that uses the client IP address for rate limiting.
     * This is useful for limiting requests per IP address.
     */
    @Bean("clientIdKeyResolver")
    fun clientIdKeyResolver(): KeyResolver {
        return KeyResolver { exchange: ServerWebExchange ->
            val clientIp = getClientIpAddress(exchange)
            Mono.just(clientIp)
        }
    }

    /**
     * Key resolver that uses the user ID from the request header.
     * This is useful for authenticated users.
     */
    @Bean("userIdKeyResolver")
    fun userIdKeyResolver(): KeyResolver {
        return KeyResolver { exchange: ServerWebExchange ->
            val userId = exchange.request.headers.getFirst("X-User-Id")
            Mono.just(userId ?: "anonymous")
        }
    }

    /**
     * Key resolver that uses a combination of IP and User-Agent.
     * This provides better protection against sophisticated attacks.
     */
    @Bean("ipUserAgentKeyResolver")
    fun ipUserAgentKeyResolver(): KeyResolver {
        return KeyResolver { exchange: ServerWebExchange ->
            val clientIp = getClientIpAddress(exchange)
            val userAgent = exchange.request.headers.getFirst("User-Agent") ?: "unknown"
            val key = "$clientIp:$userAgent"
            Mono.just(key.hashCode().toString())
        }
    }

    /**
     * Extract the real client IP address from the request.
     * Handles cases where the request goes through proxies or load balancers.
     */
    private fun getClientIpAddress(exchange: ServerWebExchange): String {
        val request = exchange.request
        
        // Check for X-Forwarded-For header (from proxies/load balancers)
        val xForwardedFor = request.headers.getFirst("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",")[0].trim()
        }
        
        // Check for X-Real-IP header (from nginx)
        val xRealIp = request.headers.getFirst("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp.trim()
        }
        
        // Check for CF-Connecting-IP header (from Cloudflare)
        val cfConnectingIp = request.headers.getFirst("CF-Connecting-IP")
        if (!cfConnectingIp.isNullOrBlank()) {
            return cfConnectingIp.trim()
        }
        
        // Fall back to the remote address
        return request.remoteAddress?.address?.hostAddress ?: "unknown"
    }
}
