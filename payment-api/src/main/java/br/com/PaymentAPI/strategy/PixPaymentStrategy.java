package br.com.PaymentAPI.strategy;

import br.com.PaymentAPI.client.BankApiClient;
import br.com.PaymentAPI.dto.DepositRequestDTO;
import br.com.PaymentAPI.dto.TransferRequestDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;

/**
 * Concrete implementation of the {@link PaymentStrategy} for PIX payments.
 * <p>
 * PIX: instant transfer, zero fees.
 * - Internal PIX (sourceAccountNumber provided): calls /transfer directly,
 *   which triggers the Anti-Fraud check for large amounts.
 * - External PIX (sourceAccountNumber is null): funds arrive from outside the bank,
 *   so only the destination account is credited.
 * </p>
 */
@ApplicationScoped
public class PixPaymentStrategy implements PaymentStrategy {

    private static final Logger log = Logger.getLogger(PixPaymentStrategy.class);

    @Inject
    @RestClient
    BankApiClient bankApiClient;

    @Override
    public void process(BigDecimal amount, Long destinationAccountNumber, Long sourceAccountNumber) {
        log.infof("Processando pagamento PIX | Destino: %d | Valor: R$ %s", destinationAccountNumber, amount);

        if (sourceAccountNumber != null) {
            // Internal PIX: use /transfer so the Anti-Fraud check is triggered for amounts > R$ 10k.
            // Calling withdraw + deposit separately would bypass the antifraud check entirely.
            log.infof("PIX interno: transferindo R$ %s de %d para %d", amount, sourceAccountNumber, destinationAccountNumber);
            bankApiClient.remoteTransfer(new TransferRequestDTO(sourceAccountNumber, destinationAccountNumber, amount));
        } else {
            // External PIX received: funds come from outside, only credit the destination.
            log.info("PIX externo recebido: creditando destino sem débito interno.");
            bankApiClient.remoteDeposit(new DepositRequestDTO(destinationAccountNumber, amount));
        }

        log.info("Pagamento PIX concluído.");
    }

    @Override
    public boolean isApplicable(String type) {
        return "PIX".equalsIgnoreCase(type);
    }
}