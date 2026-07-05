package com.vcarrin87.dynamodb_example.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.vcarrin87.dynamodb_example.models.EntityType;
import com.vcarrin87.dynamodb_example.models.Keys;
import com.vcarrin87.dynamodb_example.models.ProductItem;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Repository
public class ProductRepository {

    private static final String PRODUCT_TABLE = "ProductTable";

    private final DynamoDbEnhancedClient enhancedClient;

    public ProductRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    public void save(ProductItem product) {
        DynamoDbTable<ProductItem> table = enhancedClient.table(PRODUCT_TABLE, TableSchema.fromBean(ProductItem.class));
        table.putItem(product);
    }

    public ProductItem findById(UUID productId) {
        DynamoDbTable<ProductItem> table = enhancedClient.table(PRODUCT_TABLE, TableSchema.fromBean(ProductItem.class));
        return table.getItem(r -> r.key(Key.builder()
                .partitionValue(Keys.productPk(productId.toString()))
                .sortValue(Keys.productDetailSk())
                .build()));
    }

    public boolean deleteById(UUID productId) {
        DynamoDbTable<ProductItem> table = enhancedClient.table(PRODUCT_TABLE, TableSchema.fromBean(ProductItem.class));
        ProductItem deleted = table.deleteItem(r -> r.key(Key.builder()
                .partitionValue(Keys.productPk(productId.toString()))
                .sortValue(Keys.productDetailSk())
                .build()));
        return deleted != null;
    }

    public List<ProductItem> findAll() {
        DynamoDbTable<ProductItem> table = enhancedClient.table(PRODUCT_TABLE, TableSchema.fromBean(ProductItem.class));

        Map<String, AttributeValue> values = Map.of(
                ":entityType", AttributeValue.builder().s(EntityType.PRODUCT.name()).build());

        Expression filterExpression = Expression.builder()
                .expression("EntityType = :entityType")
                .expressionValues(values)
                .build();

        ScanEnhancedRequest req = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .build();

        return table.scan(req).items().stream().collect(Collectors.toList());
    }
}
