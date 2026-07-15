package com.vcarrin87.dynamodb_example.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vcarrin87.dynamodb_example.models.InventoryItem;
import com.vcarrin87.dynamodb_example.models.OrderLineItem;
import com.vcarrin87.dynamodb_example.models.ProductAggregate;
import com.vcarrin87.dynamodb_example.models.ProductItem;
import com.vcarrin87.dynamodb_example.repository.InventoryRepository;
import com.vcarrin87.dynamodb_example.repository.OrderItemRepository;
import com.vcarrin87.dynamodb_example.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_persistsProduct() {
        ProductItem input = ProductItem.builder().name("Laptop").price(BigDecimal.valueOf(1999)).build();

        ProductItem saved = productService.createProduct(input);

        assertEquals("Laptop", saved.getName());
        verify(productRepository).save(saved);
    }

    @Test
    void updateProduct_whenMissing_returnsNull() {
        ProductItem input = ProductItem.builder().productId(UUID.randomUUID()).name("New").build();
        when(productRepository.findById(input.getProductId())).thenReturn(null);

        ProductItem updated = productService.updateProduct(input);

        assertNull(updated);
    }

    @Test
    void getProductWithOrderItems_buildsAggregate() {
        UUID productId = UUID.randomUUID();
        ProductItem product = ProductItem.builder().productId(productId).name("A").build();
        OrderLineItem item = OrderLineItem.builder().productId(productId).build();

        when(productRepository.findById(productId)).thenReturn(product);
        when(orderItemRepository.findByProductId(productId)).thenReturn(List.of(item));

        ProductAggregate aggregate = productService.getProductWithOrderItems(productId);

        assertEquals(productId, aggregate.getProduct().getProductId());
        assertEquals(1, aggregate.getOrderItems().size());
    }

    @Test
    void getProductsInStock_filtersProductsByInventory() {
        UUID idInStock = UUID.randomUUID();
        UUID idOut = UUID.randomUUID();

        when(inventoryRepository.findInStock()).thenReturn(List.of(InventoryItem.builder().productId(idInStock).stockLevel(1).build()));
        when(productRepository.findAll()).thenReturn(List.of(
                ProductItem.builder().productId(idInStock).name("In").build(),
                ProductItem.builder().productId(idOut).name("Out").build()));

        List<ProductItem> inStock = productService.getProductsInStock();

        assertEquals(1, inStock.size());
        assertEquals(idInStock, inStock.get(0).getProductId());
    }

    @Test
    void deleteProduct_deletesRelatedAndProduct() {
        UUID productId = UUID.randomUUID();
        when(productRepository.deleteById(productId)).thenReturn(true);

        boolean deleted = productService.deleteProduct(productId);

        assertTrue(deleted);
        verify(inventoryRepository).deleteInventoryByProductId(productId);
        verify(orderItemRepository).deleteOrderItemByProductId(productId);
    }
}
