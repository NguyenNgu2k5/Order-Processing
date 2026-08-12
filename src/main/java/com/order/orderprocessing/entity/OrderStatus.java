package com.order.orderprocessing.entity;

import java.util.Set;

public enum OrderStatus {
    PENDING, CONFIRMED, PROCESSING, COMPLETED, CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> Set.of(CONFIRMED, CANCELLED).contains(next);
            case CONFIRMED -> Set.of(PROCESSING, CANCELLED).contains(next);
            case PROCESSING -> next == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
