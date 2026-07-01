package br.com.bankApi.transaction;

import br.com.bankApi.transaction.dto.*;
import br.com.bankApi.transaction.dto.*;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

/**
 * REST API Endpoints for financial operations.
 */
@Path("/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionResource {

    @Inject
    TransactionService transactionService;

    /**
     * Executes an atomic transfer between two accounts.
     * @param dto The validated transfer payload.
     * @return 201 Created with the clean receipt (TransactionResponseDTO).
     */
    @POST
    @Path("/transfer")
    public Response transfer(@Valid TransferRequestDTO dto) {

        Transaction transaction = transactionService.transfer(
                dto.originAccountNumber(),
                dto.destinationAccountNumber(),
                dto.amount()
        );
        TransferResponseDTO receipt = new TransferResponseDTO(
                transaction.id,
                transaction.originAccount.number,
                transaction.destinationAccount.number,
                transaction.value,
                transaction.date,
                transaction.type.name()
        );

        return Response.status(Response.Status.CREATED).entity(receipt).build();
    }

    /**
     * Executes a deposit.
     * @param dto The validated transfer payload.
     * @return 201 Created with the clean receipt (TransactionResponseDTO).
     */
    @POST
    @Path("/deposit")
    public Response deposit(@Valid DepositRequestDTO dto) {

        Transaction transaction = transactionService.deposit(
                dto.destinationAccountNumber(),
                dto.amount()
        );

        DepositResponseDTO receipt = new DepositResponseDTO(
                transaction.id,
                transaction.destinationAccount.number,
                transaction.value,
                transaction.date,
                transaction.type.name()
        );

        return Response.status(Response.Status.CREATED).entity(receipt).build();
    }

    /**
     * Executes an ATM cash withdrawal.
     * @return 201 Created with the clean receipt.
     */
    @POST
    @Path("/withdraw")
    public Response withdraw(@Valid WithdrawalRequestDTO dto) {

        Transaction transaction = transactionService.withdraw(
                dto.originAccountNumber(),
                dto.amount()
        );

        WithdrawalResponseDTO receipt = new WithdrawalResponseDTO(
                transaction.id,
                transaction.originAccount.number,
                transaction.value,
                transaction.date,
                transaction.type.name()
        );

        return Response.status(Response.Status.CREATED).entity(receipt).build();
    }

    /**
     * Charges an administrative fee to a client account.
     *
     * @param dto Validated payload with account details, amount, and optional description.
     * @return 201 Created with {@link FeeResponseDTO}.
     */
    @POST
    @Path("/fee")
    public Response chargeFee(@Valid FeeRequestDTO dto) {
        Transaction transaction = transactionService.chargeFee(
                dto.originAccountNumber(),
                dto.amount(),
                dto.description()
        );

        FeeResponseDTO receipt = new FeeResponseDTO(
                transaction.id, transaction.originAccount.number, transaction.value, transaction.date, transaction.description, transaction.type.name()
        );
        return Response.status(Response.Status.CREATED).entity(receipt).build();
    }

    /**
     * Reverses a successfully completed transaction.
     *
     * @param originaltransactionId The path parameter representing the UUID of the transaction to refund.
     * @return 201 Created with {@link RefundResponseDTO} linking back to the original transaction.
     */
    @POST
    @Path("/{id}/refund")
    public Response refundTransaction(@PathParam("id") UUID originaltransactionId) {
        Transaction transaction = transactionService.refund(originaltransactionId);

        RefundResponseDTO receipt = new RefundResponseDTO(
                transaction.id, transaction.originalTransaction.id, transaction.value, transaction.date, transaction.type.name()
        );
        return Response.status(Response.Status.CREATED).entity(receipt).build();
    }
}