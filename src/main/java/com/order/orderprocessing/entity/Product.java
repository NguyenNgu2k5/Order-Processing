package com.order.orderprocessing.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private boolean active;

    protected Product() {}

    public Product(String name, BigDecimal price, int stock, boolean active) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public int getStock() { return stock; }
    public boolean isActive() { return active; }

    public void reduceStock(int quantity) { stock -= quantity; }
    public void restoreStock(int quantity) { stock += quantity; }
}
