package com.example.spring.orderservice.order.event;

import lombok.Builder;

@Builder
public record OrderDispatchedMessage(
        Long orderId
) {}
