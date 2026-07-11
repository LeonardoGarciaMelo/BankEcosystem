package br.com.bankApi.auth;

import br.com.bankApi.auth.dto.LoginRequestDTO;
import br.com.bankApi.auth.dto.LoginResponseDTO;
import br.com.bankApi.client.Client;
import br.com.bankApi.credential.Credential;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Handles authentication: verifying credentials and issuing signed JWTs.
 * <p>
 * Tokens are signed with the RSA private key configured via
 * {@code mp.jwt.sign.key.location} and are valid for {@link #TOKEN_LIFETIME}.
 * The {@code groups} claim carries the RBAC role — this is the claim
 * {@code @RolesAllowed} checks against on protected endpoints.
 * </p>
 */
@ApplicationScoped
public class AuthService {

    private static final Logger log = Logger.getLogger(AuthService.class);
    private static final Duration TOKEN_LIFETIME = Duration.ofHours(1);

    /**
     * Verifies username/password and, if valid, issues a signed JWT.
     * <p>
     * Uses a generic error message and identical response timing behavior for
     * "user not found" and "wrong password" — this prevents username enumeration,
     * where an attacker could otherwise learn which usernames exist based on
     * different error messages or response codes.
     * </p>
     *
     * @param dto username + password from the request body.
     * @return a signed JWT and its metadata.
     * @throws WebApplicationException (401) if credentials are invalid or the account is disabled.
     */
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO dto) {
        Credential credential = Credential.findByUsername(dto.username());

        if (credential == null || !BcryptUtil.matches(dto.password(), credential.password)) {
            log.warnf("Failed login attempt for username: %s", dto.username());
            throw new WebApplicationException("Invalid username or password", Response.Status.UNAUTHORIZED);
        }

        if (!Boolean.TRUE.equals(credential.status)) {
            log.warnf("Login attempt on disabled account: %s", dto.username());
            throw new WebApplicationException("Account is disabled", Response.Status.UNAUTHORIZED);
        }

        Client client = Client.find("credential", credential).firstResult();

        if (client == null) {
            log.errorf("Credential %s has no associated Client — data integrity issue", credential.id);
            throw new WebApplicationException("Account data inconsistent", Response.Status.INTERNAL_SERVER_ERROR);
        }

        credential.lastLoginAt = LocalDateTime.now();
        credential.failedLoginAttempts = 0;

        String token = Jwt.issuer("bank-api")
                .upn(credential.username)
                .subject(client.clientId.toString())
                .claim("clientId", client.clientId.toString())
                .groups(Set.of(credential.role))
                .expiresIn(TOKEN_LIFETIME)
                .sign();

        log.infof("Successful login: %s (role=%s)", credential.username, credential.role);

        return new LoginResponseDTO(token, "Bearer", TOKEN_LIFETIME.toSeconds());
    }
}
