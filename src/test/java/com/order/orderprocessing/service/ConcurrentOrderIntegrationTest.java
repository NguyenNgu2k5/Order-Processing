package com.order.orderprocessing.service;

import com.order.orderprocessing.entity.AppUser;
import com.order.orderprocessing.entity.Role;
import com.order.orderprocessing.repository.UserRepository;
import com.order.orderprocessing.dto.request.CreateOrderRequest;
import com.order.orderprocessing.dto.request.CreateOrderItemRequest;
import com.order.orderprocessing.entity.Product;
import com.order.orderprocessing.repository.OrderRepository;
import com.order.orderprocessing.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrentOrderIntegrationTest {
    @Autowired OrderService orderService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired JdbcTemplate jdbc;

    private Long userId;
    private Long productId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM order_items");
        jdbc.update("DELETE FROM orders");
        jdbc.update("DELETE FROM products");
        jdbc.update("DELETE FROM users");
        userId = userRepository.save(new AppUser("concurrent@example.com", "unused", Role.USER)).getId();
        productId = productRepository.save(new Product("Limited Product", BigDecimal.TEN, 5, true)).getId();
    }

    @Test
    void preventsOversellingDuringTenConcurrentOrders() throws Exception {
        int requestCount = 10;
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(requestCount)) {
            List<Future<Boolean>> results = java.util.stream.IntStream.range(0, requestCount)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        try {
                            orderService.create(userId, new CreateOrderRequest(
                                    "Concurrent Test Address", null,
                                    List.of(new CreateOrderItemRequest(productId, 1))));
                            return true;
                        } catch (RuntimeException exception) {
                            return false;
                        }
                    }))
                    .toList();

            ready.await();
            start.countDown();

            long successes = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) successes++;
            }

            assertThat(successes).isEqualTo(5);
            assertThat(orderRepository.count()).isEqualTo(5);
            assertThat(productRepository.findById(productId).orElseThrow().getStock()).isZero();
            assertThat(productRepository.findById(productId).orElseThrow().getStock()).isNotNegative();
        }
    }
}
