package com.order.orderprocessing.dto.response;

import com.order.orderprocessing.entity.Product;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, BigDecimal price, int stock, boolean active) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPrice(),
                product.getStock(), product.isActive());
    }
}
