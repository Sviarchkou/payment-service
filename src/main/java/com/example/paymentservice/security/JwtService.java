package com.example.paymentservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${security.constants.jwt.secret}")
    private String secret;

    public UUID extractId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return UUID.fromString(claims.get("id", String.class));
    }

    public List<String> extractRoles(String token) {
        Claims claims =Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        Object rolesClaim = claims.get("roles");
        if (rolesClaim instanceof List<?>) {
            return ((List<?>) rolesClaim).stream().map(Object::toString).collect(Collectors.toList());
        }
        return List.of();
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
