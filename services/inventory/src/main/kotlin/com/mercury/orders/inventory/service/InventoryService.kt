package com.mercury.orders.inventory.service

import com.mercury.orders.events.InventoryInsufficientEvent
import com.mercury.orders.events.InventoryReleasedEvent
import com.mercury.orders.events.InventoryReservedEvent
import com.mercury.orders.inventory.domain.InventoryItem
import com.mercury.orders.inventory.domain.InventoryReservation
import com.mercury.orders.inventory.domain.ReservationStatus
import com.mercury.orders.inventory.outbox.OutboxWriter
import com.mercury.orders.inventory.repository.InventoryItemRepository
import com.mercury.orders.inventory.repository.InventoryReservationRepository
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.*

data class ReservationRequestItem(val sku: String, val quantity: Int)

@Service
class InventoryService(
    private val itemRepository: InventoryItemRepository,
    private val reservationRepository: InventoryReservationRepository,
    private val outboxWriter: OutboxWriter
) {
    private val defaultTtl: Duration = Duration.ofMinutes(15)

    @Transactional
    fun reserveForOrder(orderId: UUID, items: List<ReservationRequestItem>) {
        val missingOrInsufficient = mutableListOf<InventoryInsufficientEvent.InsufficientItem>()
        val updatedItems = mutableListOf<InventoryItem>()
        val reservations = mutableListOf<InventoryReservation>()

        items.forEach { req ->
            val item = itemRepository.findBySku(req.sku).orElse(null)
            if (item == null || !item.canReserve(req.quantity)) {
                missingOrInsufficient.add(
                    InventoryInsufficientEvent.InsufficientItem(
                        sku = req.sku,
                        requested = req.quantity,
                        available = item?.availableQuantity ?: 0
                    )
                )
            } else {
                updatedItems.add(item.reserve(req.quantity))
                reservations.add(
                    InventoryReservation(
                        orderId = orderId,
                        sku = req.sku,
                        quantity = req.quantity,
                        status = ReservationStatus.ACTIVE,
                        expiresAt = Instant.now().plus(defaultTtl)
                    )
                )
            }
        }

        if (missingOrInsufficient.isNotEmpty()) {
            outboxWriter.enqueueEvent(
                InventoryInsufficientEvent(
                    orderId = orderId,
                    insufficientItems = missingOrInsufficient
                ),
                aggregateId = orderId.toString()
            )
            return
        }

        try {
            // Persist changes
            itemRepository.saveAll(updatedItems)
            reservationRepository.saveAll(reservations)

            // Publish reserved event
            val reservedPayload = InventoryReservedEvent(
                orderId = orderId,
                reservedItems = reservations.map {
                    InventoryReservedEvent.ReservedItem(
                        sku = it.sku,
                        quantity = it.quantity,
                        reservationId = it.id.toString()
                    )
                }
            )
            outboxWriter.enqueueEvent(reservedPayload, aggregateId = orderId.toString())
        } catch (ex: OptimisticLockingFailureException) {
            // surface as insufficient to trigger retry at orchestrator level
            outboxWriter.enqueueEvent(
                InventoryInsufficientEvent(
                    orderId = orderId,
                    insufficientItems = items.map {
                        InventoryInsufficientEvent.InsufficientItem(
                            sku = it.sku,
                            requested = it.quantity,
                            available = 0
                        )
                    }
                ),
                aggregateId = orderId.toString()
            )
        }
    }

    @Transactional
    fun releaseForOrder(orderId: UUID) {
        val active = reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE)
        if (active.isEmpty()) return

        val itemsBySku = active.groupBy { it.sku }.mapValues { it.value.sumOf { r -> r.quantity } }
        val toUpdate = mutableListOf<InventoryItem>()

        itemsBySku.forEach { (sku, qty) ->
            itemRepository.findBySku(sku).ifPresent { item ->
                toUpdate.add(item.release(qty))
            }
        }

        itemRepository.saveAll(toUpdate)
        val released = active.map { it.transitionTo(ReservationStatus.RELEASED) }
        reservationRepository.saveAll(released)

        outboxWriter.enqueueEvent(
            InventoryReleasedEvent(
                orderId = orderId,
                releasedItems = active.map {
                    InventoryReleasedEvent.ReleasedItem(
                        sku = it.sku,
                        quantity = it.quantity,
                        reservationId = it.id.toString()
                    )
                }
            ),
            aggregateId = orderId.toString()
        )
    }

    @Transactional
    fun confirmForOrder(orderId: UUID) {
        val active = reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE)
        if (active.isEmpty()) return

        val itemsBySku = active.groupBy { it.sku }.mapValues { it.value.sumOf { r -> r.quantity } }
        val toUpdate = mutableListOf<InventoryItem>()

        itemsBySku.forEach { (sku, qty) ->
            itemRepository.findBySku(sku).ifPresent { item ->
                toUpdate.add(item.confirmReservation(qty))
            }
        }

        itemRepository.saveAll(toUpdate)
        val confirmed = active.map { it.transitionTo(ReservationStatus.CONFIRMED) }
        reservationRepository.saveAll(confirmed)
    }
}


