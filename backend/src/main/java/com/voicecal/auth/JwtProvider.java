package com.voicecal.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final long expiration;
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key)
                .compact();
    }

    public String getTokenId(String token) {
        return validateToken(token).getId();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new JwtAuthException("Token 已过期", e);
        } catch (JwtException e) {
            throw new JwtAuthException("Token 无效", e);
        }
    }

    public Long getUserIdFromToken(String token) {
        return Long.parseLong(validateToken(token).getSubject());
    }

    public String getUsernameFromToken(String token) {
        return validateToken(token).get("username", String.class);
    }

    public long getExpiration() {
        return expiration;
    }

    public void invalidateToken(String token) {
        blacklist.add(token);
    }

    public boolean isTokenInvalidated(String token) {
        return blacklist.contains(token);
    }

}
