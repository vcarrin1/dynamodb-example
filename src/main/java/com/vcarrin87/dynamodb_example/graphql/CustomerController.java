package com.vcarrin87.dynamodb_example.graphql;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.vcarrin87.dynamodb_example.models.CustomerAggregate;
import com.vcarrin87.dynamodb_example.models.CustomerFilterInput;
import com.vcarrin87.dynamodb_example.models.CustomerItem;
import com.vcarrin87.dynamodb_example.models.CustomerPage;
import com.vcarrin87.dynamodb_example.models.OrderItem;
import com.vcarrin87.dynamodb_example.models.PaymentItem;

import com.vcarrin87.dynamodb_example.service.CustomerService;
import com.vcarrin87.dynamodb_example.service.OrderService;
import com.vcarrin87.dynamodb_example.service.PaymentService;

@Controller
public class CustomerController {

    private final CustomerService customerService;
    private final OrderService orderService;
    private final PaymentService paymentService;

    /**
     * Creates the GraphQL customer controller.
     *
     * @param customerService customer service
     * @param orderService order service
     * @param paymentService payment service
     */
    public CustomerController(CustomerService customerService, OrderService orderService, PaymentService paymentService) {
        this.customerService = customerService;
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    /**
     * Returns a customer aggregate by customer ID.
     *
     * @param customerId customer UUID
     * @return customer aggregate or null when not found
     */
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_USER')")
    @QueryMapping
    public CustomerAggregate customer(@Argument UUID customerId) {
        return customerService.getCustomerWithOrdersAndPayments(customerId).orElse(null);
    }

    /**
     * Returns all orders for a customer.
     *
     * @param customerId customer UUID
     * @return matching order list
     */
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_USER')")
    @QueryMapping
    public List<OrderItem> ordersByCustomer(@Argument UUID customerId) {
        return orderService.listOrdersForCustomer(customerId);
    }

    /**
     * Returns all payments associated with a customer's orders.
     *
     * @param customerId customer UUID
     * @return matching payment list
     */
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_USER')")
    @QueryMapping
    public List<PaymentItem> paymentsByCustomer(@Argument UUID customerId) {
        List<OrderItem> orders = orderService.listOrdersForCustomer(customerId);
        return paymentService.listPaymentsForOrderIds(orders.stream().map(OrderItem::getOrderId).collect(Collectors.toList()));
    }

    /**
     * Returns paginated customers with optional filters.
     *
     * @param pageSize requested page size
     * @param nextToken pagination cursor
     * @param filter optional customer filters
     * @return customer page result
     */
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_USER')")
    @QueryMapping
    public CustomerPage customers(@Argument Integer pageSize, @Argument String nextToken, @Argument CustomerFilterInput filter) {
        int effectivePageSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;
        return customerService.getCustomers(effectivePageSize, nextToken, filter);
    }

    /**
     * Creates or updates a customer profile.
     *
     * @param customerInput customer payload
     * @return persisted customer
     */
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @MutationMapping("customerUpsert")
    public CustomerItem createCustomer(@Argument("customer") CustomerItem customerInput) {
        return customerService.createCustomer(customerInput);
    }

    /**
     * Deletes a customer and all related orders and payments atomically.
     *
     * @param customerId customer UUID to delete
     * @return confirmation message
     */
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @MutationMapping
    public String deleteCustomer(@Argument UUID customerId) {
        customerService.deleteCustomer(customerId);
        return String.format("Customer %s deleted successfully", customerId);
    }
}
