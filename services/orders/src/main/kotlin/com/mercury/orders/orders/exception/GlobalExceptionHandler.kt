package com.mercury.orders.orders.exception

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.Instant
import java.util.*

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Illegal argument: ${ex.message}", ex)
        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "Invalid argument provided",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Illegal state: ${ex.message}", ex)
        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.CONFLICT.value(),
            error = "Conflict",
            message = ex.message ?: "Invalid state transition",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Validation failed: ${ex.message}", ex)
        
        val validationErrors = ex.bindingResult.fieldErrors.associate { error ->
            error.field to (error.defaultMessage ?: "Invalid value")
        }
        
        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Validation Failed",
            message = "Request validation failed",
            path = request.getDescription(false).removePrefix("uri="),
            validationErrors = validationErrors
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Constraint violation: ${ex.message}", ex)
        
        val validationErrors = ex.constraintViolations.associate { violation ->
            violation.propertyPath.toString() to violation.message
        }
        
        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Validation Failed",
            message = "Constraint validation failed",
            path = request.getDescription(false).removePrefix("uri="),
            validationErrors = validationErrors
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid JSON: ${ex.message}", ex)
        
        val message = when (val cause = ex.cause) {
            is JsonMappingException -> "Invalid JSON format"
            is MissingKotlinParameterException -> "Missing required field: ${cause.parameter.name}"
            else -> "Invalid request body format"
        }
         
        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = message,
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleHttpMediaTypeNotSupportedException(ex: HttpMediaTypeNotSupportedException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Unsupported media type: ${ex.message}", ex)
        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
            error = "Unsupported Media Type",
            message = "Content-Type must be application/json",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse)
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(ex: HttpRequestMethodNotSupportedException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Method not supported: ${ex.message}", ex)
        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.METHOD_NOT_ALLOWED.value(),
            error = "Method Not Allowed",
            message = "HTTP method ${ex.method} is not supported for this endpoint",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(ex: MissingServletRequestParameterException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Missing request parameter: ${ex.message}", ex)
        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = "Missing required parameter: ${ex.parameterName}",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(ex: MethodArgumentTypeMismatchException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Type mismatch: ${ex.message}", ex)
        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = "Invalid value for parameter '${ex.name}': ${ex.value}",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(OrderNotFoundException::class)
    fun handleOrderNotFoundException(ex: OrderNotFoundException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Order not found: ${ex.message}", ex)
        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message ?: "Order not found",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    @ExceptionHandler(OrderBusinessException::class)
    fun handleOrderBusinessException(ex: OrderBusinessException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Business rule violation: ${ex.message}", ex)
        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = ex.httpStatus.value(),
            error = ex.httpStatus.reasonPhrase,
            message = ex.message ?: "Business rule violation",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(ex.httpStatus).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred", ex)
        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred. Please try again later.",
            path = request.getDescription(false).removePrefix("uri="),
            traceId = UUID.randomUUID().toString()
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    data class ErrorResponse(
        val timestamp: Instant,
        val status: Int,
        val error: String,
        val message: String,
        val path: String,
        val validationErrors: Map<String, String>? = null,
        val traceId: String? = null
    )
}

// Custom exceptions for better error handling
class OrderNotFoundException(message: String) : RuntimeException(message)

class OrderBusinessException(
    message: String,
    val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST
) : RuntimeException(message)
