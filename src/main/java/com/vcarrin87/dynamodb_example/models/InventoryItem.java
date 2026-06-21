package com.vcarrin87.dynamodb_example.models;

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
public class InventoryItem {

    @Getter(onMethod_ = {@DynamoDbAttribute("PKey"), @DynamoDbPartitionKey})
    @Setter(onMethod_ = {@DynamoDbAttribute("PKey")})
    String pKey;

    @Getter(onMethod_ = {@DynamoDbAttribute("SKey"), @DynamoDbSortKey})
    @Setter(onMethod_ = {@DynamoDbAttribute("SKey")})
    String sKey;

    @Getter(onMethod_ = {@DynamoDbAttribute("EntityType")})
    @Setter(onMethod_ = {@DynamoDbAttribute("EntityType")})
    String entityType;

    @Getter(onMethod_ = {@DynamoDbAttribute("ProductId")})
    @Setter(onMethod_ = {@DynamoDbAttribute("ProductId")})
    UUID productId;

    @Getter(onMethod_ = {@DynamoDbAttribute("StockLevel")})
    @Setter(onMethod_ = {@DynamoDbAttribute("StockLevel")})
    Integer stockLevel;
}
