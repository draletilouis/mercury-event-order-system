package com.mercury.orders.orders

import com.mercury.orders.tracing.TracingConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@Import(TracingConfiguration::class)
class OrdersApplication

fun main(args: Array<String>) {
    runApplication<OrdersApplication>(*args)
}


