package com.vcarrin87.dynamodb_example.repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.vcarrin87.dynamodb_example.models.OrderItem;

import lombok.extern.slf4j.Slf4j;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;

@Slf4j
@Repository
public class OrderRepository {

    private static final String ORDER_TABLE = "OrderTable";

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbClient dynamoDbClient;

    /**
     * Creates the order repository.
     *
     * @param enhancedClient enhanced DynamoDB client
     * @param dynamoDbClient base DynamoDB client
     */
    public OrderRepository(DynamoDbEnhancedClient enhancedClient, DynamoDbClient dynamoDbClient) {
        this.enhancedClient = enhancedClient;
        this.dynamoDbClient = dynamoDbClient;
    }

    /**
     * Returns orders for a customer using a GSI when available.
     *
     * @param customerId customer identifier
     * @return matching order items
     */
    public List<OrderItem> listOrdersForCustomer(String customerId) {
        final String indexName = "CustomerIdIndex";
        if (hasGsi(ORDER_TABLE, indexName)) {
            DynamoDbIndex<OrderItem> index = enhancedClient.table(ORDER_TABLE, TableSchema.fromBean(OrderItem.class)).index(indexName);
            QueryConditional qc = QueryConditional.keyEqualTo(Key.builder().partitionValue(customerId).build());
            return index.query(QueryEnhancedRequest.builder().queryConditional(qc).build())
                    .stream()
                    .flatMap(page -> page.items().stream())
                    .collect(Collectors.toList());
        }

        DynamoDbTable<OrderItem> table = enhancedClient.table(ORDER_TABLE, TableSchema.fromBean(OrderItem.class));
        Map<String, AttributeValue> values = Map.of(
                ":customerId", AttributeValue.builder().s(customerId).build());

        Expression filterExpression = Expression.builder()
                .expression("CustomerId = :customerId")
                .expressionValues(values)
                .build();

        ScanEnhancedRequest req = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .build();

        return table.scan(req).items().stream().collect(Collectors.toList());
    }

    /**
     * Persists a single order item.
     *
     * @param order order to save
     */
    public void saveOrder(OrderItem order) {
        DynamoDbTable<OrderItem> table = enhancedClient.table(ORDER_TABLE, TableSchema.fromBean(OrderItem.class));
        table.putItem(order);
    }

    /**
     * Checks whether a table contains a named global secondary index.
     *
     * @param tableName table name
     * @param indexName index name
     * @return true when the index exists
     */
    private boolean hasGsi(String tableName, String indexName) {
        try {
            DescribeTableRequest req = DescribeTableRequest.builder().tableName(tableName).build();
            DescribeTableResponse resp = dynamoDbClient.describeTable(req);
            return resp.table().globalSecondaryIndexes() != null && resp.table().globalSecondaryIndexes().stream().anyMatch(idx -> idx.indexName().equals(indexName));
        } catch (Exception e) {
            return false;
        }
    }
}
