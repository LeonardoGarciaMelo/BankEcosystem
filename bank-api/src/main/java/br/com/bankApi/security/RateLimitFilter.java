package br.com.bankApi.security;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory sliding-window rate limiter, applied to every endpoint in the API.
 * <p>
 * Different endpoints get different limits based on sensitivity:
 * - /auth/login: 5 requests / 60s — brute-force protection is the most critical case.
 * - /accounts/*&#47;block: 10 requests / 60s — sensitive, but called by a trusted service too.
 * - Everything else: 60 requests / 60s — generic abuse/DoS protection.
 * <p>
 * IMPORTANT LIMITATION (same as before): state is per-JVM-instance, using a
 * ConcurrentHashMap. Fine for a single instance; would need a shared store
 * (Redis) or gateway-level limiting once running multiple replicas.
 * <p>
 * Also note: this limits by IP as reported in X-Forwarded-For, which is only
 * trustworthy behind a reverse proxy that overwrites it. See earlier discussion —
 * without a proxy in front, this header is client-controlled and spoofable.
 */
@Provider
@Priority(Priorities.AUTHENTICATION - 1) // runs before InternalAuthFilter and JWT auth
public class RateLimitFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(RateLimitFilter.class);
    private static final long WINDOW_MILLIS = 60_000; // 60 seconds for every rule below

    /**
     * Ordered list of (pathPattern, maxRequests) rules. The FIRST matching pattern wins,
     * so more specific rules must come before the generic catch-all.
     */
    private static final List<RateRule> RULES = List.of(
            new RateRule(".*/auth/login$", 5),
            new RateRule(".*/accounts/\\d+/block$", 10),
            new RateRule(".*", 60) // catch-all for every other endpoint
    );

    // Key: "clientIp|pathPattern" so each endpoint has its own independent counter per IP.
    private final ConcurrentHashMap<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();

        RateRule rule = RULES.stream()
                .filter(r -> path.matches(r.pathPattern))
                .findFirst()
                .orElseThrow(); // the catch-all guarantees this never actually throws

        String clientIp = requestContext.getHeaderString("X-Forwarded-For");
        if (clientIp == null) {
            clientIp = "unknown";
        }

        String key = clientIp + "|" + rule.pathPattern;
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = requestLog.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MILLIS) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= rule.maxRequests) {
            log.warnf("Rate limit exceeded for IP %s on path %s (limit: %d/60s)", clientIp, path, rule.maxRequests);
            requestContext.abortWith(
                    Response.status(429)
                            .entity("{\"error\":\"Too many requests. Try again later.\"}")
                            .build()
            );
            return;
        }

        timestamps.addLast(now);
    }

    private record RateRule(String pathPattern, int maxRequests) {}
}