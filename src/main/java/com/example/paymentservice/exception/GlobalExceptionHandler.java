package com.example.paymentservice.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadPathVariableForGetAllPaymentsByException.class)
    public ResponseEntity<ErrorResponse> handle(BadPathVariableForGetAllPaymentsByException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(PaymentNotFoundException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PaymentSimulationException.class)
    public ResponseEntity<ErrorResponse> handle(PaymentSimulationException ex) {
        return buildErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(KafkaSendException.class)
    public ResponseEntity<ErrorResponse> handle(KafkaSendException ex) {
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handle(ConstraintViolationException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(Throwable ex, HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.create(ex, status, message));
    }
}
