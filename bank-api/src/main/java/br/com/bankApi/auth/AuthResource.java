package br.com.bankApi.auth;

import br.com.bankApi.auth.dto.LoginRequestDTO;
import br.com.bankApi.auth.dto.LoginResponseDTO;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Authentication endpoint. This is intentionally the ONLY endpoint in bank-api
 * that requires no prior authentication — everyone starts here to obtain a token.
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequestDTO request) {
        LoginResponseDTO response = authService.login(request);
        return Response.ok(response).build();
    }
}