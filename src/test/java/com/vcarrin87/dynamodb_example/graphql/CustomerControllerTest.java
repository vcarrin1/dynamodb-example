package com.vcarrin87.dynamodb_example.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import com.vcarrin87.dynamodb_example.models.CustomerItem;
import com.vcarrin87.dynamodb_example.models.OrderItem;
import com.vcarrin87.dynamodb_example.models.PaymentItem;
import com.vcarrin87.dynamodb_example.service.CustomerService;
import com.vcarrin87.dynamodb_example.service.OrderService;
import com.vcarrin87.dynamodb_example.service.PaymentService;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private CustomerController customerController;

    @Test
    void customer_whenNotFound_returnsNull() {
        UUID customerId = UUID.randomUUID();
        when(customerService.getCustomerWithOrdersAndPayments(customerId)).thenReturn(Optional.empty());

        assertNull(customerController.customer(customerId));
    }

    @Test
    void ordersByCustomer_returnsOrders() {
        UUID customerId = UUID.randomUUID();
        List<OrderItem> expected = List.of(OrderItem.builder().orderId(UUID.randomUUID()).build());
        when(orderService.listOrdersForCustomer(customerId)).thenReturn(expected);

        List<OrderItem> actual = customerController.ordersByCustomer(customerId);

        assertEquals(expected, actual);
    }

    @Test
    void paymentsByCustomer_usesOrderIds() {
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        List<OrderItem> orders = List.of(OrderItem.builder().orderId(orderId).build());
        List<PaymentItem> payments = List.of(PaymentItem.builder().paymentId(UUID.randomUUID()).orderId(orderId).build());

        when(orderService.listOrdersForCustomer(customerId)).thenReturn(orders);
        when(paymentService.listPaymentsForOrderIds(List.of(orderId))).thenReturn(payments);

        List<PaymentItem> actual = customerController.paymentsByCustomer(customerId);

        assertEquals(payments, actual);
    }

    @Test
    void customerUpsert_returnsServiceResult() {
        CustomerItem input = CustomerItem.builder().name("Jane").build();
        when(customerService.createCustomer(input)).thenReturn(input);

        CustomerItem actual = customerController.createCustomer(input);

        assertEquals(input, actual);
    }

    @Test
    void deleteCustomer_callsServiceAndReturnsMessage() {
        UUID customerId = UUID.randomUUID();

        String actual = customerController.deleteCustomer(customerId);

        verify(customerService).deleteCustomer(customerId);
        assertEquals(String.format("Customer %s deleted successfully", customerId), actual);
    }
}
