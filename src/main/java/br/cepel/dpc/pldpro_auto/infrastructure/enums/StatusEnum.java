package br.cepel.dpc.pldpro_auto.infrastructure.enums;

public enum StatusEnum {
    EXECUTANDO("executando"),
    FINALIZADO("finalizado");

    private String label;

    StatusEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
