package com.example.paymentservice;

import com.example.paymentservice.client.PaymentSimulationClient;
import com.example.paymentservice.dto.PaymentDto;
import com.example.paymentservice.entity.OrderToPay;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.exception.BadPathVariableForGetAllPaymentsByException;
import com.example.paymentservice.exception.OrderToPayNotFoundException;
import com.example.paymentservice.exception.PaymentNotFoundException;
import com.example.paymentservice.kafka.PaymentKafkaProducer;
import com.example.paymentservice.mapper.PaymentMapper;
import com.example.paymentservice.reponse.PaymentSumResult;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.request.DateRangeRequest;
import com.example.paymentservice.service.OrderToPayService;
import com.example.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private PaymentSimulationClient paymentSimulationClient;
    @Mock
    private PaymentKafkaProducer paymentKafkaProducer;
    @Mock
    private OrderToPayService orderToPayService;

    @InjectMocks
    private PaymentService paymentService;

    private Payment payment;
    private PaymentDto paymentDto;
    private String id;
    private UUID userId;
    private UUID orderId;
    private final Pageable PAGEABLE = PageRequest.of(0, 20);
    ;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID().toString();
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        payment = new Payment();
        payment.setId(id);

        paymentDto = new PaymentDto();
    }

    @Test
    void makePayment_success() {
        PaymentStatus status = PaymentStatus.SUCCESS;
        OrderToPay otp = new OrderToPay();
        otp.setOrderId(orderId);
        otp.setUserId(userId);
        otp.setTotalPrice(new BigDecimal("123.45"));

        paymentDto.setOrderId(orderId);
        paymentDto.setUserId(userId);

        when(orderToPayService.getByOrderId(paymentDto.getOrderId())).thenReturn(Mono.just(otp));
        when(paymentSimulationClient.simulatePayment()).thenReturn(Mono.just(status));
        when(paymentMapper.toEntity(paymentDto)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(Mono.just(payment));
        when(paymentMapper.toDto(payment)).thenReturn(paymentDto);
        when(paymentKafkaProducer.sendPaymentToKafka(paymentDto)).thenReturn(Mono.just(paymentDto));

        StepVerifier.create(paymentService.makePayment(paymentDto))
                .expectNext(paymentDto)
                .verifyComplete();
    }

    @Test
    void makePayment_orderToPayNotFound() {

        when(orderToPayService.getByOrderId(paymentDto.getOrderId())).thenReturn(Mono.empty());

        StepVerifier.create(paymentService.makePayment(paymentDto))
                .expectError(OrderToPayNotFoundException.class)
                .verify();
    }

    @Test
    void save_success() {
        when(paymentMapper.toEntity(paymentDto)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(Mono.just(payment));
        when(paymentMapper.toDto(payment)).thenReturn(paymentDto);

        StepVerifier.create(paymentService.save(paymentDto, PaymentStatus.SUCCESS))
                .expectNext(paymentDto)
                .verifyComplete();
    }

    @Test
    void getById_success() {
        when(paymentRepository.findById(id)).thenReturn(Mono.just(payment));
        when(paymentMapper.toDto(payment)).thenReturn(paymentDto);

        StepVerifier.create(paymentService.getById(id))
                .expectNext(paymentDto)
                .verifyComplete();
    }

    @Test
    void getById_notFound() {
        when(paymentRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(paymentService.getById(id))
                .expectError(PaymentNotFoundException.class)
                .verify();
    }

    @Test
    void getAll_success() {
        when(paymentRepository.findAllBy(PAGEABLE)).thenReturn(Flux.just(payment));
        when(paymentMapper.toDto(payment)).thenReturn(paymentDto);

        StepVerifier.create(paymentService.getAll(PAGEABLE))
                .expectNext(paymentDto)
                .verifyComplete();
    }

    @Test
    void getAllBy_uuid() {
        UUID id = UUID.randomUUID();

        when(paymentRepository.findAllByUserIdOrOrderId(id, id, PAGEABLE)).thenReturn(Flux.just(payment));
        when(paymentMapper.toDto(payment)).thenReturn(paymentDto);

        StepVerifier.create(paymentService.getAllBy(id.toString(), PAGEABLE))
                .expectNext(paymentDto)
                .verifyComplete();
    }

    @Test
    void getAllBy_status() {
        when(paymentRepository.findAllByStatus(any(), eq(PAGEABLE))).thenReturn(Flux.just(payment));
        when(paymentMapper.toDto(payment)).thenReturn(paymentDto);

        StepVerifier.create(paymentService.getAllBy("success", PAGEABLE))
                .expectNext(paymentDto)
                .verifyComplete();
    }

    @Test
    void getAllBy_badPathVariable() {
        StepVerifier.create(paymentService.getAllBy("wrong_value", PAGEABLE))
                .expectError(BadPathVariableForGetAllPaymentsByException.class)
                .verify();
    }

    @Test
    void getAllByStatus_success() {
        when(paymentRepository.findAllByStatus(any(), eq(PAGEABLE))).thenReturn(Flux.just(payment));
        when(paymentMapper.toDto(payment)).thenReturn(paymentDto);

        StepVerifier.create(paymentService.getAllByStatus("success", PAGEABLE))
                .expectNext(paymentDto)
                .verifyComplete();
    }

    @Test
    void getAllByStatus_invalid() {
        StepVerifier.create(paymentService.getAllByStatus("invalid", PAGEABLE))
                .expectError(BadPathVariableForGetAllPaymentsByException.class)
                .verify();
    }

    @Test
    void getAllByUserId_success() {
        when(paymentRepository.findAllByUserId(userId, PAGEABLE)).thenReturn(Flux.just(payment));
        when(paymentMapper.toDto(payment)).thenReturn(paymentDto);

        StepVerifier.create(paymentService.getAllByUserId(userId, PAGEABLE))
                .expectNext(paymentDto)
                .verifyComplete();
    }

    @Test
    void getAllByOrderId_success() {
        when(paymentRepository.findAllByOrderId(orderId, PAGEABLE)).thenReturn(Flux.just(payment));
        when(paymentMapper.toDto(payment)).thenReturn(paymentDto);

        StepVerifier.create(paymentService.getAllByOrderId(orderId, PAGEABLE))
                .expectNext(paymentDto)
                .verifyComplete();
    }

    @Test
    void getSumOfPaymentsForUserWithDateRange_success() {
        PaymentSumResult result = new PaymentSumResult(new BigDecimal("1234.34"));
        DateRangeRequest range = new DateRangeRequest(
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-11-30T23:59:59Z")
        );

        when(paymentRepository.getSumOfPaymentsForUserWithDateRange(userId, range.from(), range.to()))
                .thenReturn(Mono.just(result));

        StepVerifier.create(paymentService.getSumOfPaymentsForUserWithDateRange(userId, range))
                .expectNext(result)
                .verifyComplete();
    }

    @Test
    void getSumOfPaymentsForAllUsersWithDateRange_success() {
        PaymentSumResult result = new PaymentSumResult(new BigDecimal("1234.34"));
        DateRangeRequest range = new DateRangeRequest(
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-11-30T23:59:59Z")
        );

        when(paymentRepository.getSumOfPaymentsForAllUsersWithDateRange(range.from(), range.to()))
                .thenReturn(Mono.just(result));

        StepVerifier.create(paymentService.getSumOfPaymentsForAllUsersWithDateRange(range))
                .expectNext(result)
                .verifyComplete();
    }

    @Test
    void findPaymentOwnerById_success() {
        when(paymentRepository.findPaymentOwnerById(id)).thenReturn(Mono.just(userId));

        StepVerifier.create(paymentService.findPaymentOwnerById(id))
                .expectNext(userId)
                .verifyComplete();
    }

    @Test
    void findPaymentOwnerById_notFound() {
        when(paymentRepository.findPaymentOwnerById(id)).thenReturn(Mono.empty());

        StepVerifier.create(paymentService.findPaymentOwnerById(id))
                .expectError(PaymentNotFoundException.class)
                .verify();
    }

    @Test
    void findPaymentOwnerByOrderId_success() {
        when(paymentRepository.findPaymentOwnerByOrderId(orderId)).thenReturn(Mono.just(userId));

        StepVerifier.create(paymentService.findPaymentOwnerByOrderId(orderId))
                .expectNext(userId)
                .verifyComplete();
    }

    @Test
    void findPaymentOwnerByOrderId_notFound() {
        when(paymentRepository.findPaymentOwnerByOrderId(orderId)).thenReturn(Mono.empty());

        StepVerifier.create(paymentService.findPaymentOwnerByOrderId(orderId))
                .expectError(PaymentNotFoundException.class)
                .verify();
    }


}
