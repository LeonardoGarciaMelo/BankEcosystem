package br.com.bankApi.client;

import br.com.bankApi.client.dto.ClientRegistrationDTO;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * REST Resource providing endpoints for Client management.
 */
@Path("/clients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClientResource {

    @Inject
    ClientService clientService;

    /**
     * Creates a new bank client.
     * * @param dto Validated registration data.
     * @return 201 Created on success.
     */
    @POST
    public Response create(@Valid ClientRegistrationDTO dto) {
        Client client = clientService.registerNewClient(dto);
        return Response.status(Response.Status.CREATED).entity(client).build();
    }

    /**
     * Lists all registered clients.
     * * @return List of {@link Client}.
     */
    @GET
    public List<Client> listAll() {
        return Client.listAll();
    }
}