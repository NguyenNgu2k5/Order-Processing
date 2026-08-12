package com.order.orderprocessing.service;

import com.order.orderprocessing.entity.AppUser;
import com.order.orderprocessing.repository.UserRepository;
import com.order.orderprocessing.common.exception.BusinessException;
import com.order.orderprocessing.common.exception.ErrorCode;
import com.order.orderprocessing.dto.request.CreateOrderRequest;
import com.order.orderprocessing.dto.request.CreateOrderItemRequest;
import com.order.orderprocessing.dto.response.OrderResponse;
import com.order.orderprocessing.dto.response.OrderSummaryResponse;
import com.order.orderprocessing.dto.response.PageResponse;
import com.order.orderprocessing.entity.Order;
import com.order.orderprocessing.entity.OrderItem;
import com.order.orderprocessing.entity.OrderStatus;
import com.order.orderprocessing.entity.PaymentStatus;
import com.order.orderprocessing.entity.Product;
import com.order.orderprocessing.repository.OrderRepository;
import com.order.orderprocessing.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrderResponse create(Long userId, CreateOrderRequest request) {
        validateItems(request.items());

        List<Long> productIds = request.items().stream()
                .map(CreateOrderItemRequest::productId)
                .sorted()
                .toList();
        List<Product> lockedProducts = productRepository.findAllByIdForUpdate(productIds);
        Map<Long, Product> products = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (Long productId : productIds) {
            Product product = products.get(productId);
            if (product == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Product " + productId + " does not exist");
            }
            if (!product.isActive()) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_ACTIVE, "Product " + productId + " is inactive");
            }
        }

        for (CreateOrderItemRequest item : request.items()) {
            Product product = products.get(item.productId());
            if (product.getStock() < item.quantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                        "Product " + product.getId() + " has only " + product.getStock() + " items remaining");
            }
        }

        BigDecimal total = request.items().stream()
                .map(item -> products.get(item.productId()).getPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED, "Authenticated user no longer exists"));
        Order order = new Order(generateOrderCode(), user, total, request.shippingAddress(), request.note());

        for (CreateOrderItemRequest item : request.items()) {
            Product product = products.get(item.productId());
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.quantity()));
            product.reduceStock(item.quantity());
            order.addItem(new OrderItem(order, product.getId(), product.getName(), item.quantity(),
                    product.getPrice(), subtotal));
        }
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> listMine(Long userId, int page, int size) {
        Page<Order> orders = orderRepository.findByUserId(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(orders, OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getMine(Long userId, Long id) {
        return orderRepository.findDetailByIdAndUserId(id, userId)
                .map(OrderResponse::from)
                .orElseThrow(this::orderNotFound);
    }

    @Transactional
    public OrderResponse pay(Long userId, Long id) {
        Order order = orderRepository.findOwnedByIdForUpdate(id, userId)
                .orElseThrow(this::orderNotFound);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_COMPLETED, "Order has already been paid");
        }
        if (order.getStatus() != OrderStatus.PENDING || order.getPaymentStatus() != PaymentStatus.UNPAID) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_PAID, "Current order cannot be paid");
        }
        order.pay();
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancelMine(Long userId, Long id) {
        Order order = orderRepository.findOwnedByIdForUpdate(id, userId)
                .orElseThrow(this::orderNotFound);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_CANCELLED,
                    "Only a pending order can be cancelled by its owner");
        }
        restoreStockAndCancel(order);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus next) {
        Order order = orderRepository.findByIdForUpdate(id).orElseThrow(this::orderNotFound);
        if (!order.getStatus().canTransitionTo(next)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Cannot change order status from " + order.getStatus() + " to " + next);
        }
        if (next == OrderStatus.CANCELLED) {
            restoreStockAndCancel(order);
        } else {
            order.changeStatus(next);
        }
        return OrderResponse.from(order);
    }

    private void validateItems(List<CreateOrderItemRequest> items) {
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_ORDER_ITEMS, "Order item list must not be empty");
        }
        Set<Long> uniqueIds = new HashSet<>();
        for (CreateOrderItemRequest item : items) {
            if (!uniqueIds.add(item.productId())) {
                throw new BusinessException(ErrorCode.DUPLICATE_PRODUCT,
                        "Product " + item.productId() + " appears more than once");
            }
        }
    }

    private void restoreStockAndCancel(Order order) {
        List<Long> productIds = order.getItems().stream().map(OrderItem::getProductId).sorted().toList();
        Map<Long, Product> products = productRepository.findAllByIdForUpdate(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (OrderItem item : order.getItems()) {
            Product product = products.get(item.getProductId());
            if (product == null) {
                throw new IllegalStateException("Order references missing product " + item.getProductId());
            }
            product.restoreStock(item.getQuantity());
        }
        order.cancel();
    }

    private String generateOrderCode() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "OFL-" + date + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BusinessException orderNotFound() {
        return new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Order does not exist or is inaccessible");
    }
}
