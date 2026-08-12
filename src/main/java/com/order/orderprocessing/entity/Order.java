package com.order.orderprocessing.entity;

import com.order.orderprocessing.entity.AppUser;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true, length = 30)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "shipping_address", nullable = false, length = 500)
    private String shippingAddress;

    @Column(length = 500)
    private String note;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {}

    public Order(String orderCode, AppUser user, BigDecimal totalAmount, String shippingAddress, String note) {
        this.orderCode = orderCode;
        this.user = user;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
        this.note = note;
        this.status = OrderStatus.PENDING;
        this.paymentStatus = PaymentStatus.UNPAID;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addItem(OrderItem item) {
        items.add(item);
    }

    public void pay() {
        paymentStatus = PaymentStatus.PAID;
    }

    public void changeStatus(OrderStatus next) {
        status = next;
    }

    public void cancel() {
        status = OrderStatus.CANCELLED;
        cancelledAt = LocalDateTime.now();
        if (paymentStatus == PaymentStatus.PAID) {
            paymentStatus = PaymentStatus.REFUNDED;
            refundedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getOrderCode() { return orderCode; }
    public AppUser getUser() { return user; }
    public OrderStatus getStatus() { return status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getShippingAddress() { return shippingAddress; }
    public String getNote() { return note; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<OrderItem> getItems() { return items; }
}
