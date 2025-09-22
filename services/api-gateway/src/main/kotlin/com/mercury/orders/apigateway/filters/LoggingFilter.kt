package com.mercury.orders.apigateway.filters

import com.mercury.orders.apigateway.util.HeaderNames
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Instant

@Component
class LoggingFilter : GlobalFilter, Ordered {
    private val logger = LoggerFactory.getLogger(LoggingFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: org.springframework.cloud.gateway.filter.GatewayFilterChain): Mono<Void> {
        val startNanos = System.nanoTime()
        val request = exchange.request
        val correlationId = request.headers.getFirst(HeaderNames.CORRELATION_ID)
        val method = request.method?.name()
        val path = request.uri.rawPath

        logger.info("gateway_request method={} path={} correlationId={} at={}", method, path, correlationId, Instant.now().toString())

        return chain.filter(exchange).then(Mono.fromRunnable {
            val durationMs = (System.nanoTime() - startNanos) / 1_000_000
            val status = exchange.response.rawStatusCode
            val routeId = exchange.getAttribute<String>("org.springframework.cloud.gateway.support.ServerWebExchangeUtils.gatewayRoute")
            logger.info("gateway_response status={} durationMs={} routeId={} correlationId={}", status, durationMs, routeId, correlationId)
        })
    }

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE
}


