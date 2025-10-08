package com.mercury.orders.inventory.controller

import com.mercury.orders.inventory.domain.InventoryItem
import com.mercury.orders.inventory.dto.*
import com.mercury.orders.inventory.repository.InventoryItemRepository
import com.mercury.orders.inventory.repository.InventoryReservationRepository
import com.mercury.orders.inventory.service.InventoryService
import com.mercury.orders.inventory.service.ReservationRequestItem
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/v1/inventory")
class InventoryController(
    private val inventoryService: InventoryService,
    private val inventoryItemRepository: InventoryItemRepository,
    private val inventoryReservationRepository: InventoryReservationRepository
) {

    @GetMapping("/{sku}")
    fun getInventoryItem(@PathVariable sku: String): ResponseEntity<InventoryItemResponse> {
        val item = inventoryItemRepository.findBySku(sku)
            .orElseThrow { InventoryNotFoundException("Inventory item not found: $sku") }
        
        return ResponseEntity.ok(item.toResponse())
    }

    @GetMapping("/{sku}/check")
    fun checkInventoryAvailability(
        @PathVariable sku: String,
        @RequestParam(required = false) quantity: Int?
    ): ResponseEntity<InventoryCheckResponse> {
        val item = inventoryItemRepository.findBySku(sku)
            .orElseThrow { InventoryNotFoundException("Inventory item not found: $sku") }
        
        val response = if (quantity != null) {
            InventoryCheckResponse(
                sku = sku,
                available = item.availableQuantity > 0,
                availableQuantity = item.availableQuantity,
                reservedQuantity = item.reservedQuantity,
                requestedQuantity = quantity,
                canFulfill = item.canReserve(quantity)
            )
        } else {
            InventoryCheckResponse(
                sku = sku,
                available = item.availableQuantity > 0,
                availableQuantity = item.availableQuantity,
                reservedQuantity = item.reservedQuantity
            )
        }
        
        return ResponseEntity.ok(response)
    }

    @PostMapping
    fun createInventoryItem(
        @Valid @RequestBody request: CreateInventoryItemRequest
    ): ResponseEntity<InventoryItemResponse> {
        // Check if SKU already exists
        if (inventoryItemRepository.findBySku(request.sku).isPresent) {
            throw InventoryConflictException("Inventory item with SKU ${request.sku} already exists")
        }
        
        val item = InventoryItem(
            sku = request.sku,
            name = request.name,
            description = request.description,
            availableQuantity = request.initialQuantity,
            reservedQuantity = 0,
            unitCost = request.unitCost,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val saved = inventoryItemRepository.save(item)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
    }

    @PutMapping("/{sku}")
    fun updateInventoryItem(
        @PathVariable sku: String,
        @Valid @RequestBody request: UpdateInventoryItemRequest
    ): ResponseEntity<InventoryItemResponse> {
        val item = inventoryItemRepository.findBySku(sku)
            .orElseThrow { InventoryNotFoundException("Inventory item not found: $sku") }
        
        val updated = item.copy(
            name = request.name ?: item.name,
            description = request.description ?: item.description,
            unitCost = request.unitCost ?: item.unitCost,
            updatedAt = Instant.now()
        )
        
        val saved = inventoryItemRepository.save(updated)
        return ResponseEntity.ok(saved.toResponse())
    }

    @PostMapping("/{sku}/adjust")
    fun adjustInventoryQuantity(
        @PathVariable sku: String,
        @Valid @RequestBody request: AdjustInventoryQuantityRequest
    ): ResponseEntity<InventoryItemResponse> {
        val item = inventoryItemRepository.findBySku(sku)
            .orElseThrow { InventoryNotFoundException("Inventory item not found: $sku") }
        
        val newQuantity = item.availableQuantity + request.quantityChange
        if (newQuantity < 0) {
            throw InsufficientInventoryException("Adjustment would result in negative inventory for $sku")
        }
        
        val updated = item.copy(
            availableQuantity = newQuantity,
            updatedAt = Instant.now()
        )
        
        val saved = inventoryItemRepository.save(updated)
        return ResponseEntity.ok(saved.toResponse())
    }

    @PostMapping("/reserve")
    fun reserveInventory(
        @Valid @RequestBody request: ReserveInventoryRequest
    ): ResponseEntity<ReservationResponse> {
        try {
            val items = request.items.map { 
                ReservationRequestItem(sku = it.sku, quantity = it.quantity)
            }
            
            inventoryService.reserveForOrder(request.orderId, items)
            
            // Get the created reservations
            val reservations = inventoryReservationRepository.findByOrderIdAndStatus(
                request.orderId,
                com.mercury.orders.inventory.domain.ReservationStatus.ACTIVE
            )
            
            if (reservations.isEmpty()) {
                // Reservation failed - inventory insufficient
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ReservationResponse(
                        orderId = request.orderId,
                        reservations = emptyList(),
                        status = ReservationResponse.ReservationStatus.INSUFFICIENT_INVENTORY,
                        message = "Insufficient inventory for one or more items"
                    )
                )
            }
            
            val response = ReservationResponse(
                orderId = request.orderId,
                reservations = reservations.map {
                    ReservationResponse.ReservationDetail(
                        reservationId = it.id,
                        sku = it.sku,
                        quantity = it.quantity,
                        expiresAt = it.expiresAt
                    )
                },
                status = ReservationResponse.ReservationStatus.SUCCESS,
                message = "Inventory successfully reserved"
            )
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: Exception) {
            throw InventoryReservationException("Failed to reserve inventory: ${e.message}", e)
        }
    }

    @PostMapping("/release")
    fun releaseInventory(
        @Valid @RequestBody request: ReleaseInventoryRequest
    ): ResponseEntity<ReleaseResponse> {
        try {
            inventoryService.releaseForOrder(request.orderId)
            
            val response = ReleaseResponse(
                orderId = request.orderId,
                releasedItems = emptyList(), // Could be populated if needed
                message = "Inventory successfully released"
            )
            
            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            throw InventoryReleaseException("Failed to release inventory: ${e.message}", e)
        }
    }

    @GetMapping
    fun listInventoryItems(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ResponseEntity<List<InventoryItemResponse>> {
        val items = inventoryItemRepository.findAll()
            .map { it.toResponse() }
        
        return ResponseEntity.ok(items)
    }
}

// Extension function to convert domain model to response
private fun InventoryItem.toResponse() = InventoryItemResponse(
    id = id,
    sku = sku,
    name = name,
    description = description,
    availableQuantity = availableQuantity,
    reservedQuantity = reservedQuantity,
    totalQuantity = availableQuantity + reservedQuantity,
    unitCost = unitCost,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version
)

// Custom exceptions
class InventoryNotFoundException(message: String) : RuntimeException(message)
class InventoryConflictException(message: String) : RuntimeException(message)
class InsufficientInventoryException(message: String) : RuntimeException(message)
class InventoryReservationException(message: String, cause: Throwable?) : RuntimeException(message, cause)
class InventoryReleaseException(message: String, cause: Throwable?) : RuntimeException(message, cause)


