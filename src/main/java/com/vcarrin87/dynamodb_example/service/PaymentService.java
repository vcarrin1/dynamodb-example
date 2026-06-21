package com.vcarrin87.dynamodb_example.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.vcarrin87.dynamodb_example.models.EntityType;
import com.vcarrin87.dynamodb_example.models.Keys;
import com.vcarrin87.dynamodb_example.models.PaymentItem;
import com.vcarrin87.dynamodb_example.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * Creates the payment service.
     *
     * @param paymentRepository payment repository
     */
    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
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
}
