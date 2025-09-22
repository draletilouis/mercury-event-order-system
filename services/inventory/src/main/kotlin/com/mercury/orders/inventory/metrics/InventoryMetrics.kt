package com.mercury.orders.inventory.metrics

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
class InventoryMetrics(meterRegistry: MeterRegistry) {
    private val outboxBacklogGauge: AtomicInteger = AtomicInteger(0)

    init {
        meterRegistry.gauge("inventory.outbox.backlog", outboxBacklogGauge)
    }

    fun reportOutboxBacklog(size: Int) {
        outboxBacklogGauge.set(size)
    }
}


