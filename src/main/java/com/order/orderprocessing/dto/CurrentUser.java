package com.order.orderprocessing.dto;

import com.order.orderprocessing.entity.Role;

public record CurrentUser(Long id, String email, Role role) {}
