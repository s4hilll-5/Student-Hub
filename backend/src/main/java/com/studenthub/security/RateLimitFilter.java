package com.studenthub.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate-limits the sensitive authentication endpoints per client IP, to slow down
 * password guessing, account creation spam and reset-email flooding.
 *
 * The client IP is request.getRemoteAddr(). Behind a reverse proxy, set
 * server.forward-headers-strategy=framework so that it reflects X-Forwarded-For.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(int maxAttempts, Duration window) {
    }

    private static final Map<String, Rule> RULES = Map.of(
            "/api/auth/login", new Rule(10, Duration.ofMinutes(1)),
            "/api/auth/register", new Rule(5, Duration.ofMinutes(10)),
            "/api/auth/forgot-password", new Rule(5, Duration.ofMinutes(15)),
            "/api/auth/reset-password", new Rule(10, Duration.ofMinutes(15)),
            "/api/auth/refresh", new Rule(60, Duration.ofMinutes(1)));

    private final FixedWindowRateLimiter limiter;
    private final JsonErrorWriter errorWriter;
    private final boolean enabled;

    public RateLimitFilter(FixedWindowRateLimiter limiter, JsonErrorWriter errorWriter, boolean enabled) {
        this.limiter = limiter;
        this.errorWriter = errorWriter;
        this.enabled = enabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !"POST".equalsIgnoreCase(request.getMethod()) || !RULES.containsKey(pathOf(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = pathOf(request);
        Rule rule = RULES.get(path);
        long retryAfter = limiter.tryAcquire(path + ":" + request.getRemoteAddr(), rule.maxAttempts(), rule.window());
        if (retryAfter > 0) {
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
            errorWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS,
                    "Too many attempts. Try again in " + retryAfter + " seconds.");
            return;
        }
        chain.doFilter(request, response);
    }

    private static String pathOf(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
