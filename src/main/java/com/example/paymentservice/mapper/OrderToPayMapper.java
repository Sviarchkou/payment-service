package com.example.paymentservice.mapper;

import com.example.paymentservice.entity.OrderToPay;
import com.example.paymentservice.kafka.OrderKafkaMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderToPayMapper {

    OrderToPay toEntity(OrderKafkaMessage orderKafkaMessage);

}
