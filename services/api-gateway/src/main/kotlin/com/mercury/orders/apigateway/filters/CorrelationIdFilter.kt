package com.mercury.orders.apigateway.filters

import com.mercury.orders.apigateway.util.HeaderNames
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.UUID

@Component
class CorrelationIdFilter : GlobalFilter, Ordered {

    override fun filter(exchange: ServerWebExchange, chain: org.springframework.cloud.gateway.filter.GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val existing = request.headers.getFirst(HeaderNames.CORRELATION_ID)
        val correlationId = existing ?: UUID.randomUUID().toString()

        val mutatedRequest = exchange.request.mutate()
            .headers { httpHeaders: HttpHeaders ->
                httpHeaders.set(HeaderNames.CORRELATION_ID, correlationId)
            }
            .build()

        val mutatedExchange = exchange.mutate()
            .request(mutatedRequest)
            .build()

        return chain.filter(mutatedExchange)
            .then(Mono.fromRunnable {
                mutatedExchange.response.headers.set(HeaderNames.CORRELATION_ID, correlationId)
            })
    }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE
}


