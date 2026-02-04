package com.example.paymentservice.kafka;

import com.example.paymentservice.entity.OrderToPay;
import com.example.paymentservice.mapper.OrderToPayMapper;
import com.example.paymentservice.service.OrderToPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderKafkaConsumer {

    private final OrderToPayService orderToPayService;

    @KafkaListener(
            topics = "orders-to-pay",
            groupId = "order_to_pay_group",
            containerFactory = "orderToPayKafkaListenerContainerFactory"
    )
    public void consumeOrder(OrderKafkaMessage orderKafkaMessage) {
//        Mono.defer(() -> orderToPayService.deleteByOrderId(orderKafkaMessage.orderId())
//                .then(Mono.defer(() -> orderToPayService.save(orderKafkaMessage)))
//                .doOnSuccess(orderToPay -> {
//                    log.info("OrderToPay object: {} saved successfully ", orderToPay);
//                    acknowledgment.acknowledge();
//                })
//                .doOnError(e -> log.error("Error during saving OrderToPay object: {} ", orderKafkaMessage, e)));

//        process(orderKafkaMessage)
//                .doOnSuccess(v -> log.info("Order-to-pay {} saved successfully", orderKafkaMessage))
//                .doOnError(e -> log.error("Kafka processing error", e))
//                .subscribe();
        try {
            process(orderKafkaMessage).block();
            log.info("Order-to-pay {} saved successfully", orderKafkaMessage);
        } catch (Exception e) {
            log.error("Error during saving OrderToPay object: {} ", orderKafkaMessage, e);
            // Здесь можно выбросить исключение, чтобы сработал ErrorHandler/Retry
            throw e;
        }
    }

    public Mono<Void> process(OrderKafkaMessage msg) {
        return orderToPayService.deleteByOrderId(msg.orderId())
                .then(orderToPayService.save(msg))
                .then();
    }

/*
    public Mono<OrderToPay> processConsumption(OrderKafkaMessage orderKafkaMessage) {
        return Mono.defer(() -> orderToPayService.deleteByOrderId(orderKafkaMessage.orderId())
                .then(Mono.defer(() -> orderToPayService.save(orderKafkaMessage)));
    }*/
}
