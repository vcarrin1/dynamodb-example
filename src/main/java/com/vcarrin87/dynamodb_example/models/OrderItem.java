package com.vcarrin87.dynamodb_example.models;

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
public class OrderItem {

    @Getter(onMethod_ = {@DynamoDbAttribute("PKey"), @DynamoDbPartitionKey})
    @Setter(onMethod_ = {@DynamoDbAttribute("PKey")})
    String pKey;

    @Getter(onMethod_ = {@DynamoDbAttribute("SKey"), @DynamoDbSortKey})
    @Setter(onMethod_ = {@DynamoDbAttribute("SKey")})
    String sKey;

    @Getter(onMethod_ = {@DynamoDbAttribute("EntityType")})
    @Setter(onMethod_ = {@DynamoDbAttribute("EntityType")})
    String entityType;

    @Getter(onMethod_ = {@DynamoDbAttribute("OrderId")})
    @Setter(onMethod_ = {@DynamoDbAttribute("OrderId")})
    UUID orderId;

    @Getter(onMethod_ = {@DynamoDbAttribute("CustomerId")})
    @Setter(onMethod_ = {@DynamoDbAttribute("CustomerId")})
    String customerId;

    @Getter(onMethod_ = {@DynamoDbAttribute("OrderStatus")})
    @Setter(onMethod_ = {@DynamoDbAttribute("OrderStatus")})
    String orderStatus;

    @Getter(onMethod_ = {@DynamoDbAttribute("DeliveryDate")})
    @Setter(onMethod_ = {@DynamoDbAttribute("DeliveryDate")})
    Instant deliveryDate;

    @Getter(onMethod_ = {@DynamoDbAttribute("CreatedAt")})
    @Setter(onMethod_ = {@DynamoDbAttribute("CreatedAt")})
    Instant createdAt;
}
