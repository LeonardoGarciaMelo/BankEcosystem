package br.com.bankApi.transaction;

import br.com.bankApi.account.Account;
import br.com.bankApi.transaction.dto.SuspiciousTransferDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Core engine for financial movements and ledger management.
 * <p>
 * Ensures strict compliance with ACID properties during transfers.
 * Prevents overdrafts and maintains an immutable audit trail of every movement.
 * </p>
 */
@ApplicationScoped
public class TransactionService {

    private static final Logger log = Logger.getLogger(TransactionService.class);

    @Inject
    @Channel("antifraud-out")
    Emitter<SuspiciousTransferDTO> antiFraudEmitter;

    BigDecimal WARNING_AMOUNT = new BigDecimal("10000");

    /**
     * Executes an atomic transfer between two internal accounts.
     * <p>
     * 1. Validates funds in the origin account.
     * 2. Checks that neither account is blocked.
     * 3. Adjusts balances using BigDecimal math.
     * 4. Persists an immutable Transaction record.
     * </p>
     *
     * @param originAccountNumber      Origin account number.
     * @param destinationAccountNumber Destination account number.
     * @param amount                   Non-negative value to transfer.
     * @return The persisted {@link Transaction} receipt.
     */
    @Transactional
    public Transaction transfer(Long originAccountNumber, Long destinationAccountNumber, BigDecimal amount) {
        log.info(String.format("Solicitação de transferência: %d -> %d | Valor: %s", originAccountNumber,
                destinationAccountNumber, amount));

        // Basic business validation
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Tentativa de transferência com valor zero ou negativo.");
            throw new WebApplicationException("Transfer amount must be greater than zero", Response.Status.BAD_REQUEST);
        }
        if (originAccountNumber.equals(destinationAccountNumber)) {
            log.warn("Tentativa de transferência para a própria conta.");
            throw new WebApplicationException("Cannot transfer to the same account", Response.Status.BAD_REQUEST);
        }

        // Search for the accounts
        Account originAccount = Account.findByNumber(originAccountNumber);
        if (originAccount == null) {
            log.warn("Conta de origem não encontrada: " + originAccountNumber);
            throw new WebApplicationException("Origin account not found", Response.Status.NOT_FOUND);
        }

        Account destAccount = Account.findByNumber(destinationAccountNumber);
        if (destAccount == null) {
            log.warn("Conta de destino não encontrada: " + destinationAccountNumber);
            throw new WebApplicationException("Destination account not found", Response.Status.NOT_FOUND);
        }

        // FIX: check if either account is blocked before allowing the transfer
        if (Boolean.TRUE.equals(originAccount.blocked)) {
            log.warn("Transferência bloqueada: conta de origem " + originAccountNumber + " está bloqueada.");
            throw new WebApplicationException("Origin account is blocked", Response.Status.FORBIDDEN);
        }
        if (Boolean.TRUE.equals(destAccount.blocked)) {
            log.warn("Transferência bloqueada: conta de destino " + destinationAccountNumber + " está bloqueada.");
            throw new WebApplicationException("Destination account is blocked", Response.Status.FORBIDDEN);
        }

        // Balance validation
        if (originAccount.balance.compareTo(amount) < 0) {
            log.warn(String.format("Saldo insuficiente. Conta %d tentou transferir R$ %s mas tem R$ %s",
                    originAccountNumber, amount.toString(), originAccount.balance.toString()));
            throw new WebApplicationException("Insufficient funds", Response.Status.PAYMENT_REQUIRED);
        }

        // Financial movement
        log.info("Processando movimentação financeira...");
        originAccount.balance = originAccount.balance.subtract(amount);
        destAccount.balance = destAccount.balance.add(amount);

        // Receipt generation
        Transaction transaction = new Transaction();
        transaction.originAccount = originAccount;
        transaction.destinationAccount = destAccount;
        transaction.value = amount;
        transaction.type = TransactionType.TRANSFER;
        transaction.description = "Transferência entre contas";

        transaction.persist();

        log.info("Transferência concluída com sucesso! ID da Transação: " + transaction.id);

        // Send to RabbitMQ queue to be verified by the Anti-Fraud service
        if (amount.compareTo(WARNING_AMOUNT) > 0) {
            log.info("Alerta: Transferência vultosa detectada. Notificando Anti-Fraude.");

            SuspiciousTransferDTO payload = new SuspiciousTransferDTO(
                    originAccount.number,
                    destAccount.number,
                    amount,
                    transaction.date,
                    "PENDING_ANALYSIS"
            );

            antiFraudEmitter.send(payload);
        }

        return transaction;
    }

    /**
     * Executes a cash deposit into a specific account.
     * <p>
     * Increases the account balance and records an immutable Transaction of type DEPOSIT.
     * The origin account is intentionally left null as the funds originate externally.
     * </p>
     *
     * @param toNumber The destination account number.
     * @param amount   The non-negative value to deposit.
     * @return The persisted {@link Transaction} receipt.
     */
    @Transactional
    public Transaction deposit(Long toNumber, BigDecimal amount) {
        log.info(String.format("Solicitação de depósito: Conta %d | Valor: %s", toNumber, amount));

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WebApplicationException("Deposit amount must be positive", Response.Status.BAD_REQUEST);
        }

        Account account = Account.findByNumber(toNumber);

        if (account == null) {
            log.warn("Depósito falhou: Conta não encontrada.");
            throw new WebApplicationException("Account not found", Response.Status.NOT_FOUND);
        }

        account.balance = account.balance.add(amount);

        Transaction transaction = new Transaction();
        transaction.destinationAccount = account;
        transaction.originAccount = null;
        transaction.value = amount;
        transaction.type = TransactionType.DEPOSIT;
        transaction.description = "Depósito em espécie/externo";

        transaction.persist();

        log.info("Depósito finalizado. ID da transação: " + transaction.id);
        return transaction;
    }

    /**
     * Executes a cash withdrawal from a specific account.
     * <p>
     * Decreases the account balance if funds are available and records a WITHDRAWAL transaction.
     * The destination account is null as the funds leave the system.
     * </p>
     */
    @Transactional
    public Transaction withdraw(Long fromNumber, BigDecimal amount) {
        log.info(String.format("Solicitação de saque: Conta %d | Valor: %s", fromNumber, amount));

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WebApplicationException("Withdrawal amount must be positive", Response.Status.BAD_REQUEST);
        }

        Account account = Account.findByNumber(fromNumber);

        if (account == null) {
            log.warn("Saque falhou: Conta não encontrada.");
            throw new WebApplicationException("Account not found", Response.Status.NOT_FOUND);
        }

        // FIX: check if account is blocked before allowing the withdrawal
        if (Boolean.TRUE.equals(account.blocked)) {
            log.warn("Saque bloqueado: conta " + fromNumber + " está bloqueada.");
            throw new WebApplicationException("Account is blocked", Response.Status.FORBIDDEN);
        }

        // Balance validation
        if (account.balance.compareTo(amount) < 0) {
            log.warn("Saque abortado: Saldo insuficiente na conta " + fromNumber);
            throw new WebApplicationException("Insufficient funds", Response.Status.PAYMENT_REQUIRED);
        }

        account.balance = account.balance.subtract(amount);

        Transaction transaction = new Transaction();
        transaction.originAccount = account;
        transaction.destinationAccount = null;
        transaction.value = amount;
        transaction.type = TransactionType.WITHDRAWAL;
        transaction.description = "Saque em espécie/caixa eletrônico";

        transaction.persist();

        log.info("Saque finalizado. ID da transação: " + transaction.id);
        return transaction;
    }

    /**
     * Charges an administrative or service fee to an account.
     * <p>
     * Acts similarly to a withdrawal, but is categorized differently for audit and revenue tracking.
     * </p>
     *
     * @param fromNumber  The account to be charged.
     * @param amount      The fee amount.
     * @param description Context for the fee (e.g., "Monthly maintenance").
     * @return The persisted {@link Transaction} receipt.
     */
    @Transactional
    public Transaction chargeFee(Long fromNumber, BigDecimal amount, String description) {
        log.info(String.format("Cobrança de tarifa: Conta %d | Valor: %s", fromNumber, amount));

        Account account = Account.findByNumber(fromNumber);
        if (account == null) throw new WebApplicationException("Account not found", Response.Status.NOT_FOUND);

        if (account.balance.compareTo(amount) < 0) {
            throw new WebApplicationException("Insufficient funds for fee", Response.Status.PAYMENT_REQUIRED);
        }

        account.balance = account.balance.subtract(amount);

        Transaction transaction = new Transaction();
        transaction.originAccount = account;
        transaction.destinationAccount = null;
        transaction.value = amount;
        transaction.type = TransactionType.FEE;
        transaction.description = description != null ? description : "Tarifa bancária padrão";

        transaction.persist();
        return transaction;
    }

    /**
     * Reverses a previous transaction using double-entry bookkeeping principles.
     * <p>
     * Does not delete or update the original transaction. Instead, it creates a new
     * REFUND transaction that inverses the flow of funds and links back to the original ledger entry.
     * Validates that a transaction is not refunded more than once.
     * </p>
     *
     * @param originaltransactionId The UUID of the transaction to reverse.
     * @return The new {@link Transaction} representing the refund.
     * @throws WebApplicationException (HTTP 409) if the transaction is already refunded.
     */
    @Transactional
    public Transaction refund(UUID originaltransactionId) {
        log.info("Iniciando estorno para a transação: " + originaltransactionId);

        Transaction originaltransaction = Transaction.findById(originaltransactionId);
        if (originaltransaction == null) {
            throw new WebApplicationException("Original transaction not found", Response.Status.NOT_FOUND);
        }

        if (originaltransaction.type == TransactionType.REFUND) {
            throw new WebApplicationException("Cannot refund a refund", Response.Status.BAD_REQUEST);
        }

        long alreadyRefunded = Transaction.find("originalTransaction", originaltransaction).count();
        if (alreadyRefunded > 0) {
            throw new WebApplicationException("Transaction has already been refunded", Response.Status.CONFLICT);
        }

        Account accountToDeduct = originaltransaction.destinationAccount;
        Account accountToReceive = originaltransaction.originAccount;

        if (accountToDeduct != null && accountToDeduct.balance.compareTo(originaltransaction.value) < 0) {
            throw new WebApplicationException("Insufficient funds in destination account to process refund", Response.Status.PAYMENT_REQUIRED);
        }

        if (accountToDeduct != null) {
            accountToDeduct.balance = accountToDeduct.balance.subtract(originaltransaction.value);
        }
        if (accountToReceive != null) {
            accountToReceive.balance = accountToReceive.balance.add(originaltransaction.value);
        }

        Transaction refundtransaction = new Transaction();
        refundtransaction.originAccount = accountToDeduct;
        refundtransaction.destinationAccount = accountToReceive;
        refundtransaction.value = originaltransaction.value;
        refundtransaction.type = TransactionType.REFUND;
        refundtransaction.originalTransaction = originaltransaction;
        refundtransaction.description = "Estorno da transação: " + originaltransaction.id;

        refundtransaction.persist();
        return refundtransaction;
    }
}