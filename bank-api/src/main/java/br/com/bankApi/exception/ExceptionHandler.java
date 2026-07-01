package br.com.bankApi.exception;

import br.com.bankApi.exception.dto.ErrorResponseDTO;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import java.time.LocalDateTime;

/**
 * Global Exception Handler to intercept errors before they reach the user.
 * Prevents Stack Traces from leaking (AppSec) and formats output neatly.
 */
public class ExceptionHandler {

    @Context
    UriInfo info;

    /**
     * Intercepts 400 Bad Request from @Valid (e.g., wrong CPF format).
     */
    @ServerExceptionMapper
    public RestResponse<ErrorResponseDTO> handleValidationException(ConstraintViolationException ex) {
        // Pega a primeira mensagem de erro da validação do DTO
        String errorMsg = ex.getConstraintViolations().iterator().next().getMessage();

        ErrorResponseDTO error = new ErrorResponseDTO(400, errorMsg, info.getPath(), LocalDateTime.now());
        return RestResponse.status(Response.Status.BAD_REQUEST, error);
    }

    /**
     * Intercepts Business Rules exceptions (e.g., "CPF already exists").
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
     * Intercepts unexpected Erro 500 (The ultimate shield).
     */
    @ServerExceptionMapper
    public RestResponse<ErrorResponseDTO> handleGenericException(Throwable ex) {
        ex.printStackTrace();

        ErrorResponseDTO error = new ErrorResponseDTO(500, "Internal Server Error. Please contact support.", info.getPath(), LocalDateTime.now());
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, error);
    }
}