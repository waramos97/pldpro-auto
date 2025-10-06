package br.cepel.dpc.pldpro_auto.infrastructure.ssh;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.password.PasswordFinder;

import java.nio.file.Path;

public class SshClientService {

    private final Path privateKey; // ~/.ssh/id_ed25519
    private final Path knownHosts; // ~/.ssh/known_hosts

    public SshClientService() {
        this(Path.of(System.getProperty("user.home"), ".ssh/id_ed25519"),
                Path.of(System.getProperty("user.home"), ".ssh/known_hosts"));
    }

    public SshClientService(Path privateKey, Path knownHosts) {
        this.privateKey = privateKey;
        this.knownHosts = knownHosts;
    }

    public record ExecResult(int exitCode, String stdout, String stderr) {}

    public ExecResult exec(String host, int port, String user, String command) throws Exception {
        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new OpenSSHKnownHosts(knownHosts.toFile()));
            ssh.connect(host, port);

            KeyProvider keys = ssh.loadKeys(privateKey.toString(), (PasswordFinder) null);
            ssh.authPublickey(user, keys);

            try (Session session = ssh.startSession()) {
                Session.Command cmd = session.exec(command);
                String out = new String(cmd.getInputStream().readAllBytes());
                String err = new String(cmd.getErrorStream().readAllBytes());
                cmd.join();
                int rc = cmd.getExitStatus() == null ? -1 : cmd.getExitStatus();
                return new ExecResult(rc, out, err);
            }
        }
    }
}