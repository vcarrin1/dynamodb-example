package com.vcarrin87.dynamodb_example.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.vcarrin87.dynamodb_example.models.OrderLineItem;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Repository
public class OrderItemRepository {

    private static final String ORDER_LINE_TABLE = "OrderLineTable";

    private final DynamoDbEnhancedClient enhancedClient;

    public OrderItemRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    public void save(OrderLineItem orderItem) {
        DynamoDbTable<OrderLineItem> table = enhancedClient.table(ORDER_LINE_TABLE, TableSchema.fromBean(OrderLineItem.class));
        table.putItem(orderItem);
    }

    public List<OrderLineItem> findAll() {
        DynamoDbTable<OrderLineItem> table = enhancedClient.table(ORDER_LINE_TABLE, TableSchema.fromBean(OrderLineItem.class));
        return table.scan().items().stream().collect(Collectors.toList());
    }

    public List<OrderLineItem> findByOrderId(UUID orderId) {
        DynamoDbTable<OrderLineItem> table = enhancedClient.table(ORDER_LINE_TABLE, TableSchema.fromBean(OrderLineItem.class));

        Map<String, AttributeValue> values = Map.of(
                ":orderId", AttributeValue.builder().s(orderId.toString()).build());

        Expression filterExpression = Expression.builder()
                .expression("OrderId = :orderId")
                .expressionValues(values)
                .build();

        ScanEnhancedRequest req = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .build();

        return table.scan(req).items().stream().collect(Collectors.toList());
    }

    public List<OrderLineItem> findByProductId(UUID productId) {
        DynamoDbTable<OrderLineItem> table = enhancedClient.table(ORDER_LINE_TABLE, TableSchema.fromBean(OrderLineItem.class));

        Map<String, AttributeValue> values = Map.of(
                ":productId", AttributeValue.builder().s(productId.toString()).build());

        Expression filterExpression = Expression.builder()
                .expression("ProductId = :productId")
                .expressionValues(values)
                .build();

        ScanEnhancedRequest req = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .build();

        return table.scan(req).items().stream().collect(Collectors.toList());
    }

    public void deleteOrderItemByProductId(UUID productId) {
        DynamoDbTable<OrderLineItem> table = enhancedClient.table(ORDER_LINE_TABLE, TableSchema.fromBean(OrderLineItem.class));
        List<OrderLineItem> items = findByProductId(productId);
        for (OrderLineItem item : items) {
            table.deleteItem(item);
        }
    }
}
