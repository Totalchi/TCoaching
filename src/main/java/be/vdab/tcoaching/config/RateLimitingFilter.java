package be.vdab.tcoaching.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private static final long ONE_MINUTE_MILLIS = 60_000L;
    private static final long CLEANUP_TTL_MILLIS = 10 * ONE_MINUTE_MILLIS;
    private static final int CLEANUP_EVERY = 500;

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicInteger cleanupCounter = new AtomicInteger();

    private final boolean enabled;
    private final int requestsPerMinute;

    public RateLimitingFilter(
            @Value("${security.rate-limit.enabled:true}") boolean enabled,
            @Value("${security.rate-limit.requests-per-minute:180}") int requestsPerMinute
    ) {
        this.enabled = enabled;
        this.requestsPerMinute = Math.max(1, requestsPerMinute);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        String key = resolveClientKey(request);
        WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter(now));

        if (!counter.tryConsume(now, requestsPerMinute)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            return;
        }

        maybeCleanup(now);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        String apiPrefix = (contextPath == null ? "" : contextPath) + "/api/";
        return requestUri == null || !requestUri.startsWith(apiPrefix);
    }

    private void maybeCleanup(long now) {
        if (cleanupCounter.incrementAndGet() % CLEANUP_EVERY != 0) {
            return;
        }
        counters.entrySet().removeIf(entry -> now - entry.getValue().getLastSeen() > CLEANUP_TTL_MILLIS);
    }

    private String resolveClientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private static final class WindowCounter {
        private long windowStart;
        private int count;
        private long lastSeen;

        private WindowCounter(long now) {
            this.windowStart = now;
            this.lastSeen = now;
        }

        private synchronized boolean tryConsume(long now, int limit) {
            if (now - windowStart >= ONE_MINUTE_MILLIS) {
                windowStart = now;
                count = 0;
            }
            lastSeen = now;
            count++;
            return count <= limit;
        }

        private synchronized long getLastSeen() {
            return lastSeen;
        }
    }
}
