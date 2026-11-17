package com.portfolio.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window, per-IP rate limiting (in memory) for brute force and spam protection, plus a body
 * size cap. Sensitive endpoints get tight limits; everything else a generous general limit.
 * For several instances behind a load balancer, move the counters to Redis.
 *
 * <p>The client IP is {@code request.getRemoteAddr()}. Behind a reverse proxy enable
 * {@code server.forward-headers-strategy=native} so it reflects the real client, and never trust a raw
 * X-Forwarded-For header coming straight from the internet.
 */
class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_TRACKED_KEYS = 50_000;

    private record Window(long start, AtomicInteger count) {}

    private final RateLimitProperties props;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    RateLimitFilter(RateLimitProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > props.getMaxBodyBytes()) {
            JsonSecurityErrorHandlers.write(response, HttpStatus.CONTENT_TOO_LARGE, "PAYLOAD_TOO_LARGE",
                    "Dữ liệu gửi lên quá lớn");
            return;
        }
        if (props.isEnabled()) {
            String bucket = bucket(request);
            int limit = limit(bucket);
            long now = System.currentTimeMillis();
            String key = bucket + "|" + request.getRemoteAddr();
            Window w = windows.compute(key, (k, cur) ->
                    cur == null || now - cur.start() >= WINDOW_MILLIS ? new Window(now, new AtomicInteger()) : cur);
            if (w.count().incrementAndGet() > limit) {
                long retryAfter = Math.max(1, (w.start() + WINDOW_MILLIS - now) / 1000);
                response.setHeader("Retry-After", String.valueOf(retryAfter));
                JsonSecurityErrorHandlers.write(response, HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS",
                        "Bạn thao tác quá nhanh, vui lòng thử lại sau ít phút");
                return;
            }
            if (windows.size() > MAX_TRACKED_KEYS) {
                windows.entrySet().removeIf(e -> now - e.getValue().start() >= WINDOW_MILLIS);
            }
        }
        chain.doFilter(request, response);
    }

    private static String bucket(HttpServletRequest request) {
        String uri = request.getRequestURI();
        boolean post = "POST".equals(request.getMethod());
        if (post && uri.equals("/api/auth/login")) return "login";
        if (post && uri.equals("/api/auth/register")) return "register";
        if (post && uri.equals("/api/auth/refresh")) return "refresh";
        if (post && uri.startsWith("/api/posts/") && uri.endsWith("/comments")) return "comment";
        return "general";
    }

    private int limit(String bucket) {
        return switch (bucket) {
            case "login" -> props.getLogin();
            case "register" -> props.getRegister();
            case "refresh" -> props.getRefresh();
            case "comment" -> props.getComment();
            default -> props.getGeneral();
        };
    }
}
