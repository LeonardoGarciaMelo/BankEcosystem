package br.com.PaymentAPI.client;

import br.com.PaymentAPI.dto.DepositRequestDTO;
import br.com.PaymentAPI.dto.TransferRequestDTO;
import br.com.PaymentAPI.dto.WithdrawRequestDTO;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST Client interface for communication with the Bank API.
 * The base URL is configured in application.properties.
 */
@RegisterRestClient
@Path("/transactions")
public interface BankApiClient {

    @POST
    @Path("/deposit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    void remoteDeposit(DepositRequestDTO request);

    @POST
    @Path("/withdraw")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    void remoteWithdraw(WithdrawRequestDTO request);

    /**
     * Calls the bank-api /transactions/transfer endpoint.
     * This is the only path that triggers the Anti-Fraud check for large amounts.
     * PIX internal transfers must use this method instead of separate withdraw + deposit.
     */
    @POST
    @Path("/transfer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    void remoteTransfer(TransferRequestDTO request);
}