package com.example.spring.orderservice.book;


import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

class BookClientTest {

    private MockWebServer mockWebServer;
    private BookClient bookClient;

    @BeforeEach
    void setUp() throws IOException {
        this.mockWebServer = new MockWebServer();
        this.mockWebServer.start();
        WebClient build = WebClient.builder()
                .baseUrl(this.mockWebServer.url("/").toString())
                .build();

        this.bookClient = new BookClient(build);
    }

    @AfterEach
    void clean() throws IOException {
        this.mockWebServer.shutdown();
    }

    @Test
    void whenBookExistsThenReturnBook() {
        String bookIsbn = "1234567890";

        MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(
                """
                {
                    "isbn": "%s",
                    "title": "Title",
                    "author": "Author",
                    "publisher": "Publisher",
                    "price": 9.9
                }
                """.formatted(bookIsbn)
        ).addHeader("Content-Type", "application/json");

        mockWebServer.enqueue(mockResponse);

        Mono<Book> book = bookClient.getBookByIsbn(bookIsbn);

        StepVerifier.create(book)
                .expectNextMatches(b -> b.isbn().equals(bookIsbn))
                .verifyComplete();
    }

}