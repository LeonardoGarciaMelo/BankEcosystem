package br.com.bankApi.client;

import br.com.bankApi.credential.Credential;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.UUID;

/**
 * Represents a customer within the banking system.
 * <p>
 * This entity stores the business and personal data of the client.
 * It maintains a strict one-to-one relationship with the {@link Credential} entity,
 * ensuring that identity and security concerns are decoupled from business domain logic.
 * </p>
 *
 * @version 1.0
 * @since 2026-04-17
 */
@Entity
@Table(name = "client")
public class Client extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "client_id", updatable = false, nullable = false)
    public UUID clientId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credential_id", referencedColumnName = "id", unique = true, nullable = false)
    public Credential credential;

    @Column(nullable = false, length = 255)
    public String name;

    @Column(unique = true, nullable = false, length = 11)
    public String cpf;

    /**
     * Retrieves a client based on their unique CPF.
     *
     * @param cpf The exact string representing the client's CPF.
     * @return The {@link Client} entity if found, otherwise {@code null}.
     */
    public static Client findByCpf(String cpf) {
        return find("cpf", cpf).firstResult();
    }
}
