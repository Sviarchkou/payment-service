package com.example.paymentservice.exception;

public class OrderToPayNotFoundException extends RuntimeException {
    public OrderToPayNotFoundException(String message) {
        super(message);
    }
}
