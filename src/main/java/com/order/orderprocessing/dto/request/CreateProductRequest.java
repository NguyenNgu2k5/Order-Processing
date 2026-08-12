package com.order.orderprocessing.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @Min(0) int stock,
        boolean active
) {}
