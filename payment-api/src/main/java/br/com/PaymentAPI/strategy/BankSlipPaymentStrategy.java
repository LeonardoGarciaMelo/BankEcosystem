package br.com.PaymentAPI.strategy;

import br.com.PaymentAPI.client.BankApiClient;
import br.com.PaymentAPI.dto.DepositRequestDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;

/**
 * Concrete implementation of the {@link PaymentStrategy} for Boleto (Bank Slip) payments.
 * <p>
 * Fixed issuance fee of R$ 2.50 is charged on top of the product amount.
 * The merchant receives the net amount (product value - R$ 2.50 fee); the fee is retained by the bank.
 * Settlement follows the D+1 or D+2 cycle typical of boleto operations.
 * </p>
 */
@ApplicationScoped
public class BankSlipPaymentStrategy implements PaymentStrategy {

    private static final Logger log = Logger.getLogger(BankSlipPaymentStrategy.class);

    @Inject
    @RestClient
    BankApiClient bankClient;

    @Override
    public void process(BigDecimal amount, Long destinationAccountNumber, Long sourceAccountNumber) {
        // FIX: taxa fixa de R$ 2.50 retida pelo banco — lojista recebe (amount - taxa), não o valor cheio.
        BigDecimal fee = new BigDecimal("2.50");
        BigDecimal netAmount = amount.subtract(fee);

        log.infof("Processando Boleto | Conta destino: %d", destinationAccountNumber);
        log.infof("Valor produto: R$ %s | Taxa boleto: R$ %s | Repasse ao lojista: R$ %s",
                amount, fee, netAmount);

        // Lojista recebe valor líquido; taxa de R$ 2.50 fica retida no banco.
        bankClient.remoteDeposit(new DepositRequestDTO(destinationAccountNumber, netAmount));

        log.info("Liquidação do boleto concluída (sujeita ao ciclo D+1/D+2).");
    }

    @Override
    public boolean isApplicable(String type) {
        return "BOLETO".equalsIgnoreCase(type);
    }
}