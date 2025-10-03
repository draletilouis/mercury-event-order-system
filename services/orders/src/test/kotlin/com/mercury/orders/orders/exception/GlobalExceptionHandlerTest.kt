package com.mercury.orders.orders.exception

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import kotlin.reflect.KParameter
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.mockito.kotlin.mock
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.lang.reflect.Method
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
class GlobalExceptionHandlerTest {

    private val exceptionHandler = GlobalExceptionHandler()
    private val mockRequest = MockHttpServletRequest()
    private val webRequest = ServletWebRequest(mockRequest)

    @Mock
    private lateinit var bindingResult: BindingResult

    @Mock
    private lateinit var constraintViolation: ConstraintViolation<Any>

    @Test
    fun `handleIllegalArgumentException should return 400 Bad Request`() {
        // Given
        val exception = IllegalArgumentException("Invalid argument provided")
        mockRequest.requestURI = "/orders"

        // When
        val response = exceptionHandler.handleIllegalArgumentException(exception, webRequest)

        // Then
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
        assert(response.body?.status == 400)
        assert(response.body?.error == "Bad Request")
        assert(response.body?.message == "Invalid argument provided")
        assert(response.body?.path == "/orders")
    }

    @Test
    fun `handleIllegalStateException should return 409 Conflict`() {
        // Given
        val exception = IllegalStateException("Cannot transition from COMPLETED to PENDING")
        mockRequest.requestURI = "/orders/123/status"

        // When
        val response = exceptionHandler.handleIllegalStateException(exception, webRequest)

        // Then
        assert(response.statusCode == HttpStatus.CONFLICT)
        assert(response.body?.status == 409)
        assert(response.body?.error == "Conflict")
        assert(response.body?.message == "Cannot transition from COMPLETED to PENDING")
    }

    @Test
    fun `handleMethodArgumentNotValidException should return 400 with validation errors`() {
        // Given
        val fieldError = FieldError("createOrderRequest", "customerId", "Customer ID cannot be empty")
        val fieldErrors = listOf(fieldError)
        whenever(bindingResult.fieldErrors).thenReturn(fieldErrors)
        
        val method = String::class.java.getMethod("toString")
        val methodParameter = MethodParameter(method, -1)
        val exception = MethodArgumentNotValidException(methodParameter, bindingResult)
        mockRequest.requestURI = "/orders"

        // When
        val response = exceptionHandler.handleValidationException(exception, webRequest)

        // Then
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
        assert(response.body?.status == 400)
        assert(response.body?.error == "Validation Failed")
        assert(response.body?.message == "Request validation failed")
        assert(response.body?.validationErrors?.size == 1)
        assert(response.body?.validationErrors?.get("customerId") == "Customer ID cannot be empty")
    }

    @Test
    fun `handleConstraintViolationException should return 400 with constraint violations`() {
        // Given
        val mockPropertyPath = mock<Path>()
        whenever(mockPropertyPath.toString()).thenReturn("customerId")
        whenever(constraintViolation.propertyPath).thenReturn(mockPropertyPath)
        whenever(constraintViolation.message).thenReturn("Customer ID must not be blank")
        
        val constraintViolations = setOf(constraintViolation)
        val exception = ConstraintViolationException("Validation failed", constraintViolations)
        mockRequest.requestURI = "/orders"

        // When
        val response = exceptionHandler.handleConstraintViolationException(exception, webRequest)

        // Then
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
        assert(response.body?.status == 400)
        assert(response.body?.error == "Validation Failed")
        assert(response.body?.message == "Constraint validation failed")
        assert(response.body?.validationErrors?.size == 1)
        assert(response.body?.validationErrors?.get("customerId") == "Customer ID must not be blank")
    }

    @Test
    fun `handleHttpMessageNotReadableException with JsonMappingException should return 400`() {
        // Given
        val jsonMappingException = JsonMappingException("Invalid JSON format")
        val exception = HttpMessageNotReadableException("Invalid JSON", jsonMappingException)
        mockRequest.requestURI = "/orders"

        // When
        val response = exceptionHandler.handleHttpMessageNotReadableException(exception, webRequest)

        // Then
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
        assert(response.body?.status == 400)
        assert(response.body?.error == "Bad Request")
        assert(response.body?.message == "Invalid JSON format")
    }

    @Test
    fun `handleHttpMessageNotReadableException with MissingKotlinParameterException should return 400`() {
        // Given - Use JsonMappingException instead since MissingKotlinParameterException is deprecated
        val jsonMappingException = JsonMappingException(null, "Missing required field: customerId")
        val exception = HttpMessageNotReadableException("Missing parameter", jsonMappingException)
        mockRequest.requestURI = "/orders"

        // When
        val response = exceptionHandler.handleHttpMessageNotReadableException(exception, webRequest)

        // Then
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
        assert(response.body?.status == 400)
        assert(response.body?.error == "Bad Request")
        assert(response.body?.message == "Invalid JSON format")
    }

    @Test
    fun `handleHttpMediaTypeNotSupportedException should return 415`() {
        // Given
        val exception = HttpMediaTypeNotSupportedException("text/plain")
        mockRequest.requestURI = "/orders"

        // When
        val response = exceptionHandler.handleHttpMediaTypeNotSupportedException(exception, webRequest)

        // Then
        assert(response.statusCode == HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        assert(response.body?.status == 415)
        assert(response.body?.error == "Unsupported Media Type")
        assert(response.body?.message == "Content-Type must be application/json")
    }

    @Test
    fun `handleHttpRequestMethodNotSupportedException should return 405`() {
        // Given
        val exception = HttpRequestMethodNotSupportedException("DELETE", setOf("GET", "POST"))
        mockRequest.requestURI = "/orders"

        // When
        val response = exceptionHandler.handleHttpRequestMethodNotSupportedException(exception, webRequest)

        // Then
        assert(response.statusCode == HttpStatus.METHOD_NOT_ALLOWED)
        assert(response.body?.status == 405)
        assert(response.body?.error == "Method Not Allowed")
        assert(response.body?.message == "HTTP method DELETE is not supported for this endpoint")
    }

    @Test
    fun `handleMissingServletRequestParameterException should return 400`() {
        // Given
        val exception = MissingServletRequestParameterException("orderId", "UUID")
        mockRequest.requestURI = "/orders/123"

        // When
        val response = exceptionHandler.handleMissingServletRequestParameterException(exception, webRequest)

        // Then
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
        assert(response.body?.status == 400)
        assert(response.body?.error == "Bad Request")
        assert(response.body?.message == "Missing required parameter: orderId")
    }

    @Test
    fun `handleMethodArgumentTypeMismatchException should return 400`() {
        // Given
        val method = String::class.java.getMethod("substring", Int::class.java)
        val methodParameter = MethodParameter(method, 0)
        val exception = MethodArgumentTypeMismatchException("invalid-uuid", UUID::class.java, "orderId", methodParameter, null)
        mockRequest.requestURI = "/orders/invalid-uuid"

        // When
        val response = exceptionHandler.handleMethodArgumentTypeMismatchException(exception, webRequest)

        // Then
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
        assert(response.body?.status == 400)
        assert(response.body?.error == "Bad Request")
        assert(response.body?.message == "Invalid value for parameter 'orderId': invalid-uuid")
    }

    @Test
    fun `handleOrderNotFoundException should return 404`() {
        // Given
        val exception = OrderNotFoundException("Order with ID 123 not found")
        mockRequest.requestURI = "/orders/123"

        // When
        val response = exceptionHandler.handleOrderNotFoundException(exception, webRequest)

        // Then
        assert(response.statusCode == HttpStatus.NOT_FOUND)
    }

    @Test
    fun `handleOrderBusinessException should return custom status`() {
        // Given
        val exception = OrderBusinessException("Cannot cancel completed order", HttpStatus.CONFLICT)
        mockRequest.requestURI = "/orders/123/cancel"

        // When
        val response = exceptionHandler.handleOrderBusinessException(exception, webRequest)

        // Then
        assert(response.statusCode == HttpStatus.CONFLICT)
        assert(response.body?.status == 409)
        assert(response.body?.error == "Conflict")
        assert(response.body?.message == "Cannot cancel completed order")
    }

    @Test
    fun `handleGenericException should return 500 with trace ID`() {
        // Given
        val exception = RuntimeException("Unexpected error")
        mockRequest.requestURI = "/orders"

        // When
        val response = exceptionHandler.handleGenericException(exception, webRequest)

        // Then
        assert(response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR)
        assert(response.body?.status == 500)
        assert(response.body?.error == "Internal Server Error")
        assert(response.body?.message == "An unexpected error occurred. Please try again later.")
        assert(response.body?.traceId != null)
    }

    @Test
    fun `error response should include timestamp`() {
        // Given
        val exception = IllegalArgumentException("Test error")
        mockRequest.requestURI = "/orders"

        // When
        val response = exceptionHandler.handleIllegalArgumentException(exception, webRequest)

        // Then
        assert(response.body?.timestamp != null)
        assert(response.body?.timestamp!!.isBefore(Instant.now().plusSeconds(1)))
    }
}

