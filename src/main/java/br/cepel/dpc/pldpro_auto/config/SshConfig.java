package br.cepel.dpc.pldpro_auto.config;


import br.cepel.dpc.pldpro_auto.infrastructure.ssh.SshBootstrapper;
import br.cepel.dpc.pldpro_auto.infrastructure.ssh.SshClientService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SshConfig {
    @Bean public SshBootstrapper sshBootstrapper() { return new SshBootstrapper(); }
    @Bean public SshClientService sshClientService() { return new SshClientService(); }
}
