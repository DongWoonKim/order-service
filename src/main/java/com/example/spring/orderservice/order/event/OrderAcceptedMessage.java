package com.example.spring.orderservice.order.event;

public record OrderAcceptedMessage(
        Long orderId
) {}
