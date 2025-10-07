package br.cepel.dpc.pldpro_auto.infrastructure.service.impl;

import br.cepel.dpc.pldpro_auto.domain.Caso;
import br.cepel.dpc.pldpro_auto.domain.repository.ICasoRepository;
import br.cepel.dpc.pldpro_auto.infrastructure.enums.StatusEnum;
import br.cepel.dpc.pldpro_auto.infrastructure.ssh.SshClientService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class MonitoringService {

    private final ThreadPoolTaskScheduler scheduler;
    private final Map<Long, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final ICasoRepository casoRepository;
    private final SshClientService sshClientService; // seu serviço que executa comandos via SSH
    private final Duration interval = Duration.ofSeconds(30); // 30s

    public MonitoringService(ThreadPoolTaskScheduler scheduler,
                             ICasoRepository caseRepository,
                             SshClientService sshClientService) {
        this.scheduler = scheduler;
        this.casoRepository = caseRepository;
        this.sshClientService = sshClientService;
    }

    public void startMonitoring(Long caseId) {
        // evita duplicata
        if (tasks.containsKey(caseId)) return;

        Runnable task = createMonitorTask(caseId);

        // scheduleWithFixedDelay: executa a cada interval após término da execução anterior
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(task, interval.toMillis());
        tasks.put(caseId, future);
    }

    public void stopMonitoring(Long caseId) {
        ScheduledFuture<?> f = tasks.remove(caseId);
        if (f != null) {
            f.cancel(true);
        }
    }

    private Runnable createMonitorTask(Long caseId) {
        return () -> {
            try {
                Optional<Caso> opt = casoRepository.findById(caseId);
                if (opt.isEmpty()) {
                    stopMonitoring(caseId);
                    return;
                }

                Caso c = opt.get();

                // se já finalizado, cancela
                if (c.getStatus() == StatusEnum.FINALIZADO) {
                    stopMonitoring(caseId);
                    return;
                }

                // 1) ler pid.lock remoto (ou containerId se você já salvou)
                String pidLockPath = c.getPath();
                String pidLockCmd = "cat " + escapeShellArgument(pidLockPath.endsWith("/") ? pidLockPath + "pid.lock" : pidLockPath + "/pid.lock");

                SshResult pidRes = sshClientService.exec(c.getHost(), c.getPort(), c.getUser(), pidLockCmd);
                String containerId = null;
                if (pidRes != null && pidRes.getExitCode() == 0 && pidRes.getStdout() != null) {
                    containerId = pidRes.getStdout().trim().split("\\r?\\n")[0].trim();
                    if (!containerId.isEmpty()) {
                        // grava containerId se ainda não estiver salvo
                        if (!Objects.equals(c.getContainerId(), containerId)) {
                            c.setContainerId(containerId);
                            casoRepository.save(c);
                        }
                    }
                }

                if (containerId == null || containerId.isEmpty()) {
                    // não achou pid.lock — política: marcar FAILED ou esperar mais
                    // Aqui vamos apenas logar e aguardar próxima execução
                    return;
                }

                // 2) checar se container está rodando
                String inspectCmd = "docker inspect -f '{{.State.Running}}' " + escapeShellArgument(containerId);
                SshResult inspectRes = sshClientService.exec(c.getHost(), c.getPort(), c.getUser(), inspectCmd);

                boolean running = false;
                if (inspectRes != null && inspectRes.getExitCode() == 0 && inspectRes.getStdout() != null) {
                    running = "true".equals(inspectRes.getStdout().trim());
                }

                if (running) {
                    // opcional: atualizar status se necessário
                    if (c.getStatus() != StatusEnum.EXECUTANDO) {
                        c.setStatus(StatusEnum.EXECUTANDO);
                        casoRepository.save(c);
                    }
                    // container ainda ativo -> nada mais a fazer
                    return;
                } else {
                    // container não está rodando → pegar exit code e finalizar
                    String exitCmd = "docker inspect -f '{{.State.ExitCode}}' " + escapeShellArgument(containerId);
                    SshResult exitRes = sshClientService.exec(c.getHost(), c.getPort(), c.getUser(), exitCmd);
                    int exitCode = -999;
                    if (exitRes != null && exitRes.getExitCode() == 0 && exitRes.getStdout() != null) {
                        try { exitCode = Integer.parseInt(exitRes.getStdout().trim()); } catch (Exception ignored) {}
                    }

                    // atualiza status conforme exit code
                    if (exitCode == 0) c.setStatus(StatusEnum.FINISHED);
                    else c.setStatus(StatusEnum.FAILED);

                    c.setContainerId(containerId);
                    casoRepository.save(c);

                    // cancela monitor
                    stopMonitoring(caseId);
                }

            } catch (Exception e) {
                // logue e continue; não interrompa scheduling global
                // use seu logger: log.warn("Erro no monitor {}: {}", caseId, e.getMessage(), e);
            }
        };
    }

    private static String escapeShellArgument(String s) {
        // forma simples de escapar aspas - customize conforme necessidade
        return "'" + s.replace("'", "'\"'\"'") + "'";
    }
}

