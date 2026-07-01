package br.com.PaymentAPI.strategy;

import br.com.PaymentAPI.client.BankApiClient;
import br.com.PaymentAPI.dto.TransferRequestDTO;
import br.com.PaymentAPI.dto.WithdrawRequestDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;

/**
 * Concrete implementation of the {@link PaymentStrategy} for TED (Wire Transfer) payments.
 * <p>
 * Fixed transfer fee of R$ 5.00 is charged to the source account.
 * Flow:
 *  1. remoteTransfer(source → dest, amount) — moves the funds and triggers the Anti-Fraud check.
 *  2. remoteWithdraw(source, R$ 5.00)       — retains the fee in the bank separately.
 * </p>
 */
@ApplicationScoped
public class TedPaymentStrategy implements PaymentStrategy {

    private static final Logger log = Logger.getLogger(TedPaymentStrategy.class);

    @Inject
    @RestClient
    BankApiClient bankClient;

    @Override
    public void process(BigDecimal amount, Long destinationAccountNumber, Long sourceAccountNumber) {
        if (sourceAccountNumber == null) {
            throw new WebApplicationException(
                    "sourceAccountNumber is required for TED transfers",
                    Response.Status.BAD_REQUEST
            );
        }

        BigDecimal fee = new BigDecimal("5.00");

        log.infof("Processando TED | Origem: %d -> Destino: %d | Valor: R$ %s | Taxa: R$ %s",
                sourceAccountNumber, destinationAccountNumber, amount, fee);

        // for amounts above R$ 10k, same as PIX internal.
        bankClient.remoteTransfer(new TransferRequestDTO(sourceAccountNumber, destinationAccountNumber, amount));
        log.infof("Transferência de R$ %s de %d para %d concluída.", amount, sourceAccountNumber, destinationAccountNumber);

        // The fee stays in the bank (withdraw with no destination).
        bankClient.remoteWithdraw(new WithdrawRequestDTO(sourceAccountNumber, fee));
        log.infof("Taxa TED de R$ %s debitada da conta %d.", fee, sourceAccountNumber);

        log.info("TED concluída (sujeita ao horário do BACEN).");
    }

    @Override
    public boolean isApplicable(String type) {
        return "TED".equalsIgnoreCase(type);
    }
}