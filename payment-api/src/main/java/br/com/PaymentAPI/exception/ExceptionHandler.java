package br.com.PaymentAPI.exception;

import br.com.PaymentAPI.dto.ErrorResponseDTO;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.time.LocalDateTime;

/**
 * Global Exception Handler — intercepts all errors before they reach the client.
 * Prevents stack traces from leaking and formats output consistently.
 * <p>
 * Handles three scenarios specific to a gateway API:
 * - Validation errors from incoming requests (@Valid)
 * - Business rule errors thrown manually (WebApplicationException)
 * - Errors propagated from the Bank API (upstream failures)
 * - Unexpected internal errors (last resort)
 * </p>
 */
public class ExceptionHandler {

    @Context
    UriInfo info;

    /**
     * Intercepts 400 Bad Request from @Valid on PaymentRequestDTO.
     * Example: missing required field, negative amount.
     */
    @ServerExceptionMapper
    public RestResponse<ErrorResponseDTO> handleValidationException(ConstraintViolationException ex) {
        String errorMsg = ex.getConstraintViolations().iterator().next().getMessage();
        ErrorResponseDTO error = new ErrorResponseDTO(400, errorMsg, info.getPath(), LocalDateTime.now());
        return RestResponse.status(Response.Status.BAD_REQUEST, error);
    }

    /**
     * Intercepts WebApplicationExceptions — both thrown manually in this API
     * and propagated from the Bank API (e.g., insufficient funds, account not found).
     * The status code from the upstream response is preserved.
     */
    @ServerExceptionMapper
    public RestResponse<ErrorResponseDTO> handleWebAppException(WebApplicationException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                ex.getResponse().getStatus(),
                ex.getMessage(),
                info.getPath(),
                LocalDateTime.now()
        );
        return RestResponse.status(ex.getResponse().getStatusInfo(), error);
    }

    /**
     * Last resort — catches anything unexpected (NullPointerException, bank-api unreachable, etc.).
     * Always returns 500 and never exposes internal details to the client.
     */
    @ServerExceptionMapper
    public RestResponse<ErrorResponseDTO> handleGenericException(Throwable ex) {
        ex.printStackTrace();
        ErrorResponseDTO error = new ErrorResponseDTO(
                500,
                "Internal Server Error. Please contact support.",
                info.getPath(),
                LocalDateTime.now()
        );
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, error);
    }
}