package com.mercury.orders.inventory.controller

import com.mercury.orders.inventory.domain.InventoryItem
import com.mercury.orders.inventory.repository.InventoryItemRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/admin/inventory")
@Validated
class InventoryAdminController(
    private val itemRepository: InventoryItemRepository
) {
    data class UpsertItemRequest(
        @field:NotBlank val sku: String,
        @field:NotBlank val name: String,
        val description: String? = null,
        @field:Min(0) val availableQuantity: Int = 0,
        @field:Min(0) val reservedQuantity: Int = 0,
        val unitCost: BigDecimal? = null
    )

    @PutMapping
    fun upsert(@Valid @RequestBody req: UpsertItemRequest): ResponseEntity<InventoryItem> {
        val existing = itemRepository.findBySku(req.sku).orElse(null)
        val now = Instant.now()
        val item = if (existing == null) {
            InventoryItem(
                sku = req.sku,
                name = req.name,
                description = req.description,
                availableQuantity = req.availableQuantity,
                reservedQuantity = req.reservedQuantity,
                unitCost = req.unitCost,
                createdAt = now,
                updatedAt = now,
                version = 0
            )
        } else {
            existing.copy(
                name = req.name,
                description = req.description,
                availableQuantity = req.availableQuantity,
                reservedQuantity = req.reservedQuantity,
                unitCost = req.unitCost,
                updatedAt = now
            )
        }
        return ResponseEntity.ok(itemRepository.save(item))
    }

    @GetMapping("/{sku}")
    fun getBySku(@PathVariable sku: String): ResponseEntity<InventoryItem> =
        itemRepository.findBySku(sku)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())

    @GetMapping
    fun list(): List<InventoryItem> = itemRepository.findAll()
}


