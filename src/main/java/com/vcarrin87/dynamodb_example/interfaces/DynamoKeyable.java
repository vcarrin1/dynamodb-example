package com.vcarrin87.dynamodb_example.interfaces;

public interface DynamoKeyable {

    String getPartitionKey();

    String getSortKey();
}
