package com.example.paymentservice.repository;

import com.example.paymentservice.entity.OrderToPay;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface OrderToPayRepository extends ReactiveMongoRepository<OrderToPay, String> {

    Mono<OrderToPay> findByOrderId(UUID orderId);

    Mono<Void> deleteAllByOrderId(UUID orderId);

}
