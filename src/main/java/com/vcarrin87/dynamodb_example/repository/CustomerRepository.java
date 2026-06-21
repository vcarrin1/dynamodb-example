package com.vcarrin87.dynamodb_example.repository;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.vcarrin87.dynamodb_example.models.CustomerItem;
import com.vcarrin87.dynamodb_example.models.CustomerPage;
import com.vcarrin87.dynamodb_example.models.EntityType;

import lombok.extern.slf4j.Slf4j;

import com.vcarrin87.dynamodb_example.models.Keys;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Slf4j
@Repository
public class CustomerRepository {

    private static final String CUSTOMER_TABLE = "CustomerTable";
    /**
     * Enhanced DynamoDB client used for typed table and index access.
     */
    private final DynamoDbEnhancedClient enhancedClient;

    /**
     * Standard DynamoDB client used to describe table metadata and detect GSIs.
     */
    private final DynamoDbClient dynamoDbClient;

    /**
     * Creates the customer repository.
     *
     * @param enhancedClient enhanced DynamoDB client
     * @param dynamoDbClient base DynamoDB client
     */
    public CustomerRepository(DynamoDbEnhancedClient enhancedClient, DynamoDbClient dynamoDbClient) {
        this.enhancedClient = enhancedClient;
        this.dynamoDbClient = dynamoDbClient;
    }

    /**
     * Load a single customer profile row directly by primary key.
     * This uses the table partition key and sort key defined by the model.
     */
    public CustomerItem getCustomerProfile(String customerId) {
        log.info("Loading customer details {}", customerId);
        String pk = Keys.customerPk(customerId);
        DynamoDbTable<CustomerItem> table = enhancedClient.table(CUSTOMER_TABLE, TableSchema.fromBean(CustomerItem.class));
        return table.getItem(r -> r.key(Key.builder().partitionValue(pk).sortValue(Keys.customerProfileSk()).build()));
    }

    /**
     * Returns a paginated customer page with optional filter predicates.
     *
     * @param pageSize page size
     * @param nextToken pagination cursor from the previous page
     * @param customerId optional customer ID filter
     * @param createdAt optional created-at filter
     * @param name optional partial name filter using contains
     * @return customer page
     */
    public CustomerPage listCustomers(int pageSize, String nextToken, String customerId, String createdAt, String name) {
        DynamoDbTable<CustomerItem> table = enhancedClient.table(CUSTOMER_TABLE, TableSchema.fromBean(CustomerItem.class));

        Map<String, AttributeValue> values = new HashMap<>();
        Map<String, String> expressionNames = new HashMap<>();
        values.put(":entityType", AttributeValue.builder().s(EntityType.CUSTOMER.name()).build());
        List<String> clauses = new ArrayList<>();
        clauses.add("EntityType = :entityType");

        if (customerId != null && !customerId.isBlank()) {
            values.put(":customerId", AttributeValue.builder().s(customerId).build());
            clauses.add("CustomerId = :customerId");
        }

        if (createdAt != null && !createdAt.isBlank()) {
            values.put(":createdAt", AttributeValue.builder().s(createdAt).build());
            clauses.add("CreatedAt = :createdAt");
        }

        if (name != null && !name.isBlank()) {
            values.put(":name", AttributeValue.builder().s(name).build());
            expressionNames.put("#name", "Name");
            clauses.add("contains(#name, :name)");
        }

        Expression.Builder expressionBuilder = Expression.builder()
                .expression(String.join(" AND ", clauses))
                .expressionValues(values);

        if (!expressionNames.isEmpty()) {
            expressionBuilder.expressionNames(expressionNames);
        }

        Expression filterExpression = expressionBuilder.build();

        ScanEnhancedRequest.Builder requestBuilder = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .limit(pageSize);

        if (nextToken != null && !nextToken.isBlank()) {
            requestBuilder.exclusiveStartKey(decodeExclusiveStartKey(nextToken));
        }

        Page<CustomerItem> page = table.scan(requestBuilder.build()).iterator().next();
        String newNextToken = encodeExclusiveStartKey(page.lastEvaluatedKey());

        return CustomerPage.builder()
                .items(page.items())
                .nextToken(newNextToken)
                .build();
    }

    /**
     * Decodes a next token into DynamoDB exclusive start key values.
     *
     * @param nextToken encoded pagination token
     * @return exclusive start key map
     */
    private Map<String, AttributeValue> decodeExclusiveStartKey(String nextToken) {
        byte[] decoded = Base64.getDecoder().decode(nextToken);
        String decodedValue = new String(decoded, StandardCharsets.UTF_8);
        String[] parts = decodedValue.split("\n", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid nextToken");
        }
        Map<String, AttributeValue> startKey = new HashMap<>();
        startKey.put("PKey", AttributeValue.builder().s(parts[0]).build());
        startKey.put("SKey", AttributeValue.builder().s(parts[1]).build());
        return startKey;
    }

    /**
     * Encodes a DynamoDB last evaluated key as a next token.
     *
     * @param lastEvaluatedKey last evaluated key returned by scan
     * @return encoded next token or null when paging is complete
     */
    private String encodeExclusiveStartKey(Map<String, AttributeValue> lastEvaluatedKey) {
        if (lastEvaluatedKey == null || lastEvaluatedKey.isEmpty()) {
            return null;
        }
        AttributeValue pKey = lastEvaluatedKey.get("PKey");
        AttributeValue sKey = lastEvaluatedKey.get("SKey");
        if (pKey == null || sKey == null) {
            return null;
        }
        String encoded = pKey.s() + "\n" + sKey.s();
        return Base64.getEncoder().encodeToString(encoded.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Save a new customer profile to the table. This is a simple putItem using the enhanced client.
     * @param newCustomer
     */
    public void saveCustomer(CustomerItem newCustomer) {
        // save new customer
        DynamoDbTable<CustomerItem> table = enhancedClient.table(CUSTOMER_TABLE, TableSchema.fromBean(CustomerItem.class));
        table.putItem(newCustomer);
    }

}
