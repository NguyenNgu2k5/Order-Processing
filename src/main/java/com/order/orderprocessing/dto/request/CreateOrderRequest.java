package com.order.orderprocessing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank @Size(max = 500) String shippingAddress,
        @Size(max = 500) String note,
        @NotNull List<@Valid CreateOrderItemRequest> items
) {}
