package com.mercury.orders.orders.api

import com.mercury.orders.orders.readmodel.ReadModelReplayService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/read-model")
class AdminReplayController(
    private val replayService: ReadModelReplayService
) {
    @PostMapping("/replay")
    fun replay(): ResponseEntity<Void> {
        replayService.replayAll()
        return ResponseEntity.accepted().build()
    }
}


