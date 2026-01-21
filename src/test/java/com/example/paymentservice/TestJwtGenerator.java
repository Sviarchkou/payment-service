package com.example.paymentservice;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class TestJwtGenerator {

    @Value("${security.constants.jwt.secret}")
    private String secret;

    public String generateAccessToken(String login, UUID userId, List<String> roles){
        Claims claims = Jwts.claims();
        claims.put("roles", roles);
        claims.put("id", userId);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(login)
                .setIssuedAt(new Date(System.currentTimeMillis() - 100000))
                .setExpiration(new Date(System.currentTimeMillis() + 500000))
                .signWith(getSignKey(), SignatureAlgorithm.HS512)
                .compact();

    }

    public String generateAdminAccessToken(){
        return generateAccessToken("login", UUID.randomUUID(), List.of("ROLE_USER", "ROLE_ADMIN"));

    }

    public HttpEntity<Void> generateHttpEntityWithJwt(String login, UUID userId, List<String> roles){
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateAccessToken(login, userId, roles));
        return new HttpEntity<>(headers);
    }

    public HttpEntity<Void> generateHttpEntityWithJwtRoleAdmin(){
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateAdminAccessToken());
        return new HttpEntity<>(headers);
    }

    public HttpEntity<Object> generateHttpEntityWithJwtRoleAdmin(Object o){
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateAdminAccessToken());
        return new HttpEntity<>(o, headers);
    }


    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

