package com.vcarrin87.dynamodb_example.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.vcarrin87.dynamodb_example.models.CustomerAggregate;
import com.vcarrin87.dynamodb_example.models.CustomerFilterInput;
import com.vcarrin87.dynamodb_example.models.CustomerItem;
import com.vcarrin87.dynamodb_example.models.CustomerPage;
import com.vcarrin87.dynamodb_example.models.EntityType;
import com.vcarrin87.dynamodb_example.models.OrderItem;
import com.vcarrin87.dynamodb_example.models.PaymentItem;
import com.vcarrin87.dynamodb_example.models.Keys;

import com.vcarrin87.dynamodb_example.repository.CustomerRepository;

@Service
public class CustomerService {

    private final CustomerRepository repository;
    private final OrderService orderService;
    private final PaymentService paymentService;

    /**
     * Creates the customer service.
     *
     * @param repository customer repository
     * @param orderService order service
     * @param paymentService payment service
     */
    public CustomerService(CustomerRepository repository, OrderService orderService, PaymentService paymentService) {
        this.repository = repository;
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    /**
     * Builds a full customer aggregate with related orders and payments.
     *
     * @param customerId customer UUID
     * @return aggregate wrapped in optional when customer exists
     */
    public Optional<CustomerAggregate> getCustomerWithOrdersAndPayments(UUID customerId) {
        String customerIdString = customerId.toString();

        CustomerItem customer = repository.getCustomerProfile(customerIdString);

        if (customer == null) {
            return Optional.empty();
        }

        List<OrderItem> orders = orderService.listOrdersForCustomer(customerId);

        List<UUID> orderIds = orders.stream().map(OrderItem::getOrderId).collect(Collectors.toList());

        List<PaymentItem> payments = paymentService.listPaymentsForOrderIds(orderIds);

        return Optional.of(CustomerAggregate.builder()
                .customer(customer)
                .orders(orders)
                .payments(payments)
                .build());
    }

    /**
     * Creates a new customer profile.
     *
     * @param customerInput customer payload
     * @return persisted customer profile
     */
    public CustomerItem createCustomer(CustomerItem customerInput) {
        UUID customerId = customerInput.getCustomerId() != null
                ? customerInput.getCustomerId()
                : UUID.randomUUID();

        CustomerItem newCustomer = CustomerItem.builder()
                .pKey(Keys.customerPk(customerId.toString()))
                .sKey(Keys.customerProfileSk())
                .entityType(EntityType.CUSTOMER.name())
                .customerId(customerId)
                .name(customerInput.getName())
                .email(customerInput.getEmail())
                .address(customerInput.getAddress())
                .createdAt(Instant.now())
                .build();
        repository.saveCustomer(newCustomer);
        return newCustomer;
    }

    /**
     * Returns paginated customers with optional filters.
     *
     * @param pageSize page size
     * @param nextToken pagination cursor
     * @param filter optional filter input
     * @return filtered customer page
     */
    public CustomerPage getCustomers(int pageSize, String nextToken, CustomerFilterInput filter) {
        String customerId = filter != null && filter.getCustomerId() != null ? filter.getCustomerId().toString() : null;
        String createdAt = filter != null ? filter.getCreatedAt() : null;
        String name = filter != null ? filter.getName() : null;
        return repository.listCustomers(pageSize, nextToken, customerId, createdAt, name);
    }
    
}
