package br.com.bankApi.account;

import br.com.bankApi.account.dto.StatementResponseDTO;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * REST API Endpoints for Account-focused operations.
 */
@Path("/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountResource {

    @Inject
    AccountService accountService;

    /**
     * {@link JsonWebToken} is CDI-injectable and represents the caller's token,
     * IF one was provided and validated by SmallRye JWT. It is null/anonymous
     * when no Bearer token is present — which is expected for the antifraud-api
     * service call (which uses X-Internal-Token instead, never a JWT).
     */
    @Inject
    JsonWebToken jwt;

    @ConfigProperty(name = "internal.api.key")
    String expectedInternalKey;

    /**
     * Retrieves the complete bank statement for an account.
     * <p>
     * Requires a valid JWT with role CLIENT or ADMIN. A CLIENT can only view
     * their own account's statement — enforced by comparing the account owner's
     * clientId against the {@code clientId} claim in the caller's token. An ADMIN
     * can view any account (e.g. customer support, fraud investigation).
     */
    @GET
    @Path("/{number}/statement")
    @RolesAllowed({"CLIENT", "ADMIN"})
    public Response getStatement(@PathParam("number") Long number) {

        boolean isAdmin = jwt.getGroups().contains("ADMIN");

        if (!isAdmin) {
            String callerClientId = jwt.getClaim("clientId");
            boolean ownsAccount = accountService.isOwnedByClient(number, callerClientId);
            if (!ownsAccount) {
                // 404 instead of 403 here is a deliberate choice: it avoids confirming
                // to an attacker that an account number they guessed actually exists.
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        }

        StatementResponseDTO statement = accountService.getStatement(number);
        return Response.status(Response.Status.OK).entity(statement).build();
    }

    /**
     * Blocks an account, preventing it from performing any further transactions.
     * <p>
     * Two independent ways to authorize this call:
     * <ol>
     *   <li>A correct {@code X-Internal-Token} header (antifraud-api's automated calls).
     *       {@link br.com.bankApi.security.InternalAuthFilter} already rejects wrong
     *       tokens before this method runs; here we only check for a CORRECT one.</li>
     *   <li>A valid JWT with role ADMIN (a human operator manually blocking an account).</li>
     * </ol>
     * If neither is satisfied, the request is rejected.
     */
    @PATCH
    @Path("/{number}/block")
    public Response blockAccount(
            @PathParam("number") Long number,
            @HeaderParam("X-Internal-Token") String internalToken
    ) {
        boolean isTrustedService = internalToken != null && internalToken.equals(expectedInternalKey);
        boolean isAdmin = jwt != null && jwt.getGroups() != null && jwt.getGroups().contains("ADMIN");

        if (!isTrustedService && !isAdmin) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\":\"Requires internal service token or ADMIN role\"}")
                    .build();
        }

        accountService.blockAccount(number);
        return Response.ok().build();
    }
}