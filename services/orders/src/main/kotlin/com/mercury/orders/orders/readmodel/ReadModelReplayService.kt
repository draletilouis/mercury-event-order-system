package com.mercury.orders.orders.readmodel

import com.mercury.orders.events.*
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.Properties

@Service
class ReadModelReplayService(
    private val projector: OrderReadModelProjector,
    @Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String
) {
    fun replayAll() {
        val props = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, "orders-read-model-replay-" + System.currentTimeMillis())
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer::class.java)
            put(JsonDeserializer.TRUSTED_PACKAGES, "com.mercury.orders.events")
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
        }

        val topics = listOf("order-events", "payment-events", "inventory-events")

        KafkaConsumer<String, DomainEvent>(props).use { consumer ->
            consumer.subscribe(topics)
            while (true) {
                val records = consumer.poll(Duration.ofSeconds(1))
                if (records.isEmpty) break
                records.forEach { record ->
                    when (val event = record.value()) {
                        is OrderCreatedEvent -> projector.on(event)
                        is PaymentAuthorizedEvent -> projector.on(event)
                        is PaymentDeclinedEvent -> projector.on(event)
                        is InventoryReservedEvent -> projector.on(event)
                        is InventoryInsufficientEvent -> projector.on(event)
                        is OrderCompletedEvent -> projector.on(event)
                        is OrderCancelledEvent -> projector.on(event)
                        is PaymentReversedEvent -> { /* not affecting read model currently */ }
                        is InventoryReleasedEvent -> { /* not affecting read model currently */ }
                        else -> {}
                    }
                }
            }
        }
    }
}


