package br.cepel.dpc.pldpro_auto.infrastructure.event;

public class CasoCreatedEvent {
    private final Long casoId;

    public CasoCreatedEvent(Long casoId) { this.casoId = casoId; }

    public Long getCasoId() { return casoId; }
}
