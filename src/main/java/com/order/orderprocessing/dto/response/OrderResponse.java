package com.order.orderprocessing.dto.response;

import com.order.orderprocessing.entity.Order;
import com.order.orderprocessing.entity.OrderStatus;
import com.order.orderprocessing.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderCode,
        OrderStatus status,
        PaymentStatus paymentStatus,
        BigDecimal totalAmount,
        String shippingAddress,
        String note,
        List<OrderItemResponse> items,
        LocalDateTime cancelledAt,
        LocalDateTime refundedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getOrderCode(), order.getStatus(), order.getPaymentStatus(),
                order.getTotalAmount(), order.getShippingAddress(), order.getNote(),
                order.getItems().stream().map(OrderItemResponse::from).toList(), order.getCancelledAt(),
                order.getRefundedAt(), order.getCreatedAt(), order.getUpdatedAt());
    }
}
