package com.example.paymentservice.kafka;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderKafkaMessage(UUID orderId, UUID userId, BigDecimal totalPrice) {
}
