package com.vcarrin87.dynamodb_example.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vcarrin87.dynamodb_example.models.EntityType;
import com.vcarrin87.dynamodb_example.models.PaymentItem;
import com.vcarrin87.dynamodb_example.repository.PaymentRepository;
import com.vcarrin87.dynamodb_example.util.DynamoDbDeleteUtil;

import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private DynamoDbDeleteUtil deleteUtil;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createPayment_persistsBuiltPayment() {
        UUID orderId = UUID.randomUUID();
        PaymentItem input = PaymentItem.builder()
                .orderId(orderId)
                .amount(BigDecimal.valueOf(18.50))
                .paymentMethod("CARD")
                .build();

        PaymentItem saved = paymentService.createPayment(input);

        assertEquals(EntityType.PAYMENT.name(), saved.getEntityType());
        assertEquals(orderId, saved.getOrderId());
        verify(paymentRepository).savePayment(saved);
    }

    @Test
    void deletePayment_whenMissing_throws() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.listPaymentsForOrderIds(List.of(orderId))).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> paymentService.deletePayment(paymentId, orderId));
    }

    @Test
    void deletePayment_whenPresent_executesDelete() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentItem existing = PaymentItem.builder().paymentId(paymentId).orderId(orderId).build();

        when(paymentRepository.listPaymentsForOrderIds(List.of(orderId))).thenReturn(List.of(existing));
        when(deleteUtil.buildDeleteOperation(any(), any(), any())).thenReturn(TransactWriteItem.builder().build());

        paymentService.deletePayment(paymentId, orderId);

        verify(deleteUtil).executeAtomicDeletes(any(), eq(String.format("Delete payment %s", paymentId)));
    }

    @Test
    void buildDeleteOperationsForOrderIds_returnsOperations() {
        UUID orderId = UUID.randomUUID();
        PaymentItem item = PaymentItem.builder().paymentId(UUID.randomUUID()).orderId(orderId).build();
        when(paymentRepository.listPaymentsForOrderIds(List.of(orderId))).thenReturn(List.of(item));
        when(deleteUtil.buildDeleteOperation(any(), any(), any())).thenReturn(TransactWriteItem.builder().build());

        List<TransactWriteItem> operations = paymentService.buildDeleteOperationsForOrderIds(List.of(orderId));

        assertEquals(1, operations.size());
    }
}
