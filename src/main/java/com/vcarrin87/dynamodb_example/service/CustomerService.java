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
import com.vcarrin87.dynamodb_example.util.DynamoDbDeleteUtil;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

@Slf4j

@Service
public class CustomerService {

    private final CustomerRepository repository;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final DynamoDbDeleteUtil deleteUtil;

    /**
     * Creates the customer service.
     *
     * @param repository customer repository
     * @param orderService order service
     * @param paymentService payment service
     * @param deleteUtil DynamoDB delete utility for transactions
     */
    public CustomerService(CustomerRepository repository, OrderService orderService, PaymentService paymentService, DynamoDbDeleteUtil deleteUtil) {
        this.repository = repository;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.deleteUtil = deleteUtil;
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

        // Guard against malformed rows that are missing OrderId to avoid failing the aggregate query.
        List<UUID> orderIds = orders.stream()
            .map(OrderItem::getOrderId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

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

    /**
     * Deletes a customer profile and all related order/payment rows.
     *
     * @param customerId customer UUID to delete
     */
    public void deleteCustomer(UUID customerId) {
        String customerIdString = customerId.toString();
        CustomerItem customer = repository.getCustomerProfile(customerIdString);

        if (customer == null) {
            log.warn("No customer found with ID {}", customerId);
            throw new IllegalArgumentException("Customer not found: " + customerId);
        }

        List<OrderItem> orders = orderService.listOrdersForCustomer(customerId);
        List<UUID> orderIds = orders.stream()
            .map(OrderItem::getOrderId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        List<TransactWriteItem> deleteOperations = new java.util.ArrayList<>();
        deleteOperations.addAll(paymentService.buildDeleteOperationsForOrderIds(orderIds));
        deleteOperations.addAll(orderService.buildDeleteOperationsForCustomer(customerId, orders));
        deleteOperations.add(deleteUtil.buildDeleteOperation(
            "CustomerTable",
            Keys.customerPk(customerIdString),
            Keys.customerProfileSk()
        ));

        if (deleteOperations.isEmpty()) {
            return;
        }

        // DynamoDB transactions allow up to 25 operations per request.
        for (int i = 0; i < deleteOperations.size(); i += 25) {
            int toIndex = Math.min(i + 25, deleteOperations.size());
            List<TransactWriteItem> chunk = deleteOperations.subList(i, toIndex);
            deleteUtil.executeAtomicDeletes(
                chunk,
                String.format("Delete customer %s (items %d-%d)", customerId, i + 1, toIndex)
            );
        }

        log.info(
            "Customer {} deleted with {} order(s) and {} payment(s)",
            customerId,
            orders.size(),
            Math.max(0, deleteOperations.size() - orders.size() - 1)
        );
    }
    
}
