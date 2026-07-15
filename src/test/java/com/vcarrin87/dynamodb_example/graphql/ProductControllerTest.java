package com.vcarrin87.dynamodb_example.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vcarrin87.dynamodb_example.models.ProductItem;
import com.vcarrin87.dynamodb_example.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @Test
    void createProduct_returnsServiceResult() {
        ProductItem input = ProductItem.builder().name("Widget").build();
        when(productService.createProduct(input)).thenReturn(input);

        ProductItem result = productController.createProduct(input);

        assertEquals(input, result);
    }

    @Test
    void deleteProduct_returnsServiceFlag() {
        UUID productId = UUID.randomUUID();
        when(productService.deleteProduct(productId)).thenReturn(true);

        Boolean deleted = productController.deleteProduct(productId);

        assertTrue(deleted);
        verify(productService).deleteProduct(productId);
    }

    @Test
    void products_returnsList() {
        List<ProductItem> expected = List.of(ProductItem.builder().name("A").build());
        when(productService.getAllProducts()).thenReturn(expected);

        List<ProductItem> actual = productController.products();

        assertEquals(expected, actual);
    }
}
