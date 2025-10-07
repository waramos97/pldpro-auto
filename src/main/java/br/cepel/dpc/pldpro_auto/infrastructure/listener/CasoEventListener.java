package br.cepel.dpc.pldpro_auto.infrastructure.listener;


import br.cepel.dpc.pldpro_auto.infrastructure.event.CasoCreatedEvent;
import br.cepel.dpc.pldpro_auto.infrastructure.service.impl.MonitoringService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CasoEventListener {
    private final MonitoringService monitoringService;

    public CasoEventListener(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    // AFTER_COMMIT garante que o caso foi persistido com sucesso antes de agendar monitor
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCaseCreated(CasoCreatedEvent event) {
        monitoringService.startMonitoring(event.getCasoId());
    }
}
