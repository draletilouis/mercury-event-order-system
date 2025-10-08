package com.mercury.orders.payments.controller

import com.mercury.orders.payments.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.Instant

@RestControllerAdvice
class PaymentExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException::class)
    fun handlePaymentNotFoundException(
        ex: PaymentNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message ?: "Payment not found",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error)
    }

    @ExceptionHandler(InvalidPaymentStateException::class)
    fun handleInvalidPaymentStateException(
        ex: InvalidPaymentStateException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "Invalid payment state",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(PaymentAuthorizationException::class)
    fun handlePaymentAuthorizationException(
        ex: PaymentAuthorizationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = ex.message ?: "Payment authorization failed",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }

    @ExceptionHandler(PaymentReversalException::class)
    fun handlePaymentReversalException(
        ex: PaymentReversalException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = ex.message ?: "Payment reversal failed",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(
        ex: IllegalStateException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.CONFLICT.value(),
            error = "Conflict",
            message = ex.message ?: "Invalid operation",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "Invalid argument",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
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


