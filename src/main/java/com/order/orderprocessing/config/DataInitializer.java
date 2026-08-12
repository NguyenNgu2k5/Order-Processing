package com.order.orderprocessing.config;

import com.order.orderprocessing.entity.AppUser;
import com.order.orderprocessing.entity.Role;
import com.order.orderprocessing.repository.UserRepository;
import com.order.orderprocessing.entity.Product;
import com.order.orderprocessing.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@Profile("!test")
public class DataInitializer {
    @Bean
    CommandLineRunner seedDemoData(UserRepository users, ProductRepository products, PasswordEncoder encoder) {
        return args -> {
            if (users.count() == 0) {
                users.saveAll(List.of(
                        new AppUser("user@example.com", encoder.encode("password"), Role.USER),
                        new AppUser("user2@example.com", encoder.encode("password"), Role.USER),
                        new AppUser("admin@example.com", encoder.encode("password"), Role.ADMIN)
                ));
            }
            if (products.count() == 0) {
                products.saveAll(List.of(
                        new Product("Mechanical Keyboard", new BigDecimal("500000.00"), 20, true),
                        new Product("Wireless Mouse", new BigDecimal("250000.00"), 30, true),
                        new Product("Inactive Product", new BigDecimal("100000.00"), 5, false)
                ));
            }
        };
    }
}
