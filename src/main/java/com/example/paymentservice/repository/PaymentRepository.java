package com.example.paymentservice.repository;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.reponse.PaymentSumResult;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface PaymentRepository extends ReactiveMongoRepository<Payment, String> {

    Flux<Payment> findAllByStatus(PaymentStatus status);

    Flux<Payment> findAllByUserIdOrOrderId(UUID userId, UUID orderId);

    Flux<Payment> findAllByUserId(UUID userId);

    Flux<Payment> findAllByOrderId(UUID orderId);

    @Aggregation(pipeline = {
            "{ $match: { user_id: ?0, timestamp: { $gte: ?1, $lte: ?2 }, status: 'SUCCESS' } }",
            "{ $group: { _id: null, sum: { $sum: '$payment_amount' } } }"
    })
    Mono<PaymentSumResult> getSumOfPaymentsForUserWithDateRange(UUID userId, Instant from, Instant to);

    @Aggregation(pipeline = {
            "{ $match: { timestamp: { $gte: ?0, $lte: ?1 }, status: 'SUCCESS' } }",
            "{ $group: { _id: null, sum: { $sum: '$payment_amount' } } }"
    })
    Mono<PaymentSumResult> getSumOfPaymentsForAllUsersWithDateRange(Instant t1, Instant t2);

    @Query(value="{ '_id' : ?0 }", fields="{ 'user_id' : 1}")
    Mono<UUID> findPaymentOwnerById(@Param("id") String id);

    @Query(value="{ 'order_id' : ?0 }", fields="{ 'user_id' : 1}")
    Mono<UUID> findPaymentOwnerByOrderId(@Param("id") UUID orderId);
}
