package com.example.paymentservice.client;

import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.exception.PaymentSimulationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class PaymentSimulationClient {

    @Value("${app.external-api.integer-generator-url}")
    private String apiURI;

    private final WebClient webClient = WebClient.builder().build();

    public Mono<PaymentStatus> simulatePayment(){
        return webClient.get()
                .uri(apiURI)
                .retrieve()
                .bodyToMono(String.class)
                .map(str -> {
                    var num = str.strip().replaceAll("\n","").replaceAll("\r","");
                    return Integer.parseInt(num);
                })
                .map(number -> {
                    if (number % 2 == 0)
                        return PaymentStatus.SUCCESS;
                    else
                        return PaymentStatus.FAILED;
                })
                .onErrorResume(error -> {
                    log.error("Error while simulating payment: {}", error.getMessage());
                    return Mono.error(new PaymentSimulationException("Error has occurred during payment process"));
                });
    }

}
