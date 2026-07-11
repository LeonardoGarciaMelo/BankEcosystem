package br.com.PaymentAPI.security;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory sliding-window rate limiter for payment-api.
 * <p>
 * Limits POST /payments to 20 requests / 60s per IP — high enough for legitimate
 * bursts (a merchant processing several transactions), low enough to block
 * flood/DoS attempts against the payment pipeline.
 * <p>
 * Same limitations as bank-api's RateLimitFilter: per-instance state, and
 * X-Forwarded-For is only trustworthy behind a reverse proxy.
 */
@Provider
@Priority(Priorities.AUTHENTICATION - 1)
public class RateLimitFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(RateLimitFilter.class);
    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_REQUESTS = 20;

    private final ConcurrentHashMap<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();

        boolean isPaymentsEndpoint = path.equals("payments") || path.equals("/payments");
        if (!isPaymentsEndpoint) {
            return;
        }

        String clientIp = requestContext.getHeaderString("X-Forwarded-For");
        if (clientIp == null) {
            clientIp = "unknown";
        }

        long now = System.currentTimeMillis();
        Deque<Long> timestamps = requestLog.computeIfAbsent(clientIp, k -> new ConcurrentLinkedDeque<>());

        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MILLIS) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= MAX_REQUESTS) {
            log.warnf("Rate limit exceeded for IP %s on /payments", clientIp);
            requestContext.abortWith(
                    Response.status(429)
                            .entity("{\"error\":\"Too many requests. Try again later.\"}")
                            .build()
            );
            return;
        }

        timestamps.addLast(now);
    }
}