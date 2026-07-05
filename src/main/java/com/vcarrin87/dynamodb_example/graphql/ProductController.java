package com.vcarrin87.dynamodb_example.graphql;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.vcarrin87.dynamodb_example.models.ProductAggregate;
import com.vcarrin87.dynamodb_example.models.ProductItem;
import com.vcarrin87.dynamodb_example.service.ProductService;

@Controller
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @MutationMapping
    public ProductItem createProduct(@Argument("product") ProductItem product) {
        return productService.createProduct(product);
    }

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_USER')")
    @QueryMapping
    public ProductItem product(@Argument UUID productId) {
        return productService.getProductById(productId);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @MutationMapping
    public ProductItem updateProduct(@Argument("product") ProductItem product) {
        return productService.updateProduct(product);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @MutationMapping
    public Boolean deleteProduct(@Argument UUID productId) {
        return productService.deleteProduct(productId);
    }

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_USER')")
    @QueryMapping
    public List<ProductItem> products() {
        return productService.getAllProducts();
    }

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_USER')")
    @QueryMapping
    public ProductAggregate productWithOrderItems(@Argument UUID productId) {
        return productService.getProductWithOrderItems(productId);
    }

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_USER')")
    @QueryMapping
    public List<ProductItem> productsInStock() {
        return productService.getProductsInStock();
    }
}
