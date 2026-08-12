package com.order.orderprocessing.dto.response;

import com.order.orderprocessing.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.getProductId(), item.getProductName(), item.getQuantity(),
                item.getUnitPrice(), item.getSubtotal());
    }
}
