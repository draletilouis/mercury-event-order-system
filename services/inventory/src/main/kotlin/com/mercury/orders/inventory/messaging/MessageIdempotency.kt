package com.mercury.orders.inventory.messaging

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class MessageIdempotency(
    private val jdbcTemplate: JdbcTemplate
) {
    fun shouldProcess(consumer: String, record: ConsumerRecord<String, Any>): Boolean {
        val key = record.topic() + ":" + (record.key() ?: "") + ":" + record.offset()
        val now = Instant.now()
        val ttl = now.plus(7, ChronoUnit.DAYS)

        val inserted = jdbcTemplate.update(
            """
            INSERT INTO idempotency_keys (consumer, idempotency_key, processed_at, expires_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT DO NOTHING
            """.trimIndent(),
            consumer,
            key,
            now,
            ttl
        )
        return inserted > 0
    }
}


