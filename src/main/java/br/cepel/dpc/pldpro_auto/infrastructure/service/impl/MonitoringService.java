package br.cepel.dpc.pldpro_auto.infrastructure.service.impl;

import br.cepel.dpc.pldpro_auto.domain.Caso;
import br.cepel.dpc.pldpro_auto.domain.repository.ICasoRepository;
import br.cepel.dpc.pldpro_auto.infrastructure.enums.StatusEnum;
import br.cepel.dpc.pldpro_auto.infrastructure.ssh.SshClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);


    @Autowired
    private ThreadPoolTaskScheduler scheduler;

    @Autowired
    private ICasoRepository casoRepository;

    @Autowired
    private SshClientService sshClientService;

    private final Map<Long, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final Duration interval = Duration.ofSeconds(30); // 30s

    public void startMonitoring(Long casoId) {
        // evita duplicatas
        if (tasks.containsKey(casoId)) return;

        Runnable task = createTask(casoId);
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(task, interval.toMillis());
        tasks.put(casoId, future);
    }

    public void stopMonitoring(Long casoId) {
        ScheduledFuture<?> f = tasks.remove(casoId);
        if (f != null) {
            f.cancel(true);
        }
    }

    private Runnable createTask(Long casoId) {
        return () -> {
            try {
                Optional<Caso> oCaso = casoRepository.findById(casoId);
                if (oCaso.isEmpty()) {
                    stopMonitoring(casoId);
                    return;
                }

                Caso caso = oCaso.get();
                log.info("Monitorando o caso "+ caso.getIdContainer());
                // se caso já finalizado, cancela monitor
                if (caso.getStatus() == StatusEnum.FINALIZADO || caso.getStatus() == StatusEnum.ERROR) {
                    stopMonitoring(casoId);
                    return;
                }

                String containerId = caso.getIdContainer();
                if (containerId == null || containerId.trim().isEmpty()) {
                    // idContainer ainda não preenchido — aguarda próxima rodada
                    return;
                }

                // verifica se o container está rodando (delegado à implementação de SshClientService)
                boolean running = false;
                try {
                    running = sshClientService.isContainerRunning(containerId);
                } catch (Exception e) {
                    // log warning e aguarda próxima rodada (não cancela)
                    // logger.warn("Erro checando container {}: {}", containerId, e.getMessage(), e);
                    return;
                }

                if (running) {
                    if (caso.getStatus() != StatusEnum.EXECUTANDO) {
                        caso.setStatus(StatusEnum.EXECUTANDO);
                        casoRepository.save(caso);
                    }
                    // ainda em execução — aguarda próxima verificação
                    return;
                } else {
                    // container não está rodando → obter exit code e finalizar caso
                    Integer exitCode = null;
                    try {
                        exitCode = sshClientService.getContainerExitCode(containerId);
                    } catch (Exception e) {
                        // se não conseguiu obter exit code, marca como FAILED por segurança
                        exitCode = null;
                    }

                    if (exitCode != null && exitCode == 0) {
                        caso.setStatus(StatusEnum.FINALIZADO);
                    } else {
                        caso.setStatus(StatusEnum.ERROR);
                    }
                    // salva e cancela monitor
                    casoRepository.save(caso);
                    stopMonitoring(casoId);
                }

            } catch (Exception e) {
                // não deixe a exceção quebrar o scheduler; apenas log e continue
                // logger.error("Erro no monitor caso {}: {}", casoId, e.getMessage(), e);
            }
        };
    }
}

