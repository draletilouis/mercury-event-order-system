package com.mercury.orders.inventory.repository

import com.mercury.orders.inventory.domain.InventoryItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface InventoryItemRepository : JpaRepository<InventoryItem, UUID> {
    fun findBySku(sku: String): Optional<InventoryItem>
    fun existsBySku(sku: String): Boolean
}


