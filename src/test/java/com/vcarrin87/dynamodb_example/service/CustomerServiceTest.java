package com.vcarrin87.dynamodb_example.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vcarrin87.dynamodb_example.models.CustomerAggregate;
import com.vcarrin87.dynamodb_example.models.CustomerFilterInput;
import com.vcarrin87.dynamodb_example.models.CustomerItem;
import com.vcarrin87.dynamodb_example.models.CustomerPage;
import com.vcarrin87.dynamodb_example.models.OrderItem;
import com.vcarrin87.dynamodb_example.models.PaymentItem;
import com.vcarrin87.dynamodb_example.repository.CustomerRepository;
import com.vcarrin87.dynamodb_example.util.DynamoDbDeleteUtil;

import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private DynamoDbDeleteUtil deleteUtil;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void getCustomerWithOrdersAndPayments_whenMissing_returnsEmpty() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.getCustomerProfile(customerId.toString())).thenReturn(null);

        Optional<CustomerAggregate> result = customerService.getCustomerWithOrdersAndPayments(customerId);

        assertFalse(result.isPresent());
    }

    @Test
    void createCustomer_persistsBuiltCustomer() {
        CustomerItem input = CustomerItem.builder().name("Jane").email("jane@example.com").build();

        CustomerItem saved = customerService.createCustomer(input);

        assertNotNull(saved.getCustomerId());
        assertNotNull(saved.getCreatedAt());
        verify(customerRepository).saveCustomer(saved);
    }

    @Test
    void getCustomers_passesFilterValues() {
        UUID customerId = UUID.randomUUID();
        CustomerFilterInput filter = CustomerFilterInput.builder().customerId(customerId).name("Jane").createdAt("x").build();
        CustomerPage page = CustomerPage.builder().items(List.of()).nextToken(null).build();

        when(customerRepository.listCustomers(10, null, customerId.toString(), "x", "Jane")).thenReturn(page);

        CustomerPage actual = customerService.getCustomers(10, null, filter);

        assertEquals(page, actual);
    }

    @Test
    void deleteCustomer_whenMissing_throws() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.getCustomerProfile(customerId.toString())).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> customerService.deleteCustomer(customerId));
    }

    @Test
    void deleteCustomer_executesDeleteTransaction() {
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        CustomerItem customer = CustomerItem.builder().customerId(customerId).build();
        OrderItem order = OrderItem.builder().orderId(orderId).build();
        PaymentItem payment = PaymentItem.builder().paymentId(paymentId).orderId(orderId).build();

        when(customerRepository.getCustomerProfile(customerId.toString())).thenReturn(customer);
        when(orderService.listOrdersForCustomer(customerId)).thenReturn(List.of(order));
        when(paymentService.buildDeleteOperationsForOrderIds(List.of(orderId)))
                .thenReturn(List.of(TransactWriteItem.builder().build()));
        when(orderService.buildDeleteOperationsForCustomer(eq(customerId), any()))
                .thenReturn(List.of(TransactWriteItem.builder().build()));
        when(deleteUtil.buildDeleteOperation(any(), any(), any())).thenReturn(TransactWriteItem.builder().build());

        customerService.deleteCustomer(customerId);

        verify(deleteUtil).executeAtomicDeletes(any(), any());
    }
}
