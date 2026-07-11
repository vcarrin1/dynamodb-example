package com.vcarrin87.dynamodb_example.graphql;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.vcarrin87.dynamodb_example.models.PaymentItem;
import com.vcarrin87.dynamodb_example.service.PaymentService;

@Controller
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Creates the GraphQL payment controller.
     *
     * @param paymentService payment service
     */
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Creates a new payment.
     *
     * @param paymentInput payment payload
     * @return created payment
     */
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @MutationMapping
    public PaymentItem createPayment(@Argument("payment") PaymentItem paymentInput) {
        return paymentService.createPayment(paymentInput);
    }

    /**
     * Deletes a payment.
     *
     * @param paymentId payment UUID to delete
     * @param orderId order UUID associated with payment
     * @return confirmation message
     */
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @MutationMapping
    public String deletePayment(@Argument UUID paymentId, @Argument UUID orderId) {
        paymentService.deletePayment(paymentId, orderId);
        return String.format("Payment %s deleted successfully", paymentId);
    }
}
