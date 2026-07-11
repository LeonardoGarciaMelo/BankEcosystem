package br.com.bankApi.security;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.Objects;
import org.jboss.logging.Logger;

/**
 * Service-to-service authentication filter.
 * <p>
 * Protects internal-only endpoints (currently just account blocking) with a shared
 * secret header. This is NOT user authentication — it verifies that the CALLER is
 * a trusted internal service (antifraud-api), not a human end-user.
 * <p>
 * In a production system, this would typically be replaced by mTLS or an OAuth2
 * client-credentials flow between services. A shared API key is the first layer
 * of defense: cheap to implement, and it closes the most severe gap (an
 * unauthenticated endpoint that can block any customer's account) while a more
 * robust mechanism is designed.
 * <p>
 * {@code @Priority(Priorities.AUTHENTICATION)} ensures this filter runs before
 * any business logic — the request never reaches {@link br.com.bankApi.account.AccountResource}
 * if the check fails.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class InternalAuthFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(InternalAuthFilter.class.getName());
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    @ConfigProperty(name = "internal.api.key")
    String expectedApiKey;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();

        boolean isInternalOnlyEndpoint = path.matches("^.*/accounts/\\d+/block$");
        if (!isInternalOnlyEndpoint){
            return;
        }

        String token = requestContext.getHeaderString(INTERNAL_TOKEN_HEADER);

        if (token == null) {
            return;
        }

        if (!token.equals(expectedApiKey)) {
            log.warnf("Blocked request to %s: invalid %s header", path, INTERNAL_TOKEN_HEADER);
            requestContext.abortWith(
                    Response.status(Response.Status.FORBIDDEN)
                            .entity("{\"error\":\"This endpoint is restricted to internal services\"}")
                            .build()
            );
        }
    }
}
