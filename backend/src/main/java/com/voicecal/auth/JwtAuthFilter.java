package com.voicecal.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Set<String> WHITE_LIST = Set.of(
            "/auth/register", "/auth/login", "/auth/refresh",
            "/api-docs", "/swagger-ui", "/v3/api-docs",
            "/swagger-resources", "/webjars");

    private final JwtProvider jwtProvider;

    public JwtAuthFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (isWhiteListed(path)) {
            chain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);
        if (token == null) {
            writeUnauthorized(response, "缺少认证 Token");
            return;
        }

        try {
            Long userId = jwtProvider.getUserIdFromToken(token);
            String username = jwtProvider.getUsernameFromToken(token);
            UserContext.set(new LoginUser(userId, username));
            chain.doFilter(request, response);
        } catch (JwtAuthException e) {
            writeUnauthorized(response, e.getMessage());
        } finally {
            UserContext.clear();
        }
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"code\":1001,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}",
                message, System.currentTimeMillis()));
    }

}
