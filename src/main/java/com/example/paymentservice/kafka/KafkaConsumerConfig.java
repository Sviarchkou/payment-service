package com.example.paymentservice.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${app.kafka.endpoint}")
    private String kafkaEndpoint;

    @Bean
    public ConsumerFactory<String, OrderKafkaMessage> orderToPayConsumerFactory() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaEndpoint);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "order_to_pay_group");

        JsonDeserializer<OrderKafkaMessage> deserializer = new JsonDeserializer<>(OrderKafkaMessage.class);

        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderKafkaMessage> orderToPayKafkaListenerContainerFactory(
            ConsumerFactory<String, OrderKafkaMessage> consumerFactory
    ) {
        var containerFactory = new ConcurrentKafkaListenerContainerFactory<String, OrderKafkaMessage>();
        containerFactory.setConsumerFactory(consumerFactory);
        containerFactory.setConcurrency(1);
        return containerFactory;
    }
}
