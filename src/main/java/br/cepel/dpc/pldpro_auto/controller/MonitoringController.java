package br.cepel.dpc.pldpro_auto.controller;

import br.cepel.dpc.pldpro_auto.infrastructure.service.impl.MonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/monitor")
public class MonitoringController {

    @Autowired
    private MonitoringService monitoringService;


    @PostMapping("/stop/{casoId}")
    public ResponseEntity<Void> stop(@PathVariable Long casoId) {
        monitoringService.stopMonitoring(casoId);
        return ResponseEntity.noContent().build();
    }
}
