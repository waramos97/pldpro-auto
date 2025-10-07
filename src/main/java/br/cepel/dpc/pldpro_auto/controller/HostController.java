package br.cepel.dpc.pldpro_auto.controller;

import br.cepel.dpc.pldpro_auto.dto.HostRequest;
import br.cepel.dpc.pldpro_auto.infrastructure.service.IHostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/host")
public class HostController {

    @Autowired
    private IHostService hostService;

    @PostMapping
    ResponseEntity<?> createHost(@RequestBody HostRequest hostRequest){
        return ResponseEntity.ok(hostService.create(hostRequest.host(),
                hostRequest.user(),
                hostRequest.port()));
    }
}
