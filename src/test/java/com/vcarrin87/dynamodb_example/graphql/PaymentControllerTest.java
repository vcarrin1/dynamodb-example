package com.vcarrin87.dynamodb_example.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vcarrin87.dynamodb_example.models.PaymentItem;
import com.vcarrin87.dynamodb_example.service.PaymentService;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    void createPayment_returnsServiceResult() {
        PaymentItem input = PaymentItem.builder().orderId(UUID.randomUUID()).build();
        PaymentItem saved = PaymentItem.builder().paymentId(UUID.randomUUID()).build();
        when(paymentService.createPayment(input)).thenReturn(saved);

        PaymentItem result = paymentController.createPayment(input);

        assertEquals(saved, result);
    }

    @Test
    void deletePayment_callsServiceAndReturnsMessage() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        String result = paymentController.deletePayment(paymentId, orderId);

        verify(paymentService).deletePayment(paymentId, orderId);
        assertEquals(String.format("Payment %s deleted successfully", paymentId), result);
    }
}
