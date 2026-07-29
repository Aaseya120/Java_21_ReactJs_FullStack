package com.demo.order.contract;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.demo.order.controller.OrderController;
import com.demo.order.dto.OrderResponse;
import com.demo.order.entity.OrderStatus;
import com.demo.order.service.OrderService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

/**
 * Base class for Consumer-Driven Contract verification tests.
 * Ensures the API Gateway and React Frontend consumers never break against Order Service JSON schema.
 */
@SpringBootTest
public abstract class OrderContractTestBase {

    @Autowired
    private OrderController orderController;

    @MockBean
    private OrderService orderService;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.standaloneSetup(orderController);

        OrderResponse sampleOrder = new OrderResponse(
                100L,
                "ORD-1-1",
                1L,
                10L,
                2,
                new BigDecimal("99.99"),
                OrderStatus.PENDING,
                "Test notes",
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                "Order is pending"
        );

        Mockito.when(orderService.getOrderById(100L)).thenReturn(sampleOrder);
    }
}
