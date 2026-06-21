package com.vcarrin87.dynamodb_example.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class PaymentItem {

    @Getter(onMethod_ = {@DynamoDbAttribute("PKey"), @DynamoDbPartitionKey})
    @Setter(onMethod_ = {@DynamoDbAttribute("PKey")})
    String pKey;

    @Getter(onMethod_ = {@DynamoDbAttribute("SKey"), @DynamoDbSortKey})
    @Setter(onMethod_ = {@DynamoDbAttribute("SKey")})
    String sKey;

    @Getter(onMethod_ = {@DynamoDbAttribute("EntityType")})
    @Setter(onMethod_ = {@DynamoDbAttribute("EntityType")})
    String entityType;

    @Getter(onMethod_ = {@DynamoDbAttribute("PaymentId")})
    @Setter(onMethod_ = {@DynamoDbAttribute("PaymentId")})
    UUID paymentId;

    @Getter(onMethod_ = {@DynamoDbAttribute("OrderId")})
    @Setter(onMethod_ = {@DynamoDbAttribute("OrderId")})
    UUID orderId;

    @Getter(onMethod_ = {@DynamoDbAttribute("PaymentDate")})
    @Setter(onMethod_ = {@DynamoDbAttribute("PaymentDate")})
    Instant paymentDate;

    @Getter(onMethod_ = {@DynamoDbAttribute("Amount")})
    @Setter(onMethod_ = {@DynamoDbAttribute("Amount")})
    BigDecimal amount;

    @Getter(onMethod_ = {@DynamoDbAttribute("PaymentMethod")})
    @Setter(onMethod_ = {@DynamoDbAttribute("PaymentMethod")})
    String paymentMethod;
}
