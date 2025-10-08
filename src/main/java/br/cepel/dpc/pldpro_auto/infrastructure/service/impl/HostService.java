package br.cepel.dpc.pldpro_auto.infrastructure.service.impl;

import br.cepel.dpc.pldpro_auto.domain.Host;
import br.cepel.dpc.pldpro_auto.domain.repository.IHostRepository;
import br.cepel.dpc.pldpro_auto.infrastructure.service.IHostService;
import br.cepel.dpc.pldpro_auto.infrastructure.ssh.SshBootstrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
public class HostService implements IHostService {

    @Autowired
    private IHostRepository hostRepository;

    @Autowired
    private SshBootstrapper bootstrapper;

    private final Path publicKey  = Path.of(System.getProperty("user.home"), ".ssh/id_ed25519.pub");


    @Override
    public Host create(String hostName, String userName, Integer port,String password) throws IOException {

        bootstrapper.installPublicKeyFirstTime(
                hostName, port, userName, password, publicKey);

        Host host = new Host();
        host.setHostName(hostName);
        host.setUserName(userName);
        host.setBootstrap(false);
        host.setPort(port);

        return hostRepository.save(host);



    }
}
