package br.com.AntiFraudAPI;

import br.com.AntiFraudAPI.client.BankApiClient;
import br.com.AntiFraudAPI.dto.SuspiciousTransferDTO;
import io.vertx.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import java.math.BigDecimal;

/**
 * Consumer service responsible for the asynchronous analysis of suspicious transfers.
 * <p>
 * Acts as a background worker listening to the message broker (RabbitMQ) in real-time.
 * It ensures that the main banking API flow is not blocked by heavy security validations
 * and anti-fraud rules.
 * </p>
 */
@ApplicationScoped
public class FraudAnalyzerService {

    private static final Logger log = Logger.getLogger(FraudAnalyzerService.class);

    @Inject
    @RestClient
    BankApiClient bankClient;

    /**
     * Intercepts and processes high-value transfer alerts.
     * <p>
     * Applies security heuristics to determine whether a transaction
     * should be blocked or kept under monitoring.
     * If the value exceeds R$ 100.000, both accounts are blocked via the Bank API.
     * </p>
     *
     * @param rawPayload The raw bytes {@link Buffer} received from RabbitMQ.
     */
    @Incoming("frauds-in")
    public void processFraud(Buffer rawPayload) {

        SuspiciousTransferDTO alert = rawPayload.toJsonObject().mapTo(SuspiciousTransferDTO.class);

        log.warn("Transação suspeita recebida para análise");
        log.info("Conta Origem : " + alert.originAccount());
        log.info("Conta Destino: " + alert.destinationAccount());
        log.info("Valor        : R$ " + alert.amount());

        if (alert.amount().compareTo(new BigDecimal("100000")) > 0) {
            log.error("FRAUDE CONFIRMADA! Valor excede R$ 1000.000. Iniciando bloqueio das contas...");

            // FIX: actually block both accounts via Bank API instead of just logging
            try {
                bankClient.blockAccount(alert.originAccount());
                log.errorf("Conta de origem %d BLOQUEADA.", alert.originAccount());
            } catch (Exception e) {
                log.errorf("Falha ao bloquear conta de origem %d: %s", alert.originAccount(), e.getMessage());
            }

            try {
                bankClient.blockAccount(alert.destinationAccount());
                log.errorf("Conta de destino %d BLOQUEADA.", alert.destinationAccount());
            } catch (Exception e) {
                log.errorf("Falha ao bloquear conta de destino %d: %s", alert.destinationAccount(), e.getMessage());
            }

        } else {
            log.info("Análise concluída. Transação de risco médio. Liberada sob monitoramento.");
        }
    }
}