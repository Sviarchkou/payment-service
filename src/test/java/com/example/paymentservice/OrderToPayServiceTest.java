package com.example.paymentservice;

import com.example.paymentservice.dto.PaymentDto;
import com.example.paymentservice.entity.OrderToPay;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.kafka.OrderKafkaMessage;
import com.example.paymentservice.mapper.OrderToPayMapper;
import com.example.paymentservice.repository.OrderToPayRepository;
import com.example.paymentservice.service.OrderToPayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderToPayServiceTest {

    @Mock
    private OrderToPayRepository orderToPayRepository;

    @Mock
    private OrderToPayMapper orderToPayMapper;

    @InjectMocks
    OrderToPayService orderToPayService;

    private UUID userId;
    private UUID orderId;
    private final BigDecimal TOTAL_PRICE = new BigDecimal("1234.56");
    private OrderToPay orderToPay;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        orderToPay = new OrderToPay();
        orderToPay.setOrderId(orderId);
        orderToPay.setUserId(userId);
        orderToPay.setTotalPrice(TOTAL_PRICE);
    }

    @Test
    void shouldSaveOrderToPaySuccessfully() {
        OrderKafkaMessage mes = new OrderKafkaMessage(orderId, userId, TOTAL_PRICE);
        when(orderToPayMapper.toEntity(mes)).thenReturn(orderToPay);
        when(orderToPayRepository.save(orderToPay)).thenReturn(Mono.just(orderToPay));

        StepVerifier.create(orderToPayService.save(mes))
                .expectNext(orderToPay)
                .verifyComplete();
    }

    @Test
    void shouldReturnOrderToPayByOrderIdSuccessfully() {
        when(orderToPayRepository.findByOrderId(orderId)).thenReturn(Mono.just(orderToPay));

        StepVerifier.create(orderToPayService.getByOrderId(orderId))
                .expectNext(orderToPay)
                .verifyComplete();
    }

    @Test
    void shouldRemoveOrderToPayByOrderIdSuccessfully() {
        when(orderToPayRepository.deleteAllByOrderId(orderId)).thenReturn(Mono.empty());

        StepVerifier.create(orderToPayService.deleteByOrderId(orderId))
                .expectNext()
                .verifyComplete();
    }

}
