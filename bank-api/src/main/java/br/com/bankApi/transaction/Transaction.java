package br.com.bankApi.transaction;

import br.com.bankApi.account.Account;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

/**
 * Represents a financial movement between accounts or an external operation.
 * <p>
 * This entity acts as an immutable ledger. Once a transaction is saved, it cannot be updated.
 * It handles transfers, deposits, withdrawals, and supports self-referencing for refunds
 * ensuring full traceability and compliance with financial audit standards.
 * </p>
 *
 * @version 1.0
 * @since 2026-04-17
 */
@Entity
@Table(name = "transactions")
public class Transaction extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public UUID id;

    // Can be null if it's an external cash deposit.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_account_id", referencedColumnName = "id")
    public Account originAccount;

    // Can be null if it's an ATM cash withdrawal.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id", referencedColumnName = "id")
    public Account destinationAccount;

    @Column(nullable = false, precision = 15, scale = 2)
    public BigDecimal value;

    @Column(nullable = false, updatable = false)
    public LocalDateTime date = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    public TransactionType type;

    @Column(length = 255)
    public String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_transaction_id", referencedColumnName = "id")
    public Transaction originalTransaction;

    /**
     * JPA Callback: Sets the transaction date automatically before saving.
     */
    @PrePersist
    protected void onCreate() {
        this.date = LocalDateTime.now();
    }

    /**
     * Retrieves the entire transaction history for a specific account.
     * Searches for transactions where the account is either the origin OR the destination.
     *
     * @param account The {@link Account} to generate the statement for.
     * @return A list of {@link Transaction} representing the bank statement.
     */
    public static List<Transaction> getStatement(Account account) {
        return find("originAccount = ?1 or destinationAccount = ?1 order by date desc", account).list();
    }
}
