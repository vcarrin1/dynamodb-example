package com.vcarrin87.dynamodb_example.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

/**
 * Utility class for atomic DynamoDB delete operations using transactions.
 * Provides helper methods for building and executing transactional deletes.
 */
@Slf4j
public class DynamoDbDeleteUtil {

    private final DynamoDbClient dynamoDbClient;

    /**
     * Creates the DynamoDB delete utility.
     *
     * @param dynamoDbClient DynamoDB client for executing transactions
     */
    public DynamoDbDeleteUtil(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    /**
     * Builds a delete operation for a single item.
     *
     * @param tableName DynamoDB table name
     * @param partitionKey partition key attribute value
     * @param sortKey sort key attribute value
     * @return TransactWriteItem delete operation
     */
    public TransactWriteItem buildDeleteOperation(String tableName, String partitionKey, String sortKey) {
        Delete delete = Delete.builder()
            .tableName(tableName)
            .key(Map.of(
                "PKey", AttributeValue.fromS(partitionKey),
                "SKey", AttributeValue.fromS(sortKey)
            ))
            .build();
        return TransactWriteItem.builder().delete(delete).build();
    }

    /**
     * Executes a list of delete operations as a single atomic transaction.
     * Either all operations succeed or none are committed.
     *
     * @param deleteItems list of TransactWriteItem delete operations
     * @param description description of the deletion operation for logging
     * @throws RuntimeException if transaction fails
     */
    public void executeAtomicDeletes(List<TransactWriteItem> deleteItems, String description) {
        if (deleteItems == null || deleteItems.isEmpty()) {
            log.warn("No delete operations to execute for: {}", description);
            return;
        }

        if (deleteItems.size() > 25) {
            throw new IllegalArgumentException(
                "DynamoDB TransactWriteItems supports maximum 25 operations, got " + deleteItems.size()
            );
        }

        try {
            dynamoDbClient.transactWriteItems(TransactWriteItemsRequest.builder()
                .transactItems(deleteItems)
                .build());
            log.info("Successfully executed atomic delete transaction: {} ({} operations)", description, deleteItems.size());
        } catch (Exception e) {
            log.error("Unexpected error during atomic delete for {}: {}", description, e.getMessage(), e);
            throw new RuntimeException("Delete transaction failed: " + description, e);
        }
    }

    /**
     * Helper method to build multiple delete operations at once.
     *
     * @param tableName DynamoDB table name
     * @param items list of items to delete, each item is a pair of (partitionKey, sortKey)
     * @return list of TransactWriteItem delete operations
     */
    public List<TransactWriteItem> buildDeleteOperations(String tableName, List<KeyPair> items) {
        List<TransactWriteItem> deleteItems = new ArrayList<>();
        for (KeyPair keyPair : items) {
            deleteItems.add(buildDeleteOperation(tableName, keyPair.getPartitionKey(), keyPair.getSortKey()));
        }
        return deleteItems;
    }

    /**
     * Simple holder for partition and sort key pairs.
     */
    public static class KeyPair {
        private final String partitionKey;
        private final String sortKey;

        public KeyPair(String partitionKey, String sortKey) {
            this.partitionKey = partitionKey;
            this.sortKey = sortKey;
        }

        public String getPartitionKey() {
            return partitionKey;
        }

        public String getSortKey() {
            return sortKey;
        }
    }
}
