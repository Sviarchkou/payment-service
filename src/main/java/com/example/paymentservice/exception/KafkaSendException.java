package com.example.paymentservice.exception;

public class KafkaSendException extends RuntimeException {
    public KafkaSendException(String message, Throwable failure) {
        super(message);
    }
}
