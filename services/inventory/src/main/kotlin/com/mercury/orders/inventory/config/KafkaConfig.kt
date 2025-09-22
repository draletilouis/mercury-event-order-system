package com.mercury.orders.inventory.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka

@Configuration
@EnableKafka
class KafkaConfig {
    @Bean
    fun objectMapper(): ObjectMapper {
        val ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.mercury.orders.events")
            .build()
        val mapper = jacksonObjectMapper()
        return mapper.activateDefaultTyping(ptv)
    }
}


