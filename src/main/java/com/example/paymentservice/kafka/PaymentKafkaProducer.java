package com.example.paymentservice.kafka;

import com.example.paymentservice.dto.PaymentDto;
import com.example.paymentservice.exception.KafkaSendException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentKafkaProducer {

    private final KafkaTemplate<String, PaymentDto> kafkaTemplate;

    public Mono<PaymentDto> sendPaymentToKafka(PaymentDto dto) {
        return Mono.fromFuture(() ->
                        kafkaTemplate.send("payments", dto)//.get(5, TimeUnit.SECONDS)
                )
                .timeout(Duration.ofSeconds(5))
                //.subscribeOn(Schedulers.boundedElastic())
                .retryWhen(
                        Retry.backoff(5, Duration.ofMillis(200))
                                .filter(t -> {
                                    Throwable cause = Exceptions.unwrap(t);
                                    return cause instanceof RetriableException
                                            || cause instanceof TimeoutException; // from java.util.concurrent
                                })
                                .doBeforeRetry(rs ->
                                        log.warn("Retry Kafka send, attempt={}, reason={}",
                                                rs.totalRetries() + 1,
                                                rs.failure().toString()
                                        )
                                )
                                .onRetryExhaustedThrow((spec, signal) ->
                                        new KafkaSendException("Kafka send failed", signal.failure())
                                )
                )
                .thenReturn(dto);
    }


//    public Mono<PaymentDto> sendPaymentAToKafka(PaymentDto dto) {
//        return Mono.defer(() ->
//                        Mono.fromFuture(kafkaTemplate.send("payments", dto))
//                )
//                .timeout(Duration.ofSeconds(5))
//                .retryWhen(
//                        Retry.backoff(5, Duration.ofMillis(200))
//                                .filter(e -> e instanceof RetriableException
//                                        || e instanceof TimeoutException)
//                                .doBeforeRetry(rs ->
//                                        log.warn("Retry Kafka send, attempt={}, reason={}",
//                                                rs.totalRetries() + 1,
//                                                rs.failure().toString()
//                                        )
//                                )
//                                .onRetryExhaustedThrow((spec, signal) ->
//                                        new KafkaSendException("Kafka send failed", signal.failure())
//                                )
//                )
//                .thenReturn(dto);
//    }

}
