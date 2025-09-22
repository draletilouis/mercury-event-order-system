package com.mercury.orders.apigateway.ratelimit

import com.mercury.orders.apigateway.util.HeaderNames
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@Primary
class ClientIdKeyResolver : KeyResolver {
    override fun resolve(exchange: org.springframework.web.server.ServerWebExchange): Mono<String> {
        val headers = exchange.request.headers
        val clientId = headers.getFirst(HeaderNames.CLIENT_ID)
        if (!clientId.isNullOrBlank()) {
            return Mono.just(clientId)
        }

        val auth = headers.getFirst("Authorization")
        if (!auth.isNullOrBlank()) {
            return Mono.just(auth)
        }

        val remoteAddr = exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"
        return Mono.just(remoteAddr)
    }
}


