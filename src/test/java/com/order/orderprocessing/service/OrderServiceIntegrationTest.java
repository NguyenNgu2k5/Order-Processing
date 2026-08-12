package com.order.orderprocessing.service;

import com.order.orderprocessing.entity.AppUser;
import com.order.orderprocessing.entity.Role;
import com.order.orderprocessing.repository.UserRepository;
import com.order.orderprocessing.common.exception.BusinessException;
import com.order.orderprocessing.common.exception.ErrorCode;
import com.order.orderprocessing.dto.CreateOrderRequest;
import com.order.orderprocessing.dto.OrderResponse;
import com.order.orderprocessing.entity.OrderStatus;
import com.order.orderprocessing.entity.PaymentStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceIntegrationTest {
    @Autowired OrderService orderService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired JdbcTemplate jdbc;

    private AppUser user;
    private AppUser otherUser;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM order_items");
        jdbc.update("DELETE FROM orders");
        jdbc.update("DELETE FROM products");
        jdbc.update("DELETE FROM users");
        user = userRepository.save(new AppUser("user@example.com", "unused", Role.USER));
        otherUser = userRepository.save(new AppUser("other@example.com", "unused", Role.USER));
    }

    @Test
    void createsOrderAndReducesStockUsingDatabasePrice() {
        Product product = product("Keyboard", "125.50", 5, true);

        OrderResponse response = create(user, item(product, 2));

        assertThat(response.totalAmount()).isEqualByComparingTo("251.00");
        assertThat(response.items().getFirst().unitPrice()).isEqualByComparingTo("125.50");
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(3);
    }

    @Test
    void rejectsEmptyItems() {
        assertBusiness(ErrorCode.EMPTY_ORDER_ITEMS,
                () -> orderService.create(user.getId(), new CreateOrderRequest("Address", null, List.of())));
    }

    @Test
    void rejectsDuplicateProducts() {
        Product product = product("Keyboard", "10", 5, true);
        assertBusiness(ErrorCode.DUPLICATE_PRODUCT,
                () -> create(user, item(product, 1), item(product, 1)));
    }

    @Test
    void rejectsMissingProduct() {
        assertBusiness(ErrorCode.PRODUCT_NOT_FOUND,
                () -> orderService.create(user.getId(), request(new CreateOrderRequest.Item(99999L, 1))));
    }

    @Test
    void rejectsInactiveProduct() {
        Product product = product("Hidden", "10", 5, false);
        assertBusiness(ErrorCode.PRODUCT_NOT_ACTIVE, () -> create(user, item(product, 1)));
    }

    @Test
    void rejectsInsufficientStock() {
        Product product = product("Keyboard", "10", 1, true);
        assertBusiness(ErrorCode.INSUFFICIENT_STOCK, () -> create(user, item(product, 2)));
    }

    @Test
    void validatesEveryItemBeforeChangingAnyStock() {
        Product enough = product("Enough", "10", 5, true);
        Product insufficient = product("Insufficient", "10", 1, true);

        assertBusiness(ErrorCode.INSUFFICIENT_STOCK,
                () -> create(user, item(enough, 2), item(insufficient, 2)));

        assertThat(productRepository.findById(enough.getId()).orElseThrow().getStock()).isEqualTo(5);
        assertThat(productRepository.findById(insufficient.getId()).orElseThrow().getStock()).isEqualTo(1);
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void hidesAnotherUsersOrder() {
        Product product = product("Keyboard", "10", 5, true);
        OrderResponse order = create(user, item(product, 1));

        assertBusiness(ErrorCode.ORDER_NOT_FOUND, () -> orderService.getMine(otherUser.getId(), order.id()));
    }

    @Test
    void cancelsPendingUnpaidOrderAndRestoresStock() {
        Product product = product("Keyboard", "10", 5, true);
        OrderResponse order = create(user, item(product, 2));

        OrderResponse cancelled = orderService.cancelMine(user.getId(), order.id());

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.paymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test
    void cancelsPaidOrderAndRefunds() {
        Product product = product("Keyboard", "10", 5, true);
        OrderResponse order = create(user, item(product, 2));
        orderService.pay(user.getId(), order.id());

        OrderResponse cancelled = orderService.cancelMine(user.getId(), order.id());

        assertThat(cancelled.paymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(cancelled.refundedAt()).isNotNull();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test
    void repeatedCancellationDoesNotRestoreStockTwice() {
        Product product = product("Keyboard", "10", 5, true);
        OrderResponse order = create(user, item(product, 2));
        orderService.cancelMine(user.getId(), order.id());

        assertBusiness(ErrorCode.ORDER_CANNOT_BE_CANCELLED,
                () -> orderService.cancelMine(user.getId(), order.id()));
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test
    void rejectsRepeatedPayment() {
        Product product = product("Keyboard", "10", 5, true);
        OrderResponse order = create(user, item(product, 1));
        orderService.pay(user.getId(), order.id());

        assertBusiness(ErrorCode.PAYMENT_ALREADY_COMPLETED,
                () -> orderService.pay(user.getId(), order.id()));
    }

    @Test
    void enforcesStatusTransitions() {
        Product product = product("Keyboard", "10", 5, true);
        OrderResponse order = create(user, item(product, 1));
        orderService.updateStatus(order.id(), OrderStatus.CONFIRMED);
        orderService.updateStatus(order.id(), OrderStatus.PROCESSING);

        assertBusiness(ErrorCode.INVALID_STATUS_TRANSITION,
                () -> orderService.updateStatus(order.id(), OrderStatus.PENDING));
    }

    private Product product(String name, String price, int stock, boolean active) {
        return productRepository.save(new Product(name, new BigDecimal(price), stock, active));
    }

    private OrderResponse create(AppUser owner, CreateOrderRequest.Item... items) {
        return orderService.create(owner.getId(), request(items));
    }

    private CreateOrderRequest request(CreateOrderRequest.Item... items) {
        return new CreateOrderRequest("123 Test Street", "note", List.of(items));
    }

    private CreateOrderRequest.Item item(Product product, int quantity) {
        return new CreateOrderRequest.Item(product.getId(), quantity);
    }

    private void assertBusiness(ErrorCode code, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(code));
    }
}
