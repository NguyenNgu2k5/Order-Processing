package com.order.orderprocessing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank @Size(max = 500) String shippingAddress,
        @Size(max = 500) String note,
        @NotNull List<@Valid Item> items
) {
    public record Item(
            @NotNull Long productId,
            @NotNull @Min(1) Integer quantity
    ) {}
}
