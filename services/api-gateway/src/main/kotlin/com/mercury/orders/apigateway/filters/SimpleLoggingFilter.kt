package com.mercury.orders.apigateway.filters

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Simple logging filter for the API Gateway.
 * Logs incoming requests and outgoing responses.
 */
@Component
class SimpleLoggingFilter : AbstractGatewayFilterFactory<SimpleLoggingFilter.Config>() {

    private val logger = LoggerFactory.getLogger(SimpleLoggingFilter::class.java)

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val request = exchange.request
            
            logger.info("Incoming request: {} {}", request.method, request.uri)

            chain.filter(exchange)
                .doOnNext { response ->
                    logger.info("Response: {} {} - Status: {}", 
                        request.method, 
                        request.uri, 
                        exchange.response.statusCode)
                }
                .doOnError { error ->
                    logger.error("Request failed: {} {} - Error: {}", 
                        request.method, 
                        request.uri, 
                        error.message)
                }
        }
    }

    data class Config(
        val enabled: Boolean = true
    )
}
