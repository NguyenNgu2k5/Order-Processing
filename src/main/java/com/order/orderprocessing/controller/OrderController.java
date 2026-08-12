package com.order.orderprocessing.controller;

import com.order.orderprocessing.dto.CurrentUser;
import com.order.orderprocessing.dto.request.CreateOrderRequest;
import com.order.orderprocessing.dto.response.OrderResponse;
import com.order.orderprocessing.dto.response.OrderSummaryResponse;
import com.order.orderprocessing.dto.response.PageResponse;
import com.order.orderprocessing.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@Validated
@Tag(name = "Orders", description = "Authenticated user's order lifecycle")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Create an order")
    public OrderResponse create(@AuthenticationPrincipal CurrentUser user,
                                @Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(user.id(), request);
    }

    @GetMapping
    @Operation(summary = "List my orders")
    public PageResponse<OrderSummaryResponse> listMine(@AuthenticationPrincipal CurrentUser user,
                                                       @RequestParam(defaultValue = "0") @Min(0) int page,
                                                       @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return orderService.listMine(user.id(), page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get my order detail")
    public OrderResponse getMine(@AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
        return orderService.getMine(user.id(), id);
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel my pending order and restore stock")
    public OrderResponse cancel(@AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
        return orderService.cancelMine(user.id(), id);
    }

    @PutMapping("/{id}/pay")
    @Operation(summary = "Complete mock payment")
    public OrderResponse pay(@AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
        return orderService.pay(user.id(), id);
    }
}
