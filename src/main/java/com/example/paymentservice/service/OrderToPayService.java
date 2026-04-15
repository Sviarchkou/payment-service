package com.example.paymentservice.service;

import com.example.paymentservice.entity.OrderToPay;
import com.example.paymentservice.kafka.OrderKafkaMessage;
import com.example.paymentservice.mapper.OrderToPayMapper;
import com.example.paymentservice.repository.OrderToPayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderToPayService {

    private final OrderToPayRepository orderToPayRepository;
    private final OrderToPayMapper orderToPayMapper;

    public Mono<OrderToPay> save(OrderKafkaMessage orderKafkaMessage) {
        return orderToPayRepository.save(orderToPayMapper.toEntity(orderKafkaMessage));
    }

    public Mono<OrderToPay> getByOrderId(UUID orderId) {
        return orderToPayRepository.findByOrderId(orderId);
    }

    public Mono<Void> deleteByOrderId(UUID orderId) {
        return orderToPayRepository.deleteAllByOrderId(orderId);
    }
}
