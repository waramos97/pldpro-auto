package br.cepel.dpc.pldpro_auto.infrastructure.service;

import br.cepel.dpc.pldpro_auto.domain.Host;

public interface IHostService {
    Host create(String hostName, String userName, Integer port);
}
