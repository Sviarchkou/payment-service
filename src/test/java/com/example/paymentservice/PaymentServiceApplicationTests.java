package com.example.paymentservice;

import com.example.paymentservice.dto.PaymentDto;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.kafka.OrderKafkaMessage;
import com.example.paymentservice.mapper.PaymentMapper;
import com.example.paymentservice.reponse.PaymentSumResult;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.request.DateRangeRequest;
import com.example.paymentservice.service.OrderToPayService;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WireMockTest
@Testcontainers
class PaymentServiceApplicationTests {

    @RegisterExtension
    static WireMockExtension wireMockExtension = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().dynamicPort())
            .build();

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestJwtGenerator testJwtGenerator;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private OrderToPayService orderToPayService;

    @DynamicPropertySource
    public static void setUpMockBaseUrl(DynamicPropertyRegistry registry) {
        // registry.add("app.external-api.integer-generator-url", wireMockExtension::baseUrl);
        registry.add("app.external-api.integer-generator-url", () -> wireMockExtension.baseUrl() + "/integer-generator");
    }

    @Value("${app.external-api.integer-generator-url}")
    private String apiURI;

    private UUID userId;
    private UUID orderId;

    @BeforeEach
    void cleanDb() {
        paymentRepository.deleteAll().block();
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    private Payment createPayment(UUID userId, UUID orderId, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setOrderId(orderId);
        payment.setStatus(status);
        payment.setPaymentAmount(new BigDecimal("1234.56"));
        payment.setTimestamp(Instant.now());
        return paymentRepository.save(payment).block();
    }

    private PaymentDto createPaymentDto(UUID userId, UUID orderId) {
        PaymentDto dto = new PaymentDto();
        dto.setUserId(userId);
        dto.setOrderId(orderId);
        dto.setPaymentAmount(new BigDecimal("1234.56"));
        return dto;
    }

    protected DateRangeRequest defaultRange() {
        return new DateRangeRequest(
                Instant.now().minus(7, ChronoUnit.DAYS),
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
    }

    @Test
    void shouldReturnPaymentById() {
        Payment p = createPayment(userId, orderId, PaymentStatus.SUCCESS);
        PaymentDto dto = paymentMapper.toDto(p);

        System.out.println(p.getId());

        var response = webTestClient.get()
                .uri("api/v1/payments/" + p.getId())
                .header("Authorization", "Bearer " + testJwtGenerator.generateAdminAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertTrue(isPaymentsEquals(response, dto));
    }

    @Test
    void shouldReturnListOfPaymentDto() {
        Payment p1 = createPayment(userId, orderId, PaymentStatus.FAILED);
        Payment p2 = createPayment(userId, orderId, PaymentStatus.FAILED);
        Payment p3 = createPayment(userId, orderId, PaymentStatus.SUCCESS);

        PaymentDto dto1 = paymentMapper.toDto(p1);
        PaymentDto dto2 = paymentMapper.toDto(p2);
        PaymentDto dto3 = paymentMapper.toDto(p3);

        List<PaymentDto> dtos = List.of(dto1, dto2, dto3).reversed();

        var response = webTestClient.get()
                .uri("api/v1/payments")
                .header("Authorization", "Bearer " + testJwtGenerator.generateAdminAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals(response.size(), dtos.size());
        for (int i = 0; i < response.size(); i++) {
            assertTrue(isPaymentsEquals(response.get(i), dtos.get(i)));
        }
    }

    @Test
    void shouldReturnListOfFailedPaymentDto() {
        Payment p1 = createPayment(userId, orderId, PaymentStatus.FAILED);
        Payment p2 = createPayment(userId, orderId, PaymentStatus.FAILED);
        Payment p3 = createPayment(userId, orderId, PaymentStatus.SUCCESS);

        PaymentDto dto1 = paymentMapper.toDto(p1);
        PaymentDto dto2 = paymentMapper.toDto(p2);

        List<PaymentDto> dtos = List.of(dto1, dto2).reversed();
        ;

        var response = webTestClient.get()
                .uri("api/v1/payments/all/failed")
                .header("Authorization", "Bearer " + testJwtGenerator.generateAdminAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals(response.size(), dtos.size());
        for (int i = 0; i < response.size(); i++) {
            assertTrue(isPaymentsEquals(response.get(i), dtos.get(i)));
        }
    }

    @Test
    void shouldReturnListOfPaymentDtoWithCorrectUserId() {
        Payment p1 = createPayment(userId, orderId, PaymentStatus.FAILED);
        Payment p2 = createPayment(UUID.randomUUID(), UUID.randomUUID(), PaymentStatus.FAILED);
        Payment p3 = createPayment(userId, orderId, PaymentStatus.SUCCESS);

        PaymentDto dto1 = paymentMapper.toDto(p1);
        PaymentDto dto3 = paymentMapper.toDto(p3);

        List<PaymentDto> dtos = List.of(dto1, dto3).reversed();

        var response = webTestClient.get()
                .uri("api/v1/payments/users/" + userId)
                .header("Authorization", "Bearer " + testJwtGenerator
                        .generateAccessToken("login", userId, List.of("ROLE_USER")))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals(response.size(), dtos.size());
        for (int i = 0; i < response.size(); i++) {
            assertTrue(isPaymentsEquals(response.get(i), dtos.get(i)));
        }
    }

    @Test
    void shouldReturnListOfPaymentDtoWithCorrectOrderId() {
        Payment p1 = createPayment(userId, orderId, PaymentStatus.FAILED);
        Payment p2 = createPayment(UUID.randomUUID(), UUID.randomUUID(), PaymentStatus.FAILED);
        Payment p3 = createPayment(userId, UUID.randomUUID(), PaymentStatus.SUCCESS);

        PaymentDto dto1 = paymentMapper.toDto(p1);

        List<PaymentDto> dtos = List.of(dto1).reversed();
        ;

        var response = webTestClient.get()
                .uri("api/v1/payments/orders/" + orderId)
                .header("Authorization", "Bearer " + testJwtGenerator
                        .generateAccessToken("login", userId, List.of("ROLE_USER")))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals(response.size(), dtos.size());
        for (int i = 0; i < response.size(); i++) {
            assertTrue(isPaymentsEquals(response.get(i), dtos.get(i)));
        }
    }

    @Test
    void shouldReturnListOfSuccessPaymentDto() {
        Payment p1 = createPayment(userId, orderId, PaymentStatus.SUCCESS);
        Payment p2 = createPayment(userId, orderId, PaymentStatus.FAILED);
        Payment p3 = createPayment(userId, orderId, PaymentStatus.SUCCESS);

        PaymentDto dto1 = paymentMapper.toDto(p1);
        PaymentDto dto3 = paymentMapper.toDto(p3);

        List<PaymentDto> dtos = List.of(dto1, dto3).reversed();
        ;

        var response = webTestClient.get()
                .uri("api/v1/payments/status/success")
                .header("Authorization", "Bearer " + testJwtGenerator.generateAdminAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals(response.size(), dtos.size());
        for (int i = 0; i < response.size(); i++) {
            assertTrue(isPaymentsEquals(response.get(i), dtos.get(i)));
        }
    }

    @Test
    void shouldReturnListOfAllPaymentDtoByUserId() {
        Payment p1 = createPayment(userId, orderId, PaymentStatus.FAILED);
        Payment p2 = createPayment(UUID.randomUUID(), UUID.randomUUID(), PaymentStatus.FAILED);
        Payment p3 = createPayment(userId, orderId, PaymentStatus.SUCCESS);

        PaymentDto dto1 = paymentMapper.toDto(p1);
        PaymentDto dto3 = paymentMapper.toDto(p3);

        List<PaymentDto> dtos = List.of(dto1, dto3).reversed();

        var response = webTestClient.get()
                .uri("api/v1/payments/all/" + userId)
                .header("Authorization", "Bearer " + testJwtGenerator
                        .generateAccessToken("login", userId, List.of("ROLE_USER")))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals(response.size(), dtos.size());
        for (int i = 0; i < response.size(); i++) {
            assertTrue(isPaymentsEquals(response.get(i), dtos.get(i)));
        }
    }

    @Test
    void shouldThrowBadRequestOnGetPaymentBy() {
        webTestClient.get()
                .uri("api/v1/payments/all/wesdrftyghui")
                .header("Authorization", "Bearer " + testJwtGenerator.generateAdminAccessToken())
                .exchange()
                .expectStatus().isBadRequest();

    }

    @Test
    void shouldThrowNotFoundOnGetPaymentById() {
        webTestClient.get()
                .uri("api/v1/payments/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + testJwtGenerator.generateAdminAccessToken())
                .exchange()
                .expectStatus().isNotFound();

    }

    @Test
    void shouldCreateAndReturnPayment() {
        wireMockExtension.stubFor(WireMock.get("/integer-generator")
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("28")));

        var createDto = createPaymentDto(userId, orderId);

        webTestClient.mutate().responseTimeout(Duration.ofSeconds(30)).build();

        var mes = new OrderKafkaMessage(orderId, userId, createDto.getPaymentAmount());
        orderToPayService.save(mes).block();

        var response = webTestClient.post()
                .uri("api/v1/payments")
                .header("Authorization", "Bearer " + testJwtGenerator.generateAdminAccessToken())
                .bodyValue(createDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(response.getUserId(), userId);
        assertEquals(response.getOrderId(), orderId);
        assertEquals(response.getPaymentAmount(), createDto.getPaymentAmount());
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
    }

    @Test
    void shouldReturnSumOfPaymentsForUser() {
        Payment p1 = createPayment(userId, orderId, PaymentStatus.FAILED);
        Payment p2 = createPayment(userId, orderId, PaymentStatus.SUCCESS);
        Payment p3 = createPayment(userId, UUID.randomUUID(), PaymentStatus.FAILED);
        Payment p4 = createPayment(userId, UUID.randomUUID(), PaymentStatus.SUCCESS);
        Payment p5 = createPayment(userId, UUID.randomUUID(), PaymentStatus.SUCCESS);

        PaymentSumResult sum = new PaymentSumResult(p2.getPaymentAmount().add(p4.getPaymentAmount().add(p5.getPaymentAmount())));

        var response = webTestClient.post()
                .uri("api/v1/payments/users/" + userId)
                .header("Authorization", "Bearer " + testJwtGenerator
                        .generateAccessToken("login", userId, List.of("ROLE_USER")))
                .bodyValue(defaultRange())
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentSumResult.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals(response, sum);
    }

    @Test
    void shouldReturnSumOfPaymentsForAllUsers() {
        Payment p1 = createPayment(userId, orderId, PaymentStatus.FAILED);
        Payment p2 = createPayment(userId, orderId, PaymentStatus.SUCCESS);
        Payment p3 = createPayment(UUID.randomUUID(), UUID.randomUUID(), PaymentStatus.SUCCESS);
        Payment p4 = createPayment(UUID.randomUUID(), UUID.randomUUID(), PaymentStatus.SUCCESS);
        Payment p5 = createPayment(UUID.randomUUID(), UUID.randomUUID(), PaymentStatus.SUCCESS);

        PaymentSumResult sum = new PaymentSumResult(p2.getPaymentAmount()
                .add(p3.getPaymentAmount()
                        .add(p4.getPaymentAmount()
                                .add(p5.getPaymentAmount()))));

        var response = webTestClient.post()
                .uri("api/v1/payments/users")
                .header("Authorization", "Bearer " + testJwtGenerator.generateAdminAccessToken())
                .bodyValue(defaultRange())
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentSumResult.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals(response, sum);
    }

    private boolean isPaymentsEquals(PaymentDto p1, PaymentDto p2) {
        return p1.getPaymentAmount().equals(p2.getPaymentAmount())
                && p1.getStatus().equals(p2.getStatus())
                && p1.getUserId().equals(p2.getUserId())
                && p1.getOrderId().equals(p2.getOrderId())
                && p1.getId().equals(p2.getId())
//                && p1.getTimestamp().truncatedTo(ChronoUnit.MILLIS)
//                .equals(p2.getTimestamp().truncatedTo(ChronoUnit.MILLIS))
                ;
    }
}
