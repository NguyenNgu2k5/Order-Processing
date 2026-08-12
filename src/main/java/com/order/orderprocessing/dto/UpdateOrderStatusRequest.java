package com.order.orderprocessing.dto;

import com.order.orderprocessing.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {}
