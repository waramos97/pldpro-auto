package br.cepel.dpc.pldpro_auto.infrastructure.ssh;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class SshClientService {

    private static final Logger log = LoggerFactory.getLogger(SshClientService.class);


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

    /**
     * Retorna true se o container identificado por containerId estiver rodando.
     * Implementa via SSH: docker inspect -f '{{.State.Running}}' <containerId>
     *
     * O host/user/port usados vêm de variáveis de ambiente ou system properties:
     * ENV: SSH_HOST, SSH_PORT, SSH_USER
     * System props: ssh.host, ssh.port, ssh.user
     *
     * Retorna false em caso de erro (logando o problema).
     */
    public boolean isContainerRunning(String containerId){
        if (containerId == null || containerId.trim().isEmpty()) {
            log.debug("isContainerRunning: containerId vazio ou nulo");
            return false;
        }

        String host = getSshHost();
        int port = getSshPort();
        String user = getSshUser();

        String cmd = "docker inspect -f '{{.State.Running}}' " + escapeShellArg(containerId);
        try {
            ExecResult r = exec(host, port, user, cmd);
            if (r == null) {
                log.warn("isContainerRunning: exec retornou nulo para container {}", containerId);
                return false;
            }
            if (r.exitCode() != 0) {
                log.debug("isContainerRunning: comando retornou exitCode {} stderr: {}", r.exitCode(), r.stderr());
                return false;
            }
            String out = r.stdout() == null ? "" : r.stdout().trim();
            return "true".equalsIgnoreCase(out);
        } catch (Exception e) {
            log.warn("Erro ao verificar container {} em {}@{}:{} -> {}", containerId, user, host, port, e.toString());
            return false;
        }
    }

    /**
     * Retorna o exit code do container (ou null se não puder obter).
     * Implementa via SSH: docker inspect -f '{{.State.ExitCode}}' <containerId>
     */
    public Integer getContainerExitCode(String containerId){
        if (containerId == null || containerId.trim().isEmpty()) {
            log.debug("getContainerExitCode: containerId vazio ou nulo");
            return null;
        }

        String host = getSshHost();
        int port = getSshPort();
        String user = getSshUser();

        String cmd = "docker inspect -f '{{.State.ExitCode}}' " + escapeShellArg(containerId);
        try {
            ExecResult r = exec(host, port, user, cmd);
            if (r == null) {
                log.warn("getContainerExitCode: exec retornou nulo para container {}", containerId);
                return null;
            }
            if (r.exitCode() != 0) {
                log.debug("getContainerExitCode: comando retornou exitCode {} stderr: {}", r.exitCode(), r.stderr());
                return null;
            }
            String out = r.stdout() == null ? "" : r.stdout().trim();
            try {
                return Integer.valueOf(out);
            } catch (NumberFormatException nfe) {
                log.warn("getContainerExitCode: não foi possível parsear exit code '{}' para container {}",
                        out, containerId);
                return null;
            }
        } catch (Exception e) {
            log.warn("Erro ao obter exit code do container {} em {}@{}:{} -> {}", containerId, user, host, port, e.toString());
            return null;
        }
    }

    // --- helpers para obter host/port/user (env vars -> system props -> defaults) ---

    private String getSshHost() {
        String env = System.getenv("SSH_HOST");
        if (env != null && !env.isBlank()) return env;
        String prop = System.getProperty("ssh.host");
        if (prop != null && !prop.isBlank()) return prop;
        return "localhost";
    }

    private int getSshPort() {
        String env = System.getenv("SSH_PORT");
        if (env != null && !env.isBlank()) {
            try { return Integer.parseInt(env.trim()); } catch (NumberFormatException ignored) {}
        }
        String prop = System.getProperty("ssh.port");
        if (prop != null && !prop.isBlank()) {
            try { return Integer.parseInt(prop.trim()); } catch (NumberFormatException ignored) {}
        }
        return 22;
    }

    private String getSshUser() {
        String env = System.getenv("SSH_USER");
        if (env != null && !env.isBlank()) return env;
        String prop = System.getProperty("ssh.user");
        if (prop != null && !prop.isBlank()) return prop;
        return "root";
    }

    // simples escaping de argumento para shell (envolve em aspas simples, escapa aspas simples internas)
    private static String escapeShellArg(String s) {
        if (s == null) return "''";
        // replace ' with '"'"'
        String escaped = s.replace("'", "'\"'\"'");
        return "'" + escaped + "'";
    }


}