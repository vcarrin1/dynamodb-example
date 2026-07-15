package com.vcarrin87.dynamodb_example.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.vcarrin87.dynamodb_example.models.InventoryItem;
import com.vcarrin87.dynamodb_example.models.Keys;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Repository
public class InventoryRepository {

    private static final String INVENTORY_TABLE = "InventoryTable";

    private final DynamoDbEnhancedClient enhancedClient;

    public InventoryRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    public void save(InventoryItem inventory) {
        DynamoDbTable<InventoryItem> table = enhancedClient.table(INVENTORY_TABLE, TableSchema.fromBean(InventoryItem.class));
        table.putItem(inventory);
    }

    public InventoryItem findByProductId(UUID productId) {
        DynamoDbTable<InventoryItem> table = enhancedClient.table(INVENTORY_TABLE, TableSchema.fromBean(InventoryItem.class));
        return table.getItem(r -> r.key(Key.builder()
                .partitionValue(Keys.productPk(productId.toString()))
                .sortValue(Keys.inventorySk())
                .build()));
    }

    public void deleteInventoryByProductId(UUID productId) {
        DynamoDbTable<InventoryItem> table = enhancedClient.table(INVENTORY_TABLE, TableSchema.fromBean(InventoryItem.class));
        table.deleteItem(r -> r.key(Key.builder()
                .partitionValue(Keys.productPk(productId.toString()))
                .sortValue(Keys.inventorySk())
                .build()));
    }

    public List<InventoryItem> findInStock() {
        DynamoDbTable<InventoryItem> table = enhancedClient.table(INVENTORY_TABLE, TableSchema.fromBean(InventoryItem.class));

        Map<String, AttributeValue> values = Map.of(
                ":zero", AttributeValue.builder().n("0").build());

        Expression filterExpression = Expression.builder()
                .expression("StockLevel > :zero")
                .expressionValues(values)
                .build();

        ScanEnhancedRequest req = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .build();

        return table.scan(req).items().stream().collect(Collectors.toList());
    }
}
