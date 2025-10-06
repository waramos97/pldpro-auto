package br.cepel.dpc.pldpro_auto.infrastructure.ssh;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.Response;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPException;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import net.schmizz.sshj.xfer.InMemorySourceFile;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SshBootstrapper {

    public void installPublicKeyFirstTime(
            String host, int port, String user,
            String password,
            Path localPublicKey
    ) throws IOException {

        String publicKey = Files.readString(localPublicKey).trim() + "\n";

        try (SSHClient ssh = new SSHClient()) {

//            File kh = new File(System.getProperty("user.home"), ".ssh/known_hosts");
//            ssh.addHostKeyVerifier(new KnownHostsTofuVerifier(
//                    kh,
//                    ssh.getTransport().getConfig().getLoggerFactory()
//            ));

            //Descomentar somente em produção
            ssh.addHostKeyVerifier(new OpenSSHKnownHosts(
                    new File(System.getProperty("user.home"), ".ssh/known_hosts"),
                    ssh.getTransport().getConfig().getLoggerFactory()
            ));

            ssh.connect(host, port);
            ssh.authPassword(user, password);

            try (SFTPClient sftp = ssh.newSFTPClient()) {
                final String sshDir = ".ssh";
                try { sftp.stat(sshDir); } catch (Exception e) { sftp.mkdir(sshDir); }
                sftp.chmod(sshDir, 0700);

                String authorizedPath = sshDir + "/authorized_keys";

                // Lê conteúdo existente usando RemoteFile + InputStream
                String existing = "";
                try (var remote = sftp.open(authorizedPath);
                     var in = remote.new RemoteFileInputStream(0)) {
                    existing = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                } catch (SFTPException e) {
                    if (e.getStatusCode() != Response.StatusCode.NO_SUCH_FILE) throw e;
                    // se for NO_SUCH_FILE, mantém existing=""
                }

                String merged = normalize(existing + "\n" + publicKey + "\n");

                // Sobe o conteúdo em memória
                InMemorySourceFile mem = new InMemorySourceFile() {
                    private final byte[] bytes = merged.getBytes();
                    @Override public String getName() { return "authorized_keys"; }
                    @Override public long getLength() { return bytes.length; }
                    @Override public int getPermissions() { return 0600; }
                    @Override public java.io.InputStream getInputStream() {
                        return new ByteArrayInputStream(bytes);
                    }
                };

                sftp.put(mem, authorizedPath);
                sftp.chmod(authorizedPath, 0600);
            }
        }
    }

    private static String normalize(String s) {
        return s.replaceAll("(?m)^[ \\t]*\\r?\\n", "\n");
    }
}
