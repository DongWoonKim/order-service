package com.example.spring.orderservice.order.domain;

import com.example.spring.orderservice.book.Book;
import com.example.spring.orderservice.book.BookClient;
import com.example.spring.orderservice.order.event.OrderAcceptedMessage;
import com.example.spring.orderservice.order.event.OrderDispatchedMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final BookClient bookClient;
    private final OrderRepository orderRepository;
    private final StreamBridge streamBridge;


    public Flux<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public Mono<Order> submitOrder(String isbn, int quantity) {
        return bookClient.getBookByIsbn(isbn)
                .map(book -> buildAcceptedOrder(book, quantity))
                .defaultIfEmpty(buildRejectedOrder(isbn, quantity))
                .flatMap(orderRepository::save)
                .doOnNext(this::publishOrderAcceptedEvent);
    }

    public static Order buildAcceptedOrder(Book book, int quantity) {
        return Order.builder()
                .bookIsbn(book.isbn())
                .bookName(book.title() + " - " + book.author())
                .bookPrice(book.price())
                .quantity(quantity)
                .status(OrderStatus.ACCEPTED)
                .build();
    }

    public static Order buildRejectedOrder(String isbn, int quantity) {
        return Order.builder()
                .bookIsbn(isbn)
                .quantity(quantity)
                .status(OrderStatus.REJECTED)
                .build();
    }

    private Order buildDispatchedOrder(Order existingOrder) {
        return Order.builder()
                .id(existingOrder.id())
                .bookIsbn(existingOrder.bookIsbn())
                .bookName(existingOrder.bookName())
                .bookPrice(existingOrder.bookPrice())
                .quantity(existingOrder.quantity())
                .status(OrderStatus.DISPATCHED)
                .createdDate(existingOrder.createdDate())
                .lastModifiedDate(existingOrder.lastModifiedDate())
                .version(existingOrder.version())
                .build();
    }

    public Flux<Order> consumerOrderDispatchedEvent(Flux<OrderDispatchedMessage> flux) {
        return flux
                .flatMap(message ->
                        orderRepository.findById(message.orderId()))
                .map(this::buildDispatchedOrder)
                .flatMap(orderRepository::save);
    }

    private void publishOrderAcceptedEvent(Order order) {
        if (!order.status().equals(OrderStatus.ACCEPTED)) return;

        OrderAcceptedMessage orderAcceptedMessage = new OrderAcceptedMessage(order.id());
        log.info("Sending order accepted event with id : {}", order.id());
        boolean result = streamBridge.send("acceptOrder-out-0", orderAcceptedMessage);
        log.info("Result of sending data for order with id : {} : {}", order.id(), result);
    }

}
