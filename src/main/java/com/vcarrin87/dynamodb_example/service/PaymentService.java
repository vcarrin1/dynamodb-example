package com.vcarrin87.dynamodb_example.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.vcarrin87.dynamodb_example.models.EntityType;
import com.vcarrin87.dynamodb_example.models.Keys;
import com.vcarrin87.dynamodb_example.models.PaymentItem;
import com.vcarrin87.dynamodb_example.repository.PaymentRepository;
import com.vcarrin87.dynamodb_example.util.DynamoDbDeleteUtil;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final DynamoDbDeleteUtil deleteUtil;

    /**
     * Creates the payment service.
     *
     * @param paymentRepository payment repository
     * @param deleteUtil DynamoDB delete utility for transactions
     */
    public PaymentService(PaymentRepository paymentRepository, DynamoDbDeleteUtil deleteUtil) {
        this.paymentRepository = paymentRepository;
        this.deleteUtil = deleteUtil;
    }

    /**
     * Returns all payments for the provided order IDs.
     *
     * @param orderIds order IDs
     * @return matching payment list
     */
    public List<PaymentItem> listPaymentsForOrderIds(List<UUID> orderIds) {
        return paymentRepository.listPaymentsForOrderIds(orderIds);
    }

    /**
     * Creates a new payment record.
     *
     * @param paymentInput payment payload
     * @return persisted payment
     */
    public PaymentItem createPayment(PaymentItem paymentInput) {
        UUID paymentId = paymentInput.getPaymentId() != null ? paymentInput.getPaymentId() : UUID.randomUUID();
        PaymentItem newPayment = PaymentItem.builder()
            .pKey(Keys.orderPk(paymentInput.getOrderId().toString()))
            .sKey(Keys.paymentSk(paymentId.toString()))
                .entityType(EntityType.PAYMENT.name())
                .paymentId(paymentId)
                .orderId(paymentInput.getOrderId())
                .paymentDate(paymentInput.getPaymentDate())
                .amount(paymentInput.getAmount())
                .paymentMethod(paymentInput.getPaymentMethod())
                .build();

        paymentRepository.savePayment(newPayment);
        return newPayment;
    }

    /**
     * Deletes a payment by ID.
     *
     * @param paymentId payment UUID to delete
     * @param orderId order UUID associated with payment
     * @throws RuntimeException if deletion fails
     */
    public void deletePayment(UUID paymentId, UUID orderId) {
        // Verify payment exists
        List<PaymentItem> payments = listPaymentsForOrderIds(java.util.List.of(orderId));
        boolean paymentExists = payments.stream()
            .anyMatch(p -> p.getPaymentId().equals(paymentId));
        
        if (!paymentExists) {
            log.warn("No payment found with ID {}", paymentId);
            throw new IllegalArgumentException("Payment not found: " + paymentId);
        }

        // Build and execute atomic delete
        var deleteOp = deleteUtil.buildDeleteOperation(
            "PaymentTable",
            Keys.orderPk(orderId.toString()),
            Keys.paymentSk(paymentId.toString())
        );
        
        deleteUtil.executeAtomicDeletes(
            java.util.List.of(deleteOp),
            String.format("Delete payment %s", paymentId)
        );
        
        log.info("Payment with ID {} deleted", paymentId);
    }

    /**
     * Builds delete operations for payments tied to the provided orders.
     *
     * @param orderIds order UUIDs
     * @return transactional delete operations
     */
    public List<TransactWriteItem> buildDeleteOperationsForOrderIds(List<UUID> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }

        return listPaymentsForOrderIds(orderIds).stream()
            .filter(payment -> payment.getPaymentId() != null && payment.getOrderId() != null)
            .map(payment -> deleteUtil.buildDeleteOperation(
                "PaymentTable",
                Keys.orderPk(payment.getOrderId().toString()),
                Keys.paymentSk(payment.getPaymentId().toString())
            ))
            .toList();
    }
}
