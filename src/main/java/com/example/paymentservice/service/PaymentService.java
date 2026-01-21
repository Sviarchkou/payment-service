package com.example.paymentservice.service;

import com.example.paymentservice.client.PaymentSimulationClient;
import com.example.paymentservice.dto.PaymentDto;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.exception.BadPathVariableForGetAllPaymentsByException;
import com.example.paymentservice.exception.PaymentNotFoundException;
import com.example.paymentservice.kafka.PaymentKafkaProducer;
import com.example.paymentservice.mapper.PaymentMapper;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.reponse.PaymentSumResult;
import com.example.paymentservice.request.DateRangeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentSimulationClient paymentSimulationClient;
    private final PaymentKafkaProducer paymentKafkaProducer;

    public Mono<PaymentDto> makePayment(PaymentDto paymentDto) {
        return paymentSimulationClient.simulatePayment()
                .flatMap(paymentStatus -> save(paymentDto, paymentStatus))
                .flatMap(paymentKafkaProducer::sendPaymentToKafka);
    }

    public Mono<PaymentDto> save(PaymentDto paymentDto, PaymentStatus paymentStatus) {
        Payment payment = paymentMapper.toEntity(paymentDto);
        payment.setStatus(paymentStatus);
        payment.setTimestamp(Instant.now());
        return paymentRepository.save(payment).map(paymentMapper::toDto);
    }

    public Mono<PaymentDto> getById(String id) {
        return paymentRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new PaymentNotFoundException("Payment with id: %s is not found".formatted(id))))
                .map(paymentMapper::toDto);
    }

    public Flux<PaymentDto> getAll() {
        return paymentRepository.findAll().map(paymentMapper::toDto);
    }

    public Flux<PaymentDto> getAllBy(String pathVariable){
        try {
            UUID id = UUID.fromString(pathVariable);
            return paymentRepository.findAllByUserIdOrOrderId(id, id).map(paymentMapper::toDto);
        } catch (IllegalArgumentException e) {
            try {
                PaymentStatus status = PaymentStatus.valueOf(pathVariable.toUpperCase());
                return paymentRepository.findAllByStatus(status).map(paymentMapper::toDto);
            } catch (IllegalArgumentException ex) {
                return Flux.error(new BadPathVariableForGetAllPaymentsByException(
                        "Bad path variable: %s".formatted(pathVariable)));
            }
        }
    }

    public Flux<PaymentDto> getAllByStatus(String status){
        try {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            return paymentRepository.findAllByStatus(paymentStatus).map(paymentMapper::toDto);
        } catch (IllegalArgumentException ex) {
            return Flux.error(new BadPathVariableForGetAllPaymentsByException(
                    "No such status: %s".formatted(status)));
        }
    }

    public Flux<PaymentDto> getAllByUserId(UUID userId){
        return paymentRepository.findAllByUserId(userId).map(paymentMapper::toDto);
    }

    public Flux<PaymentDto> getAllByOrderId(UUID orderId){
        return paymentRepository.findAllByOrderId(orderId).map(paymentMapper::toDto);
    }

    public Mono<PaymentSumResult> getSumOfPaymentsForUserWithDateRange(UUID userId, DateRangeRequest range){
        return paymentRepository.getSumOfPaymentsForUserWithDateRange(userId, range.from(), range.to()).map(sum -> {
            System.out.println(sum);
            return sum;
        });
    }

    public Mono<PaymentSumResult> getSumOfPaymentsForAllUsersWithDateRange(DateRangeRequest range){
        return paymentRepository.getSumOfPaymentsForAllUsersWithDateRange(range.from(), range.to()).map(sum -> {
            System.out.println(sum);
            return sum;
        });
    }

    public Mono<UUID> findPaymentOwnerById(String id) {
        return paymentRepository.findPaymentOwnerById(id).switchIfEmpty(
                Mono.error(new PaymentNotFoundException("Payment with id: %s is not found".formatted(id)))
        );
    }

    public Mono<UUID> findPaymentOwnerByOrderId(UUID orderId) {
        return paymentRepository.findPaymentOwnerByOrderId(orderId).switchIfEmpty(
                Mono.error(new PaymentNotFoundException("Payment with order_id: %s is not found".formatted(orderId)))
        );
    }

}
