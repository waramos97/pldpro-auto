package br.cepel.dpc.pldpro_auto.infrastructure.service.impl;

import br.cepel.dpc.pldpro_auto.domain.Host;
import br.cepel.dpc.pldpro_auto.domain.repository.IHostRepository;
import br.cepel.dpc.pldpro_auto.infrastructure.service.IHostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HostService implements IHostService {

    @Autowired
    private IHostRepository hostRepository;

    @Override
    public Host create(String hostName, String userName, Integer port) {

        Host host = new Host();
        host.setHostName(hostName);
        host.setUserName(userName);
        host.setBootstrap(false);
        host.setPort(port);

        return hostRepository.save(host);
    }
}
