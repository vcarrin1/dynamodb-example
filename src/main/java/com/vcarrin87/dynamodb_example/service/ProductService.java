package com.vcarrin87.dynamodb_example.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.vcarrin87.dynamodb_example.models.EntityType;
import com.vcarrin87.dynamodb_example.models.InventoryItem;
import com.vcarrin87.dynamodb_example.models.Keys;
import com.vcarrin87.dynamodb_example.models.OrderLineItem;
import com.vcarrin87.dynamodb_example.models.ProductAggregate;
import com.vcarrin87.dynamodb_example.models.ProductItem;
import com.vcarrin87.dynamodb_example.repository.InventoryRepository;
import com.vcarrin87.dynamodb_example.repository.OrderItemRepository;
import com.vcarrin87.dynamodb_example.repository.ProductRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderItemRepository orderItemRepository;

    public ProductService(
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderItemRepository = orderItemRepository;
    }

    /**
     * This method creates a new product.
     *
     * @param product The product to create.
     * @return persisted product
     */
    public ProductItem createProduct(ProductItem product) {
        UUID productId = product.getProductId() != null ? product.getProductId() : UUID.randomUUID();
        ProductItem newProduct = ProductItem.builder()
                .pKey(Keys.productPk(productId.toString()))
                .sKey(Keys.productDetailSk())
                .entityType(EntityType.PRODUCT.name())
                .productId(productId)
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .build();
        productRepository.save(newProduct);
        log.info("Product created: {}", newProduct);
        return newProduct;
    }

    /**
     * Retrieves a product by its ID.
     *
     * @param id the ID of the product to retrieve
     * @return the product with the specified ID, or null if not found
     */
    public ProductItem getProductById(UUID id) {
        ProductItem product = productRepository.findById(id);
        if (product != null) {
            log.info("Product found: {}", product);
        } else {
            log.warn("Product with ID {} not found", id);
        }
        return product;
    }

    /**
     * Update an existing product.
     *
     * @param product product payload
     * @return persisted product
     */
    public ProductItem updateProduct(ProductItem product) {
        ProductItem existing = productRepository.findById(product.getProductId());
        if (existing == null) {
            log.warn("No product found with ID {}", product.getProductId());
            return null;
        }

        ProductItem updated = ProductItem.builder()
                .pKey(existing.getPKey())
                .sKey(existing.getSKey())
                .entityType(existing.getEntityType())
                .productId(existing.getProductId())
                .name(product.getName() != null ? product.getName() : existing.getName())
                .description(product.getDescription() != null ? product.getDescription() : existing.getDescription())
                .price(product.getPrice() != null ? product.getPrice() : existing.getPrice())
                .build();

        productRepository.save(updated);
        log.info("Product updated: {}", updated);
        return updated;
    }

    /**
     * Deletes a product by its ID.
     *
     * @param id the ID of the product to delete
     * @return true when deletion happened
     */
    public boolean deleteProduct(UUID id) {
        inventoryRepository.deleteInventoryByProductId(id);
        orderItemRepository.deleteOrderItemByProductId(id);
        boolean deleted = productRepository.deleteById(id);
        if (deleted) {
            log.info("Product with ID {} deleted successfully", id);
        } else {
            log.warn("No product found with ID {}", id);
        }
        return deleted;
    }

    /**
     * Retrieves all products in the database.
     *
     * @return list of products
     */
    public List<ProductItem> getAllProducts() {
        List<ProductItem> allProducts = productRepository.findAll();
        log.info("Retrieved all products: {}", allProducts);
        return allProducts;
    }

    /**
     * Get product with order items.
     *
     * @param productId the ID of the product to retrieve with order items
     * @return product aggregate with order items
     */
    public ProductAggregate getProductWithOrderItems(UUID productId) {
        ProductItem product = productRepository.findById(productId);
        if (product == null) {
            return null;
        }
        List<OrderLineItem> orderItems = orderItemRepository.findByProductId(productId);
        ProductAggregate productWithOrderItems = ProductAggregate.builder()
                .product(product)
                .orderItems(orderItems)
                .build();
        log.info("Retrieved product with order items: {}", productWithOrderItems);
        return productWithOrderItems;
    }

    /**
     * Get products in stock.
     *
     * @return list of products that are in stock
     */
    public List<ProductItem> getProductsInStock() {
        List<InventoryItem> inventoryItems = inventoryRepository.findInStock();
        Set<UUID> inStockIds = inventoryItems.stream().map(InventoryItem::getProductId).collect(Collectors.toSet());
        List<ProductItem> productsInStock = productRepository.findAll().stream()
                .filter(product -> inStockIds.contains(product.getProductId()))
                .collect(Collectors.toList());
        log.info("Retrieved products in stock: {}", productsInStock);
        return productsInStock;
    }
}
