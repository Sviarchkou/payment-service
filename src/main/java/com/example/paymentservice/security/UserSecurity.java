package com.example.paymentservice.security;

import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserSecurity {

    private final PaymentService paymentService;

    public Mono<Boolean> equalToCurrentUserId(UUID id){
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> (UUID) context.getAuthentication().getPrincipal())
                .map(userId -> userId.equals(id));
    }

    public Mono<Boolean> isPaymentOwnedByUser(String id){
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> (UUID) context.getAuthentication().getPrincipal())
                .flatMap(userId ->
                        paymentService.findPaymentOwnerById(id)
                                .map(userId::equals))
                .switchIfEmpty(Mono.just(false));
    }

    public Mono<Boolean> isOrderOwnedByUser(UUID orderId){
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> (UUID) context.getAuthentication().getPrincipal())
                .flatMap(userId ->
                        paymentService.findPaymentOwnerByOrderId(orderId)
                            .map(userId::equals))
                .switchIfEmpty(Mono.just(false));
    }

}
