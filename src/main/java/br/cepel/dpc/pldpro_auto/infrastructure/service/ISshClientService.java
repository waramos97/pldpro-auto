package br.cepel.dpc.pldpro_auto.infrastructure.service;

public interface ISshClientService {
    /**
     * Retorna true se o container identificado por containerId estiver rodando.
     * Implementação pode executar: docker inspect -f '{{.State.Running}}' <containerId>
     */
    boolean isContainerRunning(String containerId);

    /**
     * Retorna o exit code do container (ou null se não puder obter).
     * Implementação pode executar: docker inspect -f '{{.State.ExitCode}}' <containerId>
     */
    Integer getContainerExitCode(String containerId);
}
