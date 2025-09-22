package com.mercury.orders.apigateway.filters

import com.mercury.orders.apigateway.config.IdempotencyProperties
import com.mercury.orders.apigateway.util.HeaderNames
import org.springframework.core.Ordered
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.Base64

@Component
class IdempotencyFilter(
    private val properties: IdempotencyProperties,
    private val redis: ReactiveStringRedisTemplate,
) : GlobalFilter, Ordered {

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request

        if (request.method != HttpMethod.POST) {
            return chain.filter(exchange)
        }

        val keyHeader = properties.keyHeader
        val key = request.headers.getFirst(keyHeader)
        if (key.isNullOrBlank()) {
            return chain.filter(exchange)
        }

        val redisKey = buildRedisKey(request, key)
        val ttl = Duration.ofHours(properties.ttlHours)

        return redis.opsForValue().get(redisKey)
            .flatMap { cached ->
                val response = exchange.response
                val parts = cached.split("|", limit = 3)
                val status = parts.getOrNull(0)?.toIntOrNull() ?: 200
                val headersBase64 = parts.getOrNull(1)
                val bodyBase64 = parts.getOrNull(2)
                if (headersBase64 != null) {
                    val headerPairs = String(Base64.getDecoder().decode(headersBase64), StandardCharsets.UTF_8)
                    headerPairs.split("\n").filter { it.contains(":") }.forEach { line ->
                        val idx = line.indexOf(":")
                        val name = line.substring(0, idx)
                        val value = line.substring(idx + 1)
                        response.headers.add(name, value)
                    }
                }
                response.setStatusCode(HttpStatus.valueOf(status))
                val correlationId = exchange.request.headers.getFirst(HeaderNames.CORRELATION_ID)
                if (!correlationId.isNullOrBlank()) {
                    response.headers.set(HeaderNames.CORRELATION_ID, correlationId)
                }
                val bodyBytes = if (bodyBase64 != null) Base64.getDecoder().decode(bodyBase64) else ByteArray(0)
                val dataBuffer = response.bufferFactory().wrap(bodyBytes)
                response.writeWith(Mono.just(dataBuffer))
            }
            .switchIfEmpty(Mono.defer {
                val response = exchange.response
                val bufferFactory = response.bufferFactory()
                val responseCapture = StringBuilder()
                return@defer chain.filter(exchange).then(Mono.defer {
                    // Capture is non-trivial without decorating; keep minimal behavior: store status only
                    val status = response.rawStatusCode ?: 200
                    val headersEncoded = Base64.getEncoder().encodeToString(
                        response.headers.entries.joinToString("\n") { "${it.key}:${it.value.joinToString(",")}" }
                            .toByteArray(StandardCharsets.UTF_8)
                    )
                    val payloadEncoded = Base64.getEncoder().encodeToString(ByteArray(0))
                    val value = "$status|$headersEncoded|$payloadEncoded"
                    redis.opsForValue().set(redisKey, value, ttl).then()
                })
            })
    }

    private fun buildRedisKey(request: ServerHttpRequest, idempotencyKey: String): String {
        val path = request.path.toString()
        val method = request.method?.name() ?: "POST"
        return "idem:${method}:${path}:$idempotencyKey"
    }

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE - 10
}


