package br.com.PaymentAPI.strategy;

import br.com.PaymentAPI.client.BankApiClient;
import br.com.PaymentAPI.dto.DepositRequestDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concrete implementation of the {@link PaymentStrategy} for Credit Card payments.
 * <p>
 * Applies a 5% processing fee over the original amount.
 * The merchant receives the net amount (original - 5% fee); the fee is retained by the gateway.
 * The customer pays via external credit card — no internal account debit occurs.
 * </p>
 */
@ApplicationScoped
public class CreditPaymentStrategy implements PaymentStrategy {

    private static final Logger log = Logger.getLogger(CreditPaymentStrategy.class);

    @Inject
    @RestClient
    BankApiClient bankClient;

    @Override
    public void process(BigDecimal amount, Long destinationAccountNumber, Long sourceAccountNumber) {
        // FIX: taxa de 5% retida pelo gateway — lojista recebe (amount - fee), não o valor cheio.
        BigDecimal fee = amount.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = amount.subtract(fee);

        log.infof("Processando Cartão de Crédito | Conta destino: %d", destinationAccountNumber);
        log.infof("Valor bruto: R$ %s | Taxa gateway (5%%): R$ %s | Repasse ao lojista: R$ %s",
                amount, fee, netAmount);

        // Lojista recebe o valor líquido; a taxa fica retida no gateway.
        bankClient.remoteDeposit(new DepositRequestDTO(destinationAccountNumber, netAmount));

        log.info("Pagamento Cartão de Crédito concluído.");
    }

    @Override
    public boolean isApplicable(String type) {
        return "CREDIT_CARD".equalsIgnoreCase(type);
    }
}