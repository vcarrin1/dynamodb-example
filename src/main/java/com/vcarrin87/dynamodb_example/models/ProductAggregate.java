package com.vcarrin87.dynamodb_example.models;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAggregate {
    private ProductItem product;
    private List<OrderLineItem> orderItems;
}
