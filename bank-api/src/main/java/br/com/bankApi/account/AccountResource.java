package br.com.bankApi.account;

import br.com.bankApi.account.dto.StatementResponseDTO;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
     * Retrieves the complete bank statement for an account.
     * @param number The account number passed in the URL.
     * @return 200 OK with the statement (StatementResponseDTO).
     */
    @GET
    @Path("/{number}/statement")
    public Response getStatement(@PathParam("number") Long number) {

        StatementResponseDTO statement = accountService.getStatement(number);

        return Response.status(Response.Status.OK).entity(statement).build();
    }

    /**
     * Blocks an account, preventing it from performing any further transactions.
     * <p>
     * Called internally by the Anti-Fraud system when a suspicious transfer is confirmed.
     * </p>
     * @param number The account number to block.
     * @return 200 OK on success.
     */
    @PATCH
    @Path("/{number}/block")
    public Response blockAccount(@PathParam("number") Long number) {
        accountService.blockAccount(number);
        return Response.ok().build();
    }
}