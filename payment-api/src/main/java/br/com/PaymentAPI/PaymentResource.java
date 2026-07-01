package br.com.PaymentAPI;

import br.com.PaymentAPI.dto.PaymentRequestDTO;
import br.com.PaymentAPI.dto.PaymentResponseDTO;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/**
 * REST endpoint for handling incoming payment requests.
 * <p>
 * Acts strictly as a routing layer, delegating business logic to {@link PaymentProcessor}.
 * </p>
 */
@Path("/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentResource {

    @Inject
    PaymentProcessor processor;

    /**
     * Receives a payment payload and triggers the processing pipeline.
     *
     * @param request The validated payment details from the request body.
     * @return A HTTP response indicating the success of the operation.
     */
    @POST
    public Response processPayment(@Valid PaymentRequestDTO request) {
        processor.execute(
                request.method(),
                request.amount(),
                request.destinationAccountNumber(),
                request.sourceAccountNumber()
        );

        return Response.ok(new PaymentResponseDTO(
                200,
                "Payment processed successfully",
                request.method(),
                request.amount()
        )).build();
    }
}