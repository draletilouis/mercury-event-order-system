package com.mercury.orders.orders.readmodel

import com.mercury.orders.events.*
import com.mercury.orders.orders.domain.OrderStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class OrderReadModelProjector(
    private val repository: OrderReadModelRepository
) {

    @Transactional
    fun on(event: OrderCreatedEvent) {
        val read = OrderReadModel(
            orderId = event.orderId,
            customerId = event.customerId,
            status = OrderStatus.PAYMENT_PENDING.name,
            totalAmount = event.totalAmount,
            currency = event.currency,
            lastEventType = event.eventType,
            lastEventAt = event.timestamp,
            updatedAt = Instant.now()
        )
        repository.save(read)
    }

    @Transactional
    fun on(event: PaymentAuthorizedEvent) {
        repository.findById(event.orderId).ifPresent { current ->
            repository.save(
                current.copy(
                    status = OrderStatus.INVENTORY_PENDING.name,
                    lastEventType = event.eventType,
                    lastEventAt = event.timestamp,
                    updatedAt = Instant.now()
                )
            )
        }
    }

    @Transactional
    fun on(event: PaymentDeclinedEvent) {
        repository.findById(event.orderId).ifPresent { current ->
            repository.save(
                current.copy(
                    status = OrderStatus.CANCELLED.name,
                    lastEventType = event.eventType,
                    lastEventAt = event.timestamp,
                    updatedAt = Instant.now()
                )
            )
        }
    }

    @Transactional
    fun on(event: InventoryReservedEvent) {
        repository.findById(event.orderId).ifPresent { current ->
            repository.save(
                current.copy(
                    status = OrderStatus.COMPLETED.name,
                    lastEventType = event.eventType,
                    lastEventAt = event.timestamp,
                    updatedAt = Instant.now()
                )
            )
        }
    }

    @Transactional
    fun on(event: InventoryInsufficientEvent) {
        repository.findById(event.orderId).ifPresent { current ->
            repository.save(
                current.copy(
                    status = OrderStatus.CANCELLED.name,
                    lastEventType = event.eventType,
                    lastEventAt = event.timestamp,
                    updatedAt = Instant.now()
                )
            )
        }
    }

    @Transactional
    fun on(event: OrderCompletedEvent) {
        repository.findById(event.orderId).ifPresent { current ->
            repository.save(
                current.copy(
                    status = OrderStatus.COMPLETED.name,
                    lastEventType = event.eventType,
                    lastEventAt = event.timestamp,
                    updatedAt = Instant.now()
                )
            )
        }
    }

    @Transactional
    fun on(event: OrderCancelledEvent) {
        repository.findById(event.orderId).ifPresent { current ->
            repository.save(
                current.copy(
                    status = OrderStatus.CANCELLED.name,
                    lastEventType = event.eventType,
                    lastEventAt = event.timestamp,
                    updatedAt = Instant.now()
                )
            )
        }
    }
}


