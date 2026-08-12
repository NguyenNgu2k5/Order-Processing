package com.order.orderprocessing.common.exception;

import com.order.orderprocessing.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> handleBusiness(BusinessException exception, HttpServletRequest request) {
        return response(exception.getCode(), exception.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception,
                                                    HttpServletRequest request) {
        List<ErrorResponse.FieldError> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        ErrorCode code = fields.stream().anyMatch(error -> error.field().endsWith("quantity"))
                ? ErrorCode.INVALID_QUANTITY : ErrorCode.VALIDATION_ERROR;
        return response(code, "Request validation failed", request.getRequestURI(), fields);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return response(ErrorCode.ACCESS_DENIED, "Access is denied", request.getRequestURI(), List.of());
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        return response(ErrorCode.VALIDATION_ERROR, "Request validation failed",
                request.getRequestURI(), List.of());
    }

    private ResponseEntity<ErrorResponse> response(ErrorCode code, String message, String path,
                                                   List<ErrorResponse.FieldError> fields) {
        HttpStatus status = code.status();
        return ResponseEntity.status(status).body(new ErrorResponse(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), code.name(), message, path, fields));
    }
}
