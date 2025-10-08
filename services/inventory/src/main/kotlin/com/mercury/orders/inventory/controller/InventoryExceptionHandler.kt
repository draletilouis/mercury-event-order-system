package com.mercury.orders.inventory.controller

import com.mercury.orders.inventory.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.Instant

@RestControllerAdvice
class InventoryExceptionHandler {

    @ExceptionHandler(InventoryNotFoundException::class)
    fun handleInventoryNotFoundException(
        ex: InventoryNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message ?: "Inventory item not found",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error)
    }

    @ExceptionHandler(InventoryConflictException::class)
    fun handleInventoryConflictException(
        ex: InventoryConflictException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.CONFLICT.value(),
            error = "Conflict",
            message = ex.message ?: "Inventory conflict",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error)
    }

    @ExceptionHandler(InsufficientInventoryException::class)
    fun handleInsufficientInventoryException(
        ex: InsufficientInventoryException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "Insufficient inventory",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(InventoryReservationException::class)
    fun handleInventoryReservationException(
        ex: InventoryReservationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = ex.message ?: "Failed to reserve inventory",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }

    @ExceptionHandler(InventoryReleaseException::class)
    fun handleInventoryReleaseException(
        ex: InventoryReleaseException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = ex.message ?: "Failed to release inventory",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        
        val error = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Validation Failed",
            message = errors,
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = ex.message ?: "An unexpected error occurred",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }
}


