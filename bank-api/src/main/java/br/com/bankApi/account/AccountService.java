package br.com.bankApi.account;

import br.com.bankApi.account.dto.BalanceEffect;
import br.com.bankApi.account.dto.StatementItemDTO;
import br.com.bankApi.account.dto.StatementResponseDTO;
import br.com.bankApi.client.Client;
import br.com.bankApi.transaction.Transaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Service layer responsible for the lifecycle of financial accounts.
 * <p>
 * Handles the generation of unique account numbers and guarantees
 * safe initialization of balances. Designed to be called within
 * broader transactional contexts (e.g., Client registration orchestration).
 * </p>
 *
 * @version 1.1
 * @since 2026-04-20
 */
@ApplicationScoped
public class AccountService {

    private static final Logger log = Logger.getLogger(AccountService.class);
    private final Random random = new Random();

    /**
     * Creates a new, zero-balance account linked to an existing client.
     * <p>
     * This method requires an active transaction to run (TxType.REQUIRED).
     * If the account generation fails, it will flag the broader transaction for rollback.
     * </p>
     *
     * @param client The {@link Client} entity that will own the new account.
     * @return The fully persisted {@link Account} with a generated unique number.
     * @throws WebApplicationException with HTTP 500 if a unique account number cannot be generated after maximum retries.
     */
    @Transactional(Transactional.TxType.REQUIRED)
    public Account createAccount(Client client) {
        log.info("Gerando conta bancária para o cliente...");

        Account account = new Account();
        account.client = client;
        account.balance = BigDecimal.ZERO;
        account.number = generateUniqueAccountNumber();

        account.persist();

        log.info("Conta vinculada com sucesso. Número: " + account.number);
        return account;
    }

    /**
     * Generates a random 6-digit account number and verifies its uniqueness
     * against the database to prevent collisions.
     *
     * @return A guaranteed unique Long representing the account number.
     */
    private Long generateUniqueAccountNumber() {
        Long newNumber;
        int attempts = 0;

        do {
            newNumber = 100000L + random.nextInt(900000);
            if (Account.findByNumber(newNumber) == null) {
                return newNumber;
            }
            attempts++;
        } while (attempts < 10);

        log.error("Falha ao gerar número de conta único após 10 tentativas.");
        throw new WebApplicationException("Error generating account number", Response.Status.INTERNAL_SERVER_ERROR);
    }

    /**
     * Generates a complete bank statement for an account.
     * <p>
     * Retrieves the immutable transaction history and dynamically calculates
     * the effect (Credit/Debit) from the perspective of the requesting account.
     * </p>
     * @param accountNumber The account number passed in the URL.
     * @return DTO containing the current balance and formatted history.
     */
    public StatementResponseDTO getStatement(Long accountNumber) {
        log.info("Gerando extrato para a conta: " + accountNumber);

        Account account = Account.findByNumber(accountNumber);
        if (account == null) {
            throw new WebApplicationException("Account not found", Response.Status.NOT_FOUND);
        }

        List<Transaction> transactions = Transaction.getStatement(account);

        List<StatementItemDTO> history = transactions.stream().map(transaction -> {
            boolean isCredit = transaction.destinationAccount != null && transaction.destinationAccount.id.equals(account.id);
            BalanceEffect effect = isCredit ? BalanceEffect.CREDIT : BalanceEffect.DEBIT;

            return new StatementItemDTO(
                    transaction.id,
                    transaction.date,
                    transaction.type.name(),
                    transaction.value,
                    transaction.description,
                    effect
            );
        }).collect(Collectors.toList());

        log.info("Extrato gerado. Total de registros: " + history.size());
        return new StatementResponseDTO(account.number, account.balance, history);
    }

    /**
     * Blocks an account, preventing further transactions.
     * <p>
     * Called by the Anti-Fraud system when a suspicious transfer is confirmed.
     * Sets the {@code blocked} flag to {@code true} on the account entity.
     * </p>
     *
     * @param accountNumber The number of the account to be blocked.
     * @throws WebApplicationException with HTTP 404 if the account is not found.
     */
    @Transactional
    public void blockAccount(Long accountNumber) {
        log.warnf("Solicitação de bloqueio recebida para a conta: %d", accountNumber);

        Account account = Account.findByNumber(accountNumber);
        if (account == null) {
            throw new WebApplicationException("Account not found", Response.Status.NOT_FOUND);
        }

        account.blocked = true;
        log.errorf("Conta %d BLOQUEADA por suspeita de fraude.", accountNumber);
    }
}