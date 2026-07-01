package br.com.AntiFraudAPI.client;

import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST Client to communicate with the Bank API.
 * URL is configured via the BANK_API_URL environment variable (see application.properties).
 */
@RegisterRestClient
@Path("/accounts")
public interface BankApiClient {

    /**
     * Sends a block request to the Bank API for a specific account.
     * Calls PATCH /accounts/{number}/block
     *
     * @param number The account number to block.
     */
    @PATCH
    @Path("/{number}/block")
    @Produces(MediaType.APPLICATION_JSON)
    void blockAccount(@PathParam("number") Long number);
}