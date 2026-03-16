package be.vdab.tcoaching.config;

import be.vdab.tcoaching.api.common.ClientIpResolver;
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
    private static final long ONE_HOUR_MILLIS = 60 * ONE_MINUTE_MILLIS;
    private static final long CLEANUP_TTL_MILLIS = 2 * ONE_HOUR_MILLIS;
    private static final int CLEANUP_EVERY = 500;

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicInteger cleanupCounter = new AtomicInteger();

    private final boolean enabled;
    private final Rule genericRule;
    private final Rule clientLoginRule;
    private final Rule clientRegisterRule;
    private final Rule clientResetRule;
    private final ClientIpResolver clientIpResolver;

    public RateLimitingFilter(
            @Value("${security.rate-limit.enabled:true}") boolean enabled,
            @Value("${security.rate-limit.requests-per-minute:180}") int requestsPerMinute,
            @Value("${security.rate-limit.client-login-per-minute:5}") int clientLoginPerMinute,
            @Value("${security.rate-limit.client-register-per-hour:3}") int clientRegisterPerHour,
            @Value("${security.rate-limit.client-reset-per-hour:3}") int clientResetPerHour,
            ClientIpResolver clientIpResolver
    ) {
        this.enabled = enabled;
        this.genericRule = new Rule("api", Math.max(1, requestsPerMinute), ONE_MINUTE_MILLIS);
        this.clientLoginRule = new Rule("client-login", Math.max(1, clientLoginPerMinute), ONE_MINUTE_MILLIS);
        this.clientRegisterRule = new Rule("client-register", Math.max(1, clientRegisterPerHour), ONE_HOUR_MILLIS);
        this.clientResetRule = new Rule("client-reset", Math.max(1, clientResetPerHour), ONE_HOUR_MILLIS);
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        Rule rule = resolveRule(request);
        long now = System.currentTimeMillis();
        String key = resolveClientKey(request) + ":" + rule.name();
        WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter(now));

        if (!counter.tryConsume(now, rule.limit(), rule.windowMillis())) {
            long retryAfterSeconds = Math.max(1L, counter.retryAfterSeconds(now, rule.windowMillis()));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
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

    private Rule resolveRule(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method) && path != null) {
            if (path.endsWith("/api/client/login")) {
                return clientLoginRule;
            }
            if (path.endsWith("/api/client/register")) {
                return clientRegisterRule;
            }
            if (path.endsWith("/api/client/reset-password/request")) {
                return clientResetRule;
            }
        }
        return genericRule;
    }

    private void maybeCleanup(long now) {
        int current = cleanupCounter.incrementAndGet();
        if (current < CLEANUP_EVERY) {
            return;
        }
        if (!cleanupCounter.compareAndSet(current, 0)) {
            return;
        }
        counters.entrySet().removeIf(entry -> now - entry.getValue().getLastSeen() > CLEANUP_TTL_MILLIS);
    }

    private String resolveClientKey(HttpServletRequest request) {
        return clientIpResolver.resolve(request);
    }

    private record Rule(String name, int limit, long windowMillis) {
    }

    private static final class WindowCounter {
        private long windowStart;
        private int count;
        private long lastSeen;

        private WindowCounter(long now) {
            this.windowStart = now;
            this.lastSeen = now;
        }

        private synchronized boolean tryConsume(long now, int limit, long windowMillis) {
            if (now - windowStart >= windowMillis) {
                windowStart = now;
                count = 0;
            }
            lastSeen = now;
            count++;
            return count <= limit;
        }

        private synchronized long retryAfterSeconds(long now, long windowMillis) {
            long remainingMillis = Math.max(1L, windowMillis - (now - windowStart));
            return (remainingMillis + 999L) / 1000L;
        }

        private synchronized long getLastSeen() {
            return lastSeen;
        }
    }
}
