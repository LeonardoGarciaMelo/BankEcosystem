package br.com.bankApi.client;

import br.com.bankApi.account.AccountService;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import br.com.bankApi.client.dto.ClientRegistrationDTO;
import br.com.bankApi.credential.Credential;

/**
 * Service layer responsible for handling client business logic.
 * Manages atomic transactions for creating identity and business data.
 */
@ApplicationScoped
public class ClientService {

    private static final Logger log = Logger.getLogger(ClientService.class);

    @Inject
    AccountService accountService;

    /**
     * Registers a new client and their credentials in a single transaction.
     * @param dto The registration data.
     * @return The created {@link Client} entity.
     * @throws WebApplicationException if the username or CPF already exists.
     */
    @Transactional
    public Client registerNewClient(ClientRegistrationDTO dto) {

        log.info("Iniciando tentativa de registro para o CPF: " + CPFMask(dto.cpf()));

        // Security Check: Verify if identity already exists
        if (Credential.findByUsername(dto.username()) != null) {
            log.warn("Falha de registro: Tentativa de uso de username já existente - " + dto.username());
            throw new WebApplicationException("Username already taken", Response.Status.CONFLICT);
        }

        if (Client.findByCpf(dto.cpf()) != null) {
            log.warn("Falha de registro: CPF já cadastrado na base - " + CPFMask(dto.cpf()));
            throw new WebApplicationException("CPF already registered", Response.Status.CONFLICT);
        }

        // Register Client credentials
        log.info("Gerando hash criptográfico para as credenciais...");
        Credential credential = new Credential();
        credential.username = dto.username();
        credential.password = BcryptUtil.bcryptHash(dto.password());
        credential.persist();

        // Register the client
        log.info("Criando perfil de negócios do cliente...");
        Client client = new Client();
        client.name = dto.name();
        client.cpf = dto.cpf();
        client.credential = credential;
        client.persist();

        // Creating Client Account
        accountService.createAccount(client);

        log.info("Sucesso! Cliente registrado com ID: " + client.clientId);
        return client;
    }

    /**
     * Helper method to print CPF on logs
     */
    private String CPFMask(String cpf) {
        if (cpf != null && cpf.length() == 11) {
            return "***.***." + cpf.substring(6, 9) + "-" + cpf.substring(9, 11);
        }
        return "CPF_INVALIDO";
    }
}