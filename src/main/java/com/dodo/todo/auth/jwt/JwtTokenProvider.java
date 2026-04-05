package com.dodo.todo.auth.jwt;

import com.dodo.todo.auth.config.JwtProperties;
import com.dodo.todo.auth.principal.MemberPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(MemberPrincipal memberPrincipal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getAccessTokenExpirationSeconds());

        return Jwts.builder()
                .subject(memberPrincipal.getId().toString())
                .claim("email", memberPrincipal.getEmail())
                .claim("nickname", memberPrincipal.getNickname())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public Long getMemberId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.valueOf(claims.getSubject());
    }

    public boolean isValidToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.getAccessTokenExpirationSeconds();
    }
}
