package br.cepel.dpc.pldpro_auto.infrastructure.listener;

import br.cepel.dpc.pldpro_auto.domain.repository.ICasoRepository;
import br.cepel.dpc.pldpro_auto.infrastructure.enums.StatusEnum;
import br.cepel.dpc.pldpro_auto.infrastructure.service.impl.MonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MonitoringStartup {

    @Autowired
    private ICasoRepository casoRepository;

    @Autowired
    private MonitoringService monitoringService;


    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        // reativa monitores para casos que estavam RUNNING quando a app caiu
        casoRepository.findByStatusIn(java.util.List.of(StatusEnum.EXECUTANDO))
                .forEach(caso -> monitoringService.startMonitoring(caso.getId()));
    }
}
