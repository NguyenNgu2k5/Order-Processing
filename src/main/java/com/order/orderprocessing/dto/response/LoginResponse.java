package com.order.orderprocessing.dto.response;

import com.order.orderprocessing.entity.Role;

public record LoginResponse(String accessToken, String tokenType, Role role) {}
