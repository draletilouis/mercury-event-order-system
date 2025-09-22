package com.mercury.orders.orders.readmodel

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrderReadModelRepository : JpaRepository<OrderReadModel, UUID> {
}


