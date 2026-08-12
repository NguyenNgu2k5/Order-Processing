package com.order.orderprocessing.dto;

import com.order.orderprocessing.entity.Order;
import com.order.orderprocessing.entity.OrderStatus;
import com.order.orderprocessing.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(Long id, String orderCode, OrderStatus status, PaymentStatus paymentStatus,
                                   BigDecimal totalAmount, LocalDateTime createdAt) {
    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(order.getId(), order.getOrderCode(), order.getStatus(),
                order.getPaymentStatus(), order.getTotalAmount(), order.getCreatedAt());
    }
}
