package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentDto;
import com.example.paymentservice.reponse.PaymentSumResult;
import com.example.paymentservice.request.DateRangeRequest;
import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isPaymentOwnedByUser(#id)")
    public Mono<ResponseEntity<PaymentDto>> get(@PathVariable String id) {
        return paymentService.getById(id).map(paymentDto ->  ResponseEntity.ok().body(paymentDto));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<List<PaymentDto>>> getAll(
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return paymentService.getAll(pageable)
                .collectList()
                .map(paymentDtoList ->  ResponseEntity.ok().body(paymentDtoList));
    }

    @GetMapping("all/{variable}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<List<PaymentDto>>> getAllBy(
            @PathVariable String variable,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return paymentService.getAllBy(variable, pageable)
                .collectList()
                .map(paymentDtoList ->  ResponseEntity.ok().body(paymentDtoList));
    }

    @GetMapping("users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.equalToCurrentUserId(#userId)")
    public Mono<ResponseEntity<List<PaymentDto>>> getAllByUserId(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return paymentService.getAllByUserId(userId, pageable)
                .collectList()
                .map(paymentDtoList ->  ResponseEntity.ok().body(paymentDtoList));
    }

    @GetMapping("orders/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOrderOwnedByUser(#orderId)")
    public Mono<ResponseEntity<List<PaymentDto>>> getByOrderId(
            @PathVariable UUID orderId,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return paymentService.getAllByOrderId(orderId, pageable)
                .collectList()
                .map(paymentDtoList ->  ResponseEntity.ok().body(paymentDtoList));
    }

    @GetMapping("status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<List<PaymentDto>>> getByStatus(
            @PathVariable String status,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return paymentService.getAllByStatus(status, pageable)
                .collectList()
                .map(paymentDtoList ->  ResponseEntity.ok().body(paymentDtoList));
    }

    @PostMapping
    @PreAuthorize("@userSecurity.equalToCurrentUserId(#paymentDto.userId)")
    public Mono<ResponseEntity<PaymentDto>> makePayment(@RequestBody PaymentDto paymentDto) {
        return paymentService.makePayment(paymentDto)
                .map(p ->  ResponseEntity.status(HttpStatus.CREATED).body(p));
    }

    @PostMapping("users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.equalToCurrentUserId(#userId)")
    public Mono<ResponseEntity<PaymentSumResult>> getSumOfPaymentsForUserWithDateRange(@PathVariable UUID userId, @RequestBody DateRangeRequest range){
        return paymentService.getSumOfPaymentsForUserWithDateRange(userId, range)
                .map(ResponseEntity::ok);
    }

    @PostMapping("users")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<PaymentSumResult>> getSumOfPaymentsForAllUsersWithDateRange(@RequestBody DateRangeRequest range){
        return paymentService.getSumOfPaymentsForAllUsersWithDateRange(range)
                .map(ResponseEntity::ok);
    }
}
