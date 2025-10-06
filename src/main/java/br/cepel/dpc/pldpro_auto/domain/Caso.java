package br.cepel.dpc.pldpro_auto.domain;

import br.cepel.dpc.pldpro_auto.infrastructure.enums.StatusEnum;
import jakarta.persistence.*;

@Entity
public class Caso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "idContainer", nullable = false, length = 50)
    String idContainer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    StatusEnum status;

    public Caso() {
    }

    public Caso(Long id, String idContainer, StatusEnum status) {
        this.id = id;
        this.idContainer = idContainer;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdContainer() {
        return idContainer;
    }

    public void setIdContainer(String idContainer) {
        this.idContainer = idContainer;
    }

    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }
}
