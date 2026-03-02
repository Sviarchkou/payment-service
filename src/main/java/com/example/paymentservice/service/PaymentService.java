package com.example.paymentservice.service;

import com.example.paymentservice.client.PaymentSimulationClient;
import com.example.paymentservice.dto.PaymentDto;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.exception.BadPathVariableForGetAllPaymentsByException;
import com.example.paymentservice.exception.OrderToPayNotFoundException;
import com.example.paymentservice.exception.PaymentNotFoundException;
import com.example.paymentservice.exception.WrongUserIdException;
import com.example.paymentservice.kafka.PaymentKafkaProducer;
import com.example.paymentservice.mapper.PaymentMapper;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.reponse.PaymentSumResult;
import com.example.paymentservice.request.DateRangeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
    private final OrderToPayService orderToPayService;

    public Mono<PaymentDto> makePayment(PaymentDto paymentDto) {
        return orderToPayService.getByOrderId(paymentDto.getOrderId())
                .switchIfEmpty(Mono.error(
                        new OrderToPayNotFoundException("Order with id: %s is not found in pending to pay list".formatted(paymentDto.getOrderId()))))
                .flatMap(orderToPay -> {
                    if (!orderToPay.getUserId().equals(paymentDto.getUserId())){
                        return Mono.error(new WrongUserIdException("User with id: %s can not pay this order".formatted(paymentDto.getUserId())));
                    }
                    paymentDto.setPaymentAmount(orderToPay.getTotalPrice());

                    return paymentSimulationClient.simulatePayment()
                            .flatMap(paymentStatus -> save(paymentDto, paymentStatus))
                            .flatMap(paymentKafkaProducer::sendPaymentToKafka);
                });
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

    public Flux<PaymentDto> getAll(Pageable pageable) {
        return paymentRepository.findAllBy(pageable).map(paymentMapper::toDto);
    }

    public Flux<PaymentDto> getAllBy(String pathVariable, Pageable pageable){
        try {
            UUID id = UUID.fromString(pathVariable);
            return paymentRepository.findAllByUserIdOrOrderId(id, id, pageable).map(paymentMapper::toDto);
        } catch (IllegalArgumentException e) {
            try {
                PaymentStatus status = PaymentStatus.valueOf(pathVariable.toUpperCase());
                return paymentRepository.findAllByStatus(status, pageable).map(paymentMapper::toDto);
            } catch (IllegalArgumentException ex) {
                return Flux.error(new BadPathVariableForGetAllPaymentsByException(
                        "Bad path variable: %s".formatted(pathVariable)));
            }
        }
    }

    public Flux<PaymentDto> getAllByStatus(String status, Pageable pageable){
        try {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            return paymentRepository.findAllByStatus(paymentStatus, pageable).map(paymentMapper::toDto);
        } catch (IllegalArgumentException ex) {
            return Flux.error(new BadPathVariableForGetAllPaymentsByException(
                    "No such status: %s".formatted(status)));
        }
    }

    public Flux<PaymentDto> getAllByUserId(UUID userId, Pageable pageable){
        return paymentRepository.findAllByUserId(userId, pageable).map(paymentMapper::toDto);
    }

    public Flux<PaymentDto> getAllByOrderId(UUID orderId, Pageable pageable){
        return paymentRepository.findAllByOrderId(orderId, pageable).map(paymentMapper::toDto);
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
