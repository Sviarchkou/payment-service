package com.example.paymentservice.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "orders-to-pay")
@CompoundIndex(def = "{ 'order_id': 1 }")
public class OrderToPay {
    @Id
    String id;

    @Field(name = "order_id")
    UUID orderId;

    @Field(name = "user_id")
    UUID userId;

    @Field(targetType = FieldType.DECIMAL128, name = "total_price")
    BigDecimal totalPrice;
}
