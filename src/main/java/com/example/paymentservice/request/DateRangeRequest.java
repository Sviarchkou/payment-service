package com.example.paymentservice.request;

import java.time.Instant;

public record DateRangeRequest(Instant from, Instant to) {
}
