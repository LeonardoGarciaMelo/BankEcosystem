package br.com.bankApi.account;

import br.com.bankApi.client.Client;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Represents a financial account belonging to a {@link Client}.
 * <p>
 * A single client can own multiple accounts (e.g., Checking, Savings).
 * This entity tracks the balance using {@link BigDecimal} to ensure financial precision
 * and prevent rounding errors common in floating-point types.
 * </p>
 *
 * @version 1.0
 * @since 2026-04-17
 */
@Entity
@Table(name = "accounts")
public class Account extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "client_id", nullable = false)
    public Client client;

    @Column(unique = true, nullable = false)
    public Long number;

    @Column(nullable = false, precision = 15, scale = 2)
    public BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    public Boolean blocked = false;

    /**
     * Finds an account by its public account number.
     *
     * @param number The account number to search for.
     * @return The {@link Account} entity if found, otherwise {@code null}.
     */
    public static Account findByNumber(Long number) {
        return find("number", number).firstResult();
    }

    /**
     * Retrieves all accounts belonging to a specific client.
     *
     * @param client The {@link Client} owner.
     * @return A list of {@link Account} entities.
     */
    public static List<Account> listByClient(Client client) {
        return list("client", client);
    }
}