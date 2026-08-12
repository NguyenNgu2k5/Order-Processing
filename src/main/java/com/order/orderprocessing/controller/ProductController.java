package com.order.orderprocessing.controller;

import com.order.orderprocessing.entity.Product;
import com.order.orderprocessing.repository.ProductRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

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

    public record CreateProductRequest(
            @NotBlank @Size(max = 255) String name,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @Min(0) int stock,
            boolean active
    ) {}

    public record ProductResponse(Long id, String name, BigDecimal price, int stock, boolean active) {
        static ProductResponse from(Product product) {
            return new ProductResponse(product.getId(), product.getName(), product.getPrice(),
                    product.getStock(), product.isActive());
        }
    }
}
