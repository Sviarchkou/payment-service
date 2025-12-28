package com.example.paymentservice.dto;

import com.example.paymentservice.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDto {

    String id;

    @NotNull
    UUID orderId;

    @NotNull
    UUID userId;

    PaymentStatus status;

    Instant timestamp;

    @Positive
    @NotNull
    BigDecimal paymentAmount;

}
