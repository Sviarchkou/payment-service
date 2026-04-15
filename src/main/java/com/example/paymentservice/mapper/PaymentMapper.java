package com.example.paymentservice.mapper;

import com.example.paymentservice.dto.PaymentDto;
import com.example.paymentservice.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    public Payment toEntity(PaymentDto paymentDto);

    public PaymentDto toDto(Payment payment);

}
