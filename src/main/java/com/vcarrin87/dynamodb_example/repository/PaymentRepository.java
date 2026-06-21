package com.vcarrin87.dynamodb_example.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.vcarrin87.dynamodb_example.models.PaymentItem;

import lombok.extern.slf4j.Slf4j;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
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
public class PaymentRepository {

    private static final String PAYMENT_TABLE = "PaymentTable";

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbClient dynamoDbClient;

    /**
     * Creates the payment repository.
     *
     * @param enhancedClient enhanced DynamoDB client
     * @param dynamoDbClient base DynamoDB client
     */
    public PaymentRepository(DynamoDbEnhancedClient enhancedClient, DynamoDbClient dynamoDbClient) {
        this.enhancedClient = enhancedClient;
        this.dynamoDbClient = dynamoDbClient;
    }

    /**
     * Returns payments for the provided order IDs.
     *
     * @param orderIds order identifiers
     * @return matching payment items
     */
    public List<PaymentItem> listPaymentsForOrderIds(List<UUID> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }

        final String indexName = "OrderIdIndex";
        if (hasGsi(PAYMENT_TABLE, indexName)) {
            DynamoDbIndex<PaymentItem> index = enhancedClient.table(PAYMENT_TABLE, TableSchema.fromBean(PaymentItem.class)).index(indexName);
            if (orderIds.size() == 1) {
                QueryConditional qc = QueryConditional.keyEqualTo(software.amazon.awssdk.enhanced.dynamodb.Key.builder().partitionValue(orderIds.get(0).toString()).build());
                return index.query(QueryEnhancedRequest.builder().queryConditional(qc).build())
                        .stream()
                        .flatMap(page -> page.items().stream())
                        .collect(Collectors.toList());
            }
        }

        DynamoDbTable<PaymentItem> table = enhancedClient.table(PAYMENT_TABLE, TableSchema.fromBean(PaymentItem.class));
        Map<String, AttributeValue> values = orderIds.stream()
                .collect(Collectors.toMap(id -> ":orderId" + orderIds.indexOf(id), id -> AttributeValue.builder().s(id.toString()).build()));

        String expression = orderIds.stream()
                .map(id -> ":orderId" + orderIds.indexOf(id))
                .collect(Collectors.joining(", "));

        Expression filterExpression = Expression.builder()
                .expression("OrderId IN (" + expression + ")")
                .expressionValues(values)
                .build();

        ScanEnhancedRequest req = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .build();

        return table.scan(req).items().stream().collect(Collectors.toList());
    }

    /**
     * Persists a single payment item.
     *
     * @param payment payment to save
     */
    public void savePayment(PaymentItem payment) {
        DynamoDbTable<PaymentItem> table = enhancedClient.table(PAYMENT_TABLE, TableSchema.fromBean(PaymentItem.class));
        table.putItem(payment);
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
