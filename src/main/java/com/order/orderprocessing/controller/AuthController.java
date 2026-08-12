package com.order.orderprocessing.controller;

import com.order.orderprocessing.common.exception.BusinessException;
import com.order.orderprocessing.common.exception.ErrorCode;
import com.order.orderprocessing.dto.request.LoginRequest;
import com.order.orderprocessing.dto.response.LoginResponse;
import com.order.orderprocessing.entity.AppUser;
import com.order.orderprocessing.repository.UserRepository;
import com.order.orderprocessing.service.JwtService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive a JWT access token")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.email())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPassword()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password"));
        return new LoginResponse(jwtService.createToken(user), "Bearer", user.getRole());
    }
}
