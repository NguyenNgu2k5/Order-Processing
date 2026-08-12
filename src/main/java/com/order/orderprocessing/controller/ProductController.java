package com.order.orderprocessing.controller;

import com.order.orderprocessing.dto.request.CreateProductRequest;
import com.order.orderprocessing.dto.response.ProductResponse;
import com.order.orderprocessing.entity.Product;
import com.order.orderprocessing.repository.ProductRepository;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostMapping
    @Operation(summary = "Create a product (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        Product product = productRepository.save(new Product(
                request.name(), request.price(), request.stock(), request.active()));
        return ProductResponse.from(product);
    }
}
