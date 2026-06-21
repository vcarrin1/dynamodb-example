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
public class CustomerAggregate {

    private CustomerItem customer;
    private List<OrderItem> orders;
    private List<PaymentItem> payments;
}
