package com.example.paymentservice.security;

import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserSecurity {

    private final PaymentService paymentService;

    public Mono<Boolean> equalToCurrentUserId(UUID id){
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Mono.just(userId.equals(id));
    }

    public Mono<Boolean> isPaymentOwnedByUser(String id){
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return paymentService.findPaymentOwnerById(id).map(userId::equals);
    }

    public Mono<Boolean> isOrderOwnedByUser(UUID orderId){
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return paymentService.findPaymentOwnerByOrderId(orderId).map(userId::equals);
    }

}
