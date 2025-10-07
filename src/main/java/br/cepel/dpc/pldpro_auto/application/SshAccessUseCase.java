package br.cepel.dpc.pldpro_auto.application;



import br.cepel.dpc.pldpro_auto.domain.Host;
import br.cepel.dpc.pldpro_auto.domain.repository.IHostRepository;
import br.cepel.dpc.pldpro_auto.dto.AccessHostRequest;
import br.cepel.dpc.pldpro_auto.dto.AccessHostResponse;
import br.cepel.dpc.pldpro_auto.infrastructure.ssh.SshBootstrapper;
import br.cepel.dpc.pldpro_auto.infrastructure.ssh.SshClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class SshAccessUseCase {

    @Autowired
    private SshBootstrapper bootstrapper;

    @Autowired
    private SshClientService client;

    @Autowired
    private IHostRepository hostRepository;

    private final Path privateKey = Path.of(System.getProperty("user.home"), ".ssh/id_ed25519");
    private final Path publicKey  = Path.of(System.getProperty("user.home"), ".ssh/id_ed25519.pub");
    private final Path knownHosts = Path.of(System.getProperty("user.home"), ".ssh/known_hosts");


    public AccessHostResponse execute(AccessHostRequest req) throws Exception {
        ensureLocalKeyPair();

        Optional<Host> oHost = hostRepository.findByHostName(req.hostName());

        if(oHost.isEmpty()){
            throw  new Exception("Host não encontrado");
        }

        Host host = oHost.get();

        if (Boolean.TRUE.equals(host.getBootstrap())) {
            if (host.ge == null || req.password().isBlank()) {
                throw new IllegalArgumentException("password é obrigatório quando bootstrap=true");
            }
            bootstrapper.installPublicKeyFirstTime(
                    req.host(), req.port(), req.user(), req.password(), publicKey
            );
        }

        var r = client.exec(
                req.host(), req.port(), req.user(),
                (req.command() == null || req.command().isBlank()) ? "echo OK" : req.command()
        );
        return new AccessHostResponse(r.exitCode(), r.stdout(), r.stderr());
    }

    private void ensureLocalKeyPair() throws Exception {
        if (Files.notExists(privateKey) || Files.notExists(publicKey)) {
            new ProcessBuilder("ssh-keygen", "-t", "ed25519", "-N", "", "-f", privateKey.toString())
                    .inheritIO().start().waitFor();
        }
        if (Files.notExists(knownHosts)) {
            Files.createDirectories(knownHosts.getParent());
            Files.createFile(knownHosts);
        }
    }
}
