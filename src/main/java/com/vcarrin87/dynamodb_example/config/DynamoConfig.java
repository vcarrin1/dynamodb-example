package com.vcarrin87.dynamodb_example.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

import com.vcarrin87.dynamodb_example.util.DynamoDbDeleteUtil;

@Configuration
public class DynamoConfig {

    /**
     * Creates a DynamoDB client configured for local development.
     *
     * @return configured DynamoDB client
     */
    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:8000"))
                .region(Region.of("us-west-1"))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create("dummy", "dummy")))
                .build();
    }

    /**
     * Creates an enhanced DynamoDB client wrapper.
     *
     * @param dynamoDbClient base DynamoDB client
     * @return enhanced DynamoDB client
     */
    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    /**
     * Creates a DynamoDB delete utility for atomic transaction operations.
     *
     * @param dynamoDbClient base DynamoDB client
     * @return DynamoDB delete utility
     */
    @Bean
    public DynamoDbDeleteUtil dynamoDbDeleteUtil(DynamoDbClient dynamoDbClient) {
        return new DynamoDbDeleteUtil(dynamoDbClient);
    }
}
